package net.mehvahdjukaar.polytone.bedrock.convert;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.polytone.bedrock.Diagnostic;
import net.mehvahdjukaar.polytone.bedrock.DiagnosticSink;
import net.mehvahdjukaar.polytone.bedrock.model.BedrockColor;
import net.mehvahdjukaar.polytone.bedrock.model.BedrockComponentTypes;
import net.mehvahdjukaar.polytone.bedrock.model.BedrockComponents;
import net.mehvahdjukaar.polytone.bedrock.model.BedrockEvent;
import net.mehvahdjukaar.polytone.bedrock.model.BedrockParticleEffect;
import net.mehvahdjukaar.polytone.bedrock.model.BedrockParticleFile;
import net.mehvahdjukaar.polytone.bedrock.model.BedrockShapeDirection;
import net.mehvahdjukaar.polytone.bedrock.model.EmitterComponents;
import net.mehvahdjukaar.polytone.bedrock.model.ParticleComponents;
import net.mehvahdjukaar.polytone.bedrock.molang.MolangExpr;
import net.mehvahdjukaar.polytone.bedrock.molang.MolangTranslator.Scope;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;
import net.minecraft.core.Direction.Axis;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

// particle_* components become the visible particle, emitter_* ones a second invisible particle whose
// particle_emitters spawn the first. See the package README for what that trade costs.
public class BedrockParticleConverter {

    public static ConversionResult convert(BedrockParticleFile file, ConversionOptions options) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        DiagnosticSink sink = DiagnosticSink.collectingInto(diagnostics);

        BedrockParticleEffect effect = file.effect();
        BedrockComponents components = effect.components();
        components.reportUnknown(sink);
        reportStructuralGaps(effect, components, sink);

        PolytoneParticleJson particle = buildParticle(effect, components, options, sink);
        PolytoneParticleJson emitter = buildEmitter(components, options, sink);

        List<ConversionResult.OutputFile> files = new ArrayList<>();
        addParticleFiles(files, options.namespace(), options.path(), particle, options, sink);
        if (emitter != null) {
            addParticleFiles(files, options.namespace(), options.path() + "_emitter", emitter, options, sink);
        }

        List<ConversionResult.TextureRequest> textures = textureRequests(effect, components, options, sink);

        return new ConversionResult(options.particleId(), emitter != null ? options.emitterId() : null,
                files, textures, diagnostics);
    }

    private static PolytoneParticleJson buildParticle(BedrockParticleEffect effect, BedrockComponents components,
                                                      ConversionOptions options, DiagnosticSink sink) {
        PolytoneParticleJson json = new PolytoneParticleJson();
        json.renderMode(renderMode(effect.description().renderParams().material(), sink));

        OptionalDouble lifetimeTicks = convertLifetime(components, json, options, sink);
        boolean drawsNothing = convertBillboard(components, json, options, sink);
        convertTinting(components, json, options, sink);
        convertSpin(components, json, options, sink);
        convertMotion(components, json, options, sink);
        convertCollision(components, json, sink);
        convertExpiry(components, json, sink);

        convertCreationEvents(effect, components, json, options, lifetimeTicks, drawsNothing, sink);

        if (components.has(BedrockComponentTypes.PARTICLE_APPEARANCE_LIGHTING)) {
            sink.info("particle_appearance_lighting", "Our particles already sample world light, nothing to do");
        }
        // Bedrock particles never collide unless they say so, and never vanish for standing still
        if (!components.has(BedrockComponentTypes.PARTICLE_MOTION_COLLISION)) {
            json.hasPhysics(false);
        }
        json.killWhenStill(false);
        return json;
    }

    // returns the lifetime in ticks when it is a fixed number, which decides how creation events convert
    private static OptionalDouble convertLifetime(BedrockComponents components, PolytoneParticleJson json,
                                                  ConversionOptions options, DiagnosticSink sink) {
        Optional<ParticleComponents.LifetimeExpression> lifetime =
                components.get(BedrockComponentTypes.PARTICLE_LIFETIME_EXPRESSION, sink);
        if (lifetime.isEmpty()) {
            sink.warn("particle_lifetime_expression",
                    "No lifetime component, the particle falls back to the vanilla random lifetime");
            return OptionalDouble.empty();
        }

        OptionalDouble ticks = OptionalDouble.empty();
        Optional<MolangExpr> maxLifetime = lifetime.get().maxLifetime();
        if (maxLifetime.isPresent()) {
            String seconds = translate(maxLifetime.get(), Scope.PARTICLE, "particle_lifetime_expression/max_lifetime", options, sink);
            String asTicks = PolytoneExpressions.scale(seconds, BedrockUnits.TICKS_PER_SECOND);
            json.init("lifetime", asTicks);
            ticks = PolytoneExpressions.asNumber(asTicks);
        } else {
            sink.warn("particle_lifetime_expression",
                    "No max_lifetime, so the particle only dies through its expiration expression");
        }
        MolangExpr expiration = lifetime.get().expirationExpression();
        if (!expiration.isConstant(0)) {
            // same contract on both sides: non-zero means the particle is done
            json.tick("remove_condition", translate(expiration, Scope.PARTICLE,
                    "particle_lifetime_expression/expiration_expression", options, sink));
        }
        return ticks;
    }

    // true when the particle draws nothing and exists only to run its components
    private static boolean convertBillboard(BedrockComponents components, PolytoneParticleJson json,
                                            ConversionOptions options, DiagnosticSink sink) {
        Optional<ParticleComponents.AppearanceBillboard> maybe =
                components.get(BedrockComponentTypes.PARTICLE_APPEARANCE_BILLBOARD, sink);
        if (maybe.isEmpty()) return true;
        ParticleComponents.AppearanceBillboard billboard = maybe.get();

        json.rotationMode(rotationMode(billboard.facingCameraMode(), sink));

        String where = "particle_appearance_billboard/size";
        String width = translate(billboard.size().x(), Scope.PARTICLE, where, options, sink);
        String height = translate(billboard.size().y(), Scope.PARTICLE, where, options, sink);
        if (!width.equals(height)) {
            sink.warn(where, "Non-square size " + Diagnostic.brief(width) + " x " + Diagnostic.brief(height)
                    + ", using the width: our particles have a single size");
        }
        // both sides measure a billboard by half extent in blocks, so this is 1:1
        assign(json, "size", width);

        billboard.flipbookFrames().ifPresent(frames ->
                sink.warn("particle_appearance_billboard/uv/flipbook",
                        "Flipbook animation is not converted; the generated particle uses a single sprite. " +
                                "Split the atlas into " + frames + " sprites and list them in the particle json to animate it"));
        return PolytoneExpressions.isZero(width);
    }

    private static void convertTinting(BedrockComponents components, PolytoneParticleJson json,
                                       ConversionOptions options, DiagnosticSink sink) {
        Optional<ParticleComponents.AppearanceTinting> maybe =
                components.get(BedrockComponentTypes.PARTICLE_APPEARANCE_TINTING, sink);
        if (maybe.isEmpty()) return;
        String where = "particle_appearance_tinting/color";
        BedrockColor color = maybe.get().color();

        if (color instanceof BedrockColor.Gradient gradient) {
            if (gradient.stops().isEmpty()) return;
            sink.warn(where, "Colour gradients are not converted (" + gradient.stops().size() + " stops); " +
                    "the first stop is used as a static colour. A colormap, or red/green/blue expressions " +
                    "over AGE, would do the whole job");
            color = gradient.stops().getFirst().color();
        }
        applySolidColor(color, json, options, sink, where);
    }

    private static void applySolidColor(BedrockColor color, PolytoneParticleJson json, ConversionOptions options,
                                        DiagnosticSink sink, String where) {
        BedrockColor.Rgba rgba;
        if (color instanceof BedrockColor.Hex hex) {
            rgba = hex.toRgba();
            if (rgba == null) {
                sink.error(where, "Could not read colour '" + hex.value() + "'");
                return;
            }
        } else if (color instanceof BedrockColor.Rgba direct) {
            rgba = direct;
        } else {
            sink.error(where, "Nested gradients are not a thing");
            return;
        }
        assign(json, "red", translate(rgba.r(), Scope.PARTICLE, where, options, sink));
        assign(json, "green", translate(rgba.g(), Scope.PARTICLE, where, options, sink));
        assign(json, "blue", translate(rgba.b(), Scope.PARTICLE, where, options, sink));
        assign(json, "alpha", translate(rgba.a(), Scope.PARTICLE, where, options, sink));
    }

    private static void convertSpin(BedrockComponents components, PolytoneParticleJson json,
                                    ConversionOptions options, DiagnosticSink sink) {
        Optional<ParticleComponents.InitialSpin> maybe =
                components.get(BedrockComponentTypes.PARTICLE_INITIAL_SPIN, sink);
        if (maybe.isEmpty()) return;
        ParticleComponents.InitialSpin spin = maybe.get();

        String start = translate(spin.rotation(), Scope.PARTICLE, "particle_initial_spin/rotation", options, sink);
        String rate = translate(spin.rotationRate(), Scope.PARTICLE, "particle_initial_spin/rotation_rate", options, sink);

        String startRadians = PolytoneExpressions.scale(start, BedrockUnits.degreesToRadians(1));
        String radiansPerTick = PolytoneExpressions.scale(rate, BedrockUnits.degreesPerSecondToRadiansPerTick(1));

        if (PolytoneExpressions.isZero(radiansPerTick)) {
            if (!PolytoneExpressions.isZero(startRadians)) json.init("roll", startRadians);
        } else {
            json.tick("roll", PolytoneExpressions.spin(startRadians, radiansPerTick));
        }
    }

    private static void convertMotion(BedrockComponents components, PolytoneParticleJson json,
                                      ConversionOptions options, DiagnosticSink sink) {
        components.get(BedrockComponentTypes.PARTICLE_MOTION_PARAMETRIC, sink).ifPresent(parametric -> {
            // packs often use this component only to drive rotation, which does convert
            boolean movesPosition = !parametric.relativePosition().isZero() || parametric.direction().isPresent();
            if (movesPosition) {
                sink.error("particle_motion_parametric",
                        "Position is not converted: it is relative to the emitter, and a spawned particle does not " +
                                "keep a reference to where it came from");
            }
            if (!parametric.rotation().isConstant(0)) {
                String degrees = translate(parametric.rotation(), Scope.PARTICLE,
                        "particle_motion_parametric/rotation", options, sink);
                assign(json, "roll", PolytoneExpressions.scale(degrees, BedrockUnits.degreesToRadians(1)));
            }
        });

        Optional<ParticleComponents.MotionDynamic> maybe =
                components.get(BedrockComponentTypes.PARTICLE_MOTION_DYNAMIC, sink);
        if (maybe.isEmpty()) return;
        ParticleComponents.MotionDynamic motion = maybe.get();

        String dragSource = translate(motion.linearDragCoefficient(), Scope.PARTICLE,
                "particle_motion_dynamic/linear_drag_coefficient", options, sink);
        OptionalDouble drag = PolytoneExpressions.asNumber(dragSource);
        if (drag.isEmpty()) {
            sink.warn("particle_motion_dynamic/linear_drag_coefficient",
                    "Drag varies at runtime; converted using its value as written, which may not be a number");
        }
        double dragMultiplier = BedrockUnits.dragToPerTickMultiplier(drag.orElse(0));

        for (Axis axis : Axis.values()) {
            String perSecondSquared = translate(motion.linearAcceleration().get(axis), Scope.PARTICLE,
                    "particle_motion_dynamic/linear_acceleration", options, sink);
            String perTickSquared = PolytoneExpressions.scale(perSecondSquared,
                    BedrockUnits.perSecondSquaredToPerTickSquared(1));
            if (dragMultiplier == 1 && PolytoneExpressions.isZero(perTickSquared)) continue;
            json.tick(velocityField(axis), PolytoneExpressions.integrateVelocity(axis, dragMultiplier, perTickSquared));
        }
        // our particles damp themselves every tick by default, which would drag on top of Bedrock's own
        json.init("friction", "1");

        if (!motion.rotationAcceleration().isConstant(0) || !motion.rotationDragCoefficient().isConstant(0)) {
            sink.warn("particle_motion_dynamic", "Rotational acceleration and drag are not converted");
        }
    }

    private static void convertCollision(BedrockComponents components, PolytoneParticleJson json, DiagnosticSink sink) {
        Optional<ParticleComponents.MotionCollision> maybe =
                components.get(BedrockComponentTypes.PARTICLE_MOTION_COLLISION, sink);
        if (maybe.isEmpty()) return;
        ParticleComponents.MotionCollision collision = maybe.get();

        json.hasPhysics(true);
        json.killOnContact(collision.expireOnContact());
        if (collision.collisionRadius() > 0) {
            json.init("hitbox_size", PolytoneExpressions.constant(collision.collisionRadius() * 2));
        }
        if (collision.coefficientOfRestitution() != 0) {
            sink.warn("particle_motion_collision/coefficient_of_restitution", "Bouncing is not converted, particles just stop");
        }
        if (collision.collisionDrag() != 0) {
            sink.warn("particle_motion_collision/collision_drag", "Drag on impact is not converted");
        }
        if (!collision.events().isEmpty()) {
            sink.warn("particle_motion_collision/events", "Collision events are not converted");
        }
    }

    private static void convertExpiry(BedrockComponents components, PolytoneParticleJson json, DiagnosticSink sink) {
        components.get(BedrockComponentTypes.PARTICLE_KILL_PLANE, sink).ifPresent(plane -> sink.warn(
                "particle_kill_plane",
                "Not converted: it needs a comparison, and remove_condition takes a plain number. " +
                        "Add it by hand as the MVEL expression \"((" + plane.a() + "*o.x()+" + plane.b() + "*o.y()+"
                        + plane.c() + "*o.z()+" + plane.d() + ") < 0) ? 1 : 0\""));

        components.get(BedrockComponentTypes.PARTICLE_EXPIRE_IF_IN_BLOCKS, sink).ifPresent(expire -> {
            if (isJustWater(expire.blocks())) {
                json.liquidAffinity("non_liquids");
            } else {
                sink.warn("particle_expire_if_in_blocks",
                        "Only water is expressible (as liquid_affinity), these are not: " + expire.blocks());
            }
        });
        components.get(BedrockComponentTypes.PARTICLE_EXPIRE_IF_NOT_IN_BLOCKS, sink).ifPresent(expire -> {
            if (isJustWater(expire.blocks())) {
                json.liquidAffinity("liquids");
            } else {
                sink.warn("particle_expire_if_not_in_blocks",
                        "Only water is expressible (as liquid_affinity), these are not: " + expire.blocks());
            }
        });
    }

    // A creation event fires once but our emitters fire every tick, so the two only line up on a particle living
    // a single tick. Common enough: Bedrock emitter effects are size-0 particles whose creation event spawns the
    // real thing, the same trick as our meta particle.
    private static void convertCreationEvents(BedrockParticleEffect effect, BedrockComponents components,
                                              PolytoneParticleJson json, ConversionOptions options,
                                              OptionalDouble lifetimeTicks, boolean drawsNothing,
                                              DiagnosticSink sink) {
        String where = "particle_lifetime_events/creation_event";
        Optional<ParticleComponents.LifetimeEvents> events =
                components.get(BedrockComponentTypes.PARTICLE_LIFETIME_EVENTS, sink);
        if (events.isEmpty()) return;

        if (!events.get().expirationEvent().isEmpty() || !events.get().timeline().isEmpty()) {
            sink.warn("particle_lifetime_events", "Expiration and timeline events are not converted; only what " +
                    "fires when the particle is born has an equivalent");
        }
        List<String> names = events.get().creationEvent();
        if (names.isEmpty()) return;

        List<EventAction> actions = resolveEvents(names, effect, where, sink);
        if (actions.isEmpty()) return;

        boolean firesOnce = lifetimeTicks.isPresent() && lifetimeTicks.getAsDouble() <= 1;
        if (!firesOnce && !drawsNothing) {
            sink.warn(where, "Not converted: the event fires once but our emitters run every tick, and this " +
                    "particle is visible so its lifetime cannot be pinned to one tick. Spawn " + describe(actions) +
                    " from wherever this particle is spawned instead");
            return;
        }
        if (!firesOnce) {
            json.init("lifetime", "1");
            sink.info(where, "Particle draws nothing and exists only to fire its creation event, so it now lives " +
                    "a single tick and emits exactly once");
        }

        for (EventAction action : actions) {
            String chance = PolytoneExpressions.constant(action.chance());
            if (action.effect() != null) {
                JsonObject emitter = new JsonObject();
                emitter.addProperty("particle", targetId(action.effect(), options));
                emitter.addProperty("chance", chance);
                emitter.addProperty("count", "1");
                json.emitter(emitter);
            } else if (action.sound() != null) {
                JsonObject emitter = new JsonObject();
                emitter.addProperty("sound", action.sound());
                emitter.addProperty("chance", chance);
                json.soundEmitter(emitter);
            }
        }
    }

    // an emitter ref points at a whole effect, so the meta particle; a particle ref points at the visible one
    private static String targetId(BedrockEvent.EffectRef ref, ConversionOptions options) {
        String id = ConversionOptions.identifierOf(ref.effect());
        return ref.type().startsWith("emitter") ? id + "_emitter" : id;
    }

    private static List<EventAction> resolveEvents(List<String> names, BedrockParticleEffect effect,
                                                   String where, DiagnosticSink sink) {
        List<EventAction> actions = new ArrayList<>();
        for (String name : names) {
            BedrockEvent event = effect.events().get(name);
            if (event == null) {
                sink.error(where, "Event '" + name + "' is referenced but never declared");
                continue;
            }
            flatten(event, 1, actions);
        }
        return actions;
    }

    // a sequence runs all children, a randomize picks one by weight
    private static void flatten(BedrockEvent event, double chance, List<EventAction> out) {
        event.particleEffect().ifPresent(ref -> out.add(new EventAction(chance, ref, null)));
        event.soundEffect().ifPresent(ref -> out.add(new EventAction(chance, null, ref.eventName())));
        for (BedrockEvent child : event.sequence()) {
            flatten(child, chance, out);
        }
        double total = event.randomize().stream().mapToDouble(e -> e.weight().orElse(1.0)).sum();
        if (total > 0) {
            for (BedrockEvent child : event.randomize()) {
                flatten(child, chance * child.weight().orElse(1.0) / total, out);
            }
        }
    }

    private static String describe(List<EventAction> actions) {
        return actions.stream()
                .map(a -> a.effect() != null ? a.effect().effect() : String.valueOf(a.sound()))
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private record EventAction(double chance, BedrockEvent.@Nullable EffectRef effect, @Nullable String sound) {
    }

    private static @Nullable PolytoneParticleJson buildEmitter(BedrockComponents components,
                                                               ConversionOptions options, DiagnosticSink sink) {
        JsonObject entry = new JsonObject();
        entry.addProperty("particle", options.particleId());

        SpawnRate rate = convertRate(components, options, sink);
        if (rate == null) return null;
        entry.addProperty("chance", rate.chance());
        entry.addProperty("count", rate.count());

        convertShape(components, entry, options, sink);
        convertInitialSpeed(components, entry, options, sink);

        PolytoneParticleJson json = new PolytoneParticleJson();
        json.renderMode(ParticleRenderMode.INVISIBLE)
                .hasPhysics(false)
                .killWhenStill(false)
                .killWhenNotInView(false)
                .emitter(entry);
        json.init("lifetime", PolytoneExpressions.constant(emitterLifetimeTicks(components, rate, sink)));
        json.init("size", "0");
        return json;
    }

    private static double emitterLifetimeTicks(BedrockComponents components, SpawnRate rate, DiagnosticSink sink) {
        if (rate.instant()) return 1;

        Optional<EmitterComponents.LifetimeOnce> once =
                components.get(BedrockComponentTypes.EMITTER_LIFETIME_ONCE, sink);
        if (once.isPresent()) {
            return BedrockUnits.secondsToTicks(once.get().activeTime().constantOr(10));
        }
        Optional<EmitterComponents.LifetimeLooping> looping =
                components.get(BedrockComponentTypes.EMITTER_LIFETIME_LOOPING, sink);
        if (looping.isPresent()) {
            double cycle = looping.get().activeTime().constantOr(10) + looping.get().sleepTime().constantOr(0);
            sink.warn("emitter_lifetime_looping",
                    "A particle cannot live forever, so the loop runs once (" + cycle + "s). For a real loop, " +
                            "put the emitter on a block or an entity bone instead of on a meta particle");
            return BedrockUnits.secondsToTicks(cycle);
        }
        if (components.has(BedrockComponentTypes.EMITTER_LIFETIME_EXPRESSION)) {
            sink.warn("emitter_lifetime_expression",
                    "Activation and expiration expressions are not converted; the emitter runs for 10s");
        }
        return BedrockUnits.secondsToTicks(10);
    }

    private static @Nullable SpawnRate convertRate(BedrockComponents components, ConversionOptions options,
                                                   DiagnosticSink sink) {
        Optional<EmitterComponents.RateInstant> instant =
                components.get(BedrockComponentTypes.EMITTER_RATE_INSTANT, sink);
        if (instant.isPresent()) {
            String count = translate(instant.get().numParticles(), Scope.EMITTER,
                    "emitter_rate_instant/num_particles", options, sink);
            return new SpawnRate("1", count, true);
        }

        Optional<EmitterComponents.RateSteady> steady =
                components.get(BedrockComponentTypes.EMITTER_RATE_STEADY, sink);
        if (steady.isPresent()) {
            String perSecond = translate(steady.get().spawnRate(), Scope.EMITTER,
                    "emitter_rate_steady/spawn_rate", options, sink);
            String perTick = PolytoneExpressions.scale(perSecond, BedrockUnits.perSecondToPerTick(1));
            OptionalDouble constant = PolytoneExpressions.asNumber(perTick);
            if (constant.isEmpty()) {
                sink.warn("emitter_rate_steady/spawn_rate",
                        "Rate varies at runtime; it is used as a per-tick chance, so anything above 20/s is clamped");
                return new SpawnRate(perTick, "1", false);
            }
            double value = constant.getAsDouble();
            if (value <= 1) return new SpawnRate(PolytoneExpressions.constant(value), "1", false);
            return new SpawnRate("1", PolytoneExpressions.constant(Math.round(value)), false);
        }

        if (components.has(BedrockComponentTypes.EMITTER_RATE_MANUAL)) {
            sink.warn("emitter_rate_manual",
                    "Manual emission is script driven and has no equivalent, so no emitter was generated. " +
                            "Spawn " + options.particleId() + " from a block or entity emitter instead");
            return null;
        }
        sink.info("emitter", "No rate component, so no emitter was generated. " +
                "Spawn " + options.particleId() + " from a block, an entity bone or another particle");
        return null;
    }

    private static void convertShape(BedrockComponents components, JsonObject entry,
                                     ConversionOptions options, DiagnosticSink sink) {
        Optional<EmitterComponents.ShapePoint> point = components.get(BedrockComponentTypes.EMITTER_SHAPE_POINT, sink);
        if (point.isPresent()) {
            putOffset(entry, point.get().offset(), options, sink, "emitter_shape_point/offset");
            return;
        }
        Optional<EmitterComponents.ShapeCustom> custom = components.get(BedrockComponentTypes.EMITTER_SHAPE_CUSTOM, sink);
        if (custom.isPresent()) {
            putOffset(entry, custom.get().offset(), options, sink, "emitter_shape_custom/offset");
            sink.info("emitter_shape_custom", "Converted exactly: per-axis expressions are what our emitters take");
            return;
        }
        Optional<EmitterComponents.ShapeBox> box = components.get(BedrockComponentTypes.EMITTER_SHAPE_BOX, sink);
        if (box.isPresent()) {
            EmitterComponents.ShapeBox shape = box.get();
            for (Axis axis : Axis.values()) {
                String offset = translate(shape.offset().get(axis), Scope.EMITTER,
                        "emitter_shape_box/offset", options, sink);
                String half = translate(shape.halfDimensions().get(axis), Scope.EMITTER,
                        "emitter_shape_box/half_dimensions", options, sink);
                entry.addProperty(axis.getSerializedName(),
                        PolytoneExpressions.add(offset, PolytoneExpressions.randomSymmetric(half)));
            }
            if (shape.surfaceOnly()) {
                sink.warn("emitter_shape_box/surface_only", "Particles fill the box instead of sitting on its faces");
            }
            return;
        }
        Optional<EmitterComponents.ShapeSphere> sphere = components.get(BedrockComponentTypes.EMITTER_SHAPE_SPHERE, sink);
        if (sphere.isPresent()) {
            EmitterComponents.ShapeSphere shape = sphere.get();
            String radius = translate(shape.radius(), Scope.EMITTER, "emitter_shape_sphere/radius", options, sink);
            for (Axis axis : Axis.values()) {
                String offset = translate(shape.offset().get(axis), Scope.EMITTER,
                        "emitter_shape_sphere/offset", options, sink);
                entry.addProperty(axis.getSerializedName(), PolytoneExpressions.add(offset,
                        PolytoneExpressions.randomGaussian(PolytoneExpressions.scale(radius, 0.5))));
            }
            sink.warn("emitter_shape_sphere", "Approximated with a gaussian blob: x, y and z are separate expressions " +
                    "with no shared state, so a point on a sphere cannot be sampled properly" +
                    (shape.surfaceOnly() ? " (surface_only is lost entirely)" : ""));
            return;
        }
        Optional<EmitterComponents.ShapeDisc> disc = components.get(BedrockComponentTypes.EMITTER_SHAPE_DISC, sink);
        if (disc.isPresent()) {
            EmitterComponents.ShapeDisc shape = disc.get();
            String radius = translate(shape.radius(), Scope.EMITTER, "emitter_shape_disc/radius", options, sink);
            Axis normal = constantAxis(shape.planeNormal());
            for (Axis axis : Axis.values()) {
                String offset = translate(shape.offset().get(axis), Scope.EMITTER,
                        "emitter_shape_disc/offset", options, sink);
                entry.addProperty(axis.getSerializedName(), axis == normal ? offset : PolytoneExpressions.add(offset,
                        PolytoneExpressions.randomGaussian(PolytoneExpressions.scale(radius, 0.5))));
            }
            sink.warn("emitter_shape_disc", normal == null
                    ? "Plane normal is not axis aligned, converted as a sphere"
                    : "Approximated with a gaussian spread in the plane, for the same reason as spheres");
            return;
        }
        if (components.has(BedrockComponentTypes.EMITTER_SHAPE_ENTITY_AABB)) {
            sink.error("emitter_shape_entity_aabb",
                    "There is no entity to take a bounding box from. Use an entity modifier bone emitter instead");
        }
    }

    private static void convertInitialSpeed(BedrockComponents components, JsonObject entry,
                                            ConversionOptions options, DiagnosticSink sink) {
        Optional<ParticleComponents.InitialSpeed> maybe =
                components.get(BedrockComponentTypes.PARTICLE_INITIAL_SPEED, sink);
        if (maybe.isEmpty()) return;
        String where = "particle_initial_speed";

        Optional<MolangExpr.Vec3> vector = maybe.get().vector();
        if (vector.isPresent()) {
            for (Axis axis : Axis.values()) {
                String perSecond = translate(vector.get().get(axis), Scope.EMITTER, where, options, sink);
                entry.addProperty(velocityField(axis),
                        PolytoneExpressions.scale(perSecond, BedrockUnits.perSecondToPerTick(1)));
            }
            return;
        }

        String speed = PolytoneExpressions.scale(
                translate(maybe.get().scalar().orElse(MolangExpr.ZERO), Scope.EMITTER, where, options, sink),
                BedrockUnits.perSecondToPerTick(1));
        if (PolytoneExpressions.isZero(speed)) return;

        BedrockShapeDirection direction = shapeDirection(components, sink);
        if (direction.mode() == BedrockShapeDirection.Mode.CUSTOM && direction.custom() != null) {
            for (Axis axis : Axis.values()) {
                String component = translate(direction.custom().get(axis), Scope.EMITTER,
                        "emitter_shape/direction", options, sink);
                entry.addProperty(velocityField(axis), PolytoneExpressions.multiply(component, speed));
            }
            return;
        }
        double sign = direction.mode() == BedrockShapeDirection.Mode.INWARDS ? -1 : 1;
        for (Axis axis : Axis.values()) {
            entry.addProperty(velocityField(axis),
                    PolytoneExpressions.randomGaussian(PolytoneExpressions.scale(speed, sign * 0.6)));
        }
        sink.warn("emitter_shape/direction",
                "'" + direction.mode().name().toLowerCase(Locale.ROOT) + "' needs the spawn offset the particle " +
                        "actually got, which a separate velocity expression cannot see; using a random direction instead");
    }

    private static void assign(PolytoneParticleJson json, String field, String expression) {
        if (PolytoneExpressions.asNumber(expression).isPresent()) {
            json.init(field, expression);
        } else {
            json.tick(field, expression);
        }
    }

    private static String translate(MolangExpr expr, Scope scope, String where, ConversionOptions options,
                                    DiagnosticSink sink) {
        return options.translator().translate(expr, scope, where, sink);
    }

    private static String velocityField(Axis axis) {
        return "d" + axis.getSerializedName();
    }

    private static @Nullable Axis constantAxis(MolangExpr.Vec3 normal) {
        Axis found = null;
        for (Axis axis : Axis.values()) {
            MolangExpr part = normal.get(axis);
            if (!part.isConstant()) return null;
            if (part.constantOr(0) != 0) {
                if (found != null) return null;
                found = axis;
            }
        }
        return found;
    }

    private static BedrockShapeDirection shapeDirection(BedrockComponents components, DiagnosticSink sink) {
        return components.get(BedrockComponentTypes.EMITTER_SHAPE_POINT, sink)
                .map(EmitterComponents.ShapePoint::direction)
                .or(() -> components.get(BedrockComponentTypes.EMITTER_SHAPE_SPHERE, sink)
                        .map(EmitterComponents.ShapeSphere::direction))
                .or(() -> components.get(BedrockComponentTypes.EMITTER_SHAPE_BOX, sink)
                        .map(EmitterComponents.ShapeBox::direction))
                .or(() -> components.get(BedrockComponentTypes.EMITTER_SHAPE_DISC, sink)
                        .map(EmitterComponents.ShapeDisc::direction))
                .or(() -> components.get(BedrockComponentTypes.EMITTER_SHAPE_CUSTOM, sink)
                        .map(EmitterComponents.ShapeCustom::direction))
                .orElse(BedrockShapeDirection.OUTWARDS);
    }

    private static void putOffset(JsonObject entry, MolangExpr.Vec3 offset, ConversionOptions options,
                                  DiagnosticSink sink, String where) {
        for (Axis axis : Axis.values()) {
            entry.addProperty(axis.getSerializedName(), translate(offset.get(axis), Scope.EMITTER, where, options, sink));
        }
    }

    private static ParticleRenderMode renderMode(String material, DiagnosticSink sink) {
        return switch (material.toLowerCase(Locale.ROOT)) {
            case "particles_alpha", "particles_opaque" -> ParticleRenderMode.OPAQUE;
            case "particles_blend" -> ParticleRenderMode.TRANSLUCENT;
            case "particles_add" -> ParticleRenderMode.ADDITIVE_TRANSLUCENT;
            default -> {
                sink.warn("basic_render_parameters/material", "Unknown material '" + material + "', using opaque");
                yield ParticleRenderMode.OPAQUE;
            }
        };
    }

    private static String rotationMode(ParticleComponents.AppearanceBillboard.FacingMode mode, DiagnosticSink sink) {
        return switch (mode) {
            case ROTATE_XYZ, LOOKAT_XYZ -> "look_at_xyz";
            case ROTATE_Y, LOOKAT_Y -> "look_at_y";
            case LOOKAT_DIRECTION, DIRECTION_X, DIRECTION_Y, DIRECTION_Z -> {
                sink.warn("particle_appearance_billboard/facing_camera_mode",
                        "'" + mode.getSerializedName() + "' converted to movement_aligned, which is close but not identical");
                yield "movement_aligned";
            }
            case EMITTER_TRANSFORM_XY, EMITTER_TRANSFORM_XZ, EMITTER_TRANSFORM_YZ -> {
                sink.warn("particle_appearance_billboard/facing_camera_mode",
                        "'" + mode.getSerializedName() + "' orients by the emitter's transform, which we do not track");
                yield "look_at_xyz";
            }
        };
    }

    private static boolean isJustWater(List<String> blocks) {
        return !blocks.isEmpty() && blocks.stream()
                .allMatch(b -> b.endsWith("water") || b.endsWith("flowing_water"));
    }

    private static void reportStructuralGaps(BedrockParticleEffect effect, BedrockComponents components,
                                             DiagnosticSink sink) {
        // the events map itself is not reported, the hooks are diagnosed one by one where they convert
        if (!effect.curves().isEmpty()) {
            sink.warn("curves", effect.curves().size() + " curve(s) are not converted; inline the interpolation " +
                    "into whichever expression reads them");
        }
        if (components.has(BedrockComponentTypes.EMITTER_LIFETIME_EVENTS)) {
            sink.warn("emitter_lifetime_events", "Emitter events are not converted; the meta particle has no " +
                    "timeline and lives too long to stand in for a one-shot creation event");
        }
        if (components.has(BedrockComponentTypes.EMITTER_LOCAL_SPACE)) {
            sink.warn("emitter_local_space", "Particles always simulate in world space once spawned");
        }
        if (components.has(BedrockComponentTypes.EMITTER_INITIALIZATION)) {
            sink.warn("emitter_initialization", "Emitter scoped variables are not converted; the meta particle has " +
                    "a single 'custom' slot to stash state in");
        }
        if (components.has(BedrockComponentTypes.PARTICLE_INITIALIZATION)) {
            sink.warn("particle_initialization", "Per-particle variables are not converted; a particle carries one " +
                    "'custom' number, not a named variable set");
        }
    }

    private static void addParticleFiles(List<ConversionResult.OutputFile> files, String namespace, String path,
                                         PolytoneParticleJson particle, ConversionOptions options, DiagnosticSink sink) {
        if (options.validate()) {
            particle.validate().error().ifPresent(error ->
                    sink.error(path, "Generated json does not load: " + error.message()));
        }
        files.add(new ConversionResult.OutputFile(
                "assets/" + namespace + "/polytone/custom_particles/" + path + ".json", particle.build()));
        files.add(new ConversionResult.OutputFile(
                "assets/" + namespace + "/particles/" + path + ".json", spriteList(namespace + ":" + options.path())));
    }

    // even an invisible emitter needs a sprite list to register on the vanilla side
    private static JsonObject spriteList(String sprite) {
        JsonObject json = new JsonObject();
        JsonArray textures = new JsonArray();
        textures.add(sprite);
        json.add("textures", textures);
        return json;
    }

    private static List<ConversionResult.TextureRequest> textureRequests(BedrockParticleEffect effect,
                                                                         BedrockComponents components,
                                                                         ConversionOptions options,
                                                                         DiagnosticSink sink) {
        String source = effect.description().renderParams().texture();
        if (source.isEmpty()) {
            sink.warn("basic_render_parameters/texture", "No texture, you will have to supply the sprite yourself");
            return List.of();
        }
        Optional<ParticleComponents.AppearanceBillboard.Uv> uv =
                components.get(BedrockComponentTypes.PARTICLE_APPEARANCE_BILLBOARD, sink)
                        .flatMap(ParticleComponents.AppearanceBillboard::uv);
        if (uv.isEmpty()) {
            sink.info("basic_render_parameters/texture", "Whole texture used as the sprite, no uv rect was given");
            return List.of(ConversionResult.TextureRequest.wholeTexture(source, options.particleId()));
        }

        ParticleComponents.AppearanceBillboard.Uv rect = uv.get();
        // a flipbook restates the rect in its own fields, and those are the ones that draw
        MolangExpr.Vec2 origin = rect.flipbook().map(ParticleComponents.AppearanceBillboard.Flipbook::baseUv)
                .orElse(rect.uv());
        MolangExpr.Vec2 size = rect.flipbook().map(ParticleComponents.AppearanceBillboard.Flipbook::sizeUv)
                .orElse(rect.uvSize());

        OptionalDouble u = PolytoneExpressions.asNumber(origin.x().source());
        OptionalDouble v = PolytoneExpressions.asNumber(origin.y().source());
        OptionalDouble width = PolytoneExpressions.asNumber(size.x().source());
        OptionalDouble height = PolytoneExpressions.asNumber(size.y().source());
        if (u.isEmpty() || v.isEmpty() || width.isEmpty() || height.isEmpty()) {
            String hint = origin.x().source().contains("random") || origin.y().source().contains("random")
                    ? "Uv rect picks a random cell of the atlas: cut the atlas into one sprite per cell, list " +
                    "them all in the sprite json and set random_sprite on the particle"
                    : "Uv rect is expression driven, using the whole texture";
            sink.warn("particle_appearance_billboard/uv", hint);
            return List.of(ConversionResult.TextureRequest.wholeTexture(source, options.particleId()));
        }
        return List.of(new ConversionResult.TextureRequest(source, options.particleId(),
                u.getAsDouble(), v.getAsDouble(), width.getAsDouble(), height.getAsDouble(),
                rect.textureWidth(), rect.textureHeight()));
    }

    // instant means the whole batch comes out in one tick, which decides how long the meta particle stays alive
    private record SpawnRate(String chance, String count, boolean instant) {
    }
}
