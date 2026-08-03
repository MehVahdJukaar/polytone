package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.bedrock.molang.MolangExpr;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code emitter_*} half of the component list: when particles come out, where from, and how long
 * the emitter itself lives.
 */
public class EmitterComponents {

    public record Initialization(Optional<MolangExpr> creationExpression,
                                 Optional<MolangExpr> perUpdateExpression) {
        public static final Codec<Initialization> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("creation_expression").forGetter(Initialization::creationExpression),
                MolangExpr.CODEC.optionalFieldOf("per_update_expression").forGetter(Initialization::perUpdateExpression)
        ).apply(i, Initialization::new));
    }

    /** Whether an entity-attached emitter simulates its particles in the entity's frame or the world's. */
    public record LocalSpace(boolean position, boolean rotation, boolean velocity) {
        public static final Codec<LocalSpace> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("position", false).forGetter(LocalSpace::position),
                Codec.BOOL.optionalFieldOf("rotation", false).forGetter(LocalSpace::rotation),
                Codec.BOOL.optionalFieldOf("velocity", false).forGetter(LocalSpace::velocity)
        ).apply(i, LocalSpace::new));
    }

    // ---- rate ----

    public record RateInstant(MolangExpr numParticles) {
        public static final Codec<RateInstant> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("num_particles", MolangExpr.of(10)).forGetter(RateInstant::numParticles)
        ).apply(i, RateInstant::new));
    }

    /** {@code spawn_rate} is particles per second, not per tick. */
    public record RateSteady(MolangExpr spawnRate, MolangExpr maxParticles) {
        public static final Codec<RateSteady> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("spawn_rate", MolangExpr.ONE).forGetter(RateSteady::spawnRate),
                MolangExpr.CODEC.optionalFieldOf("max_particles", MolangExpr.of(50)).forGetter(RateSteady::maxParticles)
        ).apply(i, RateSteady::new));
    }

    public record RateManual(MolangExpr maxParticles) {
        public static final Codec<RateManual> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("max_particles", MolangExpr.of(50)).forGetter(RateManual::maxParticles)
        ).apply(i, RateManual::new));
    }

    // ---- lifetime ----

    public record LifetimeOnce(MolangExpr activeTime) {
        public static final Codec<LifetimeOnce> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("active_time", MolangExpr.of(10)).forGetter(LifetimeOnce::activeTime)
        ).apply(i, LifetimeOnce::new));
    }

    public record LifetimeLooping(MolangExpr activeTime, MolangExpr sleepTime) {
        public static final Codec<LifetimeLooping> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("active_time", MolangExpr.of(10)).forGetter(LifetimeLooping::activeTime),
                MolangExpr.CODEC.optionalFieldOf("sleep_time", MolangExpr.ZERO).forGetter(LifetimeLooping::sleepTime)
        ).apply(i, LifetimeLooping::new));
    }

    public record LifetimeExpression(MolangExpr activationExpression, MolangExpr expirationExpression) {
        public static final Codec<LifetimeExpression> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("activation_expression", MolangExpr.ONE)
                        .forGetter(LifetimeExpression::activationExpression),
                MolangExpr.CODEC.optionalFieldOf("expiration_expression", MolangExpr.ZERO)
                        .forGetter(LifetimeExpression::expirationExpression)
        ).apply(i, LifetimeExpression::new));
    }

    public record LifetimeEvents(List<String> creationEvent, List<String> expirationEvent,
                                 Map<String, List<String>> timeline,
                                 Map<String, List<String>> travelDistanceEvents) {
        public static final Codec<LifetimeEvents> CODEC = RecordCodecBuilder.create(i -> i.group(
                BedrockEvent.NAMES.optionalFieldOf("creation_event", List.of()).forGetter(LifetimeEvents::creationEvent),
                BedrockEvent.NAMES.optionalFieldOf("expiration_event", List.of()).forGetter(LifetimeEvents::expirationEvent),
                BedrockEvent.TIMELINE.optionalFieldOf("timeline", Map.of()).forGetter(LifetimeEvents::timeline),
                BedrockEvent.TIMELINE.optionalFieldOf("travel_distance_events", Map.of())
                        .forGetter(LifetimeEvents::travelDistanceEvents)
        ).apply(i, LifetimeEvents::new));
    }

    // ---- shape ----

    public record ShapePoint(MolangExpr.Vec3 offset, BedrockShapeDirection direction) {
        public static final Codec<ShapePoint> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.Vec3.CODEC.optionalFieldOf("offset", MolangExpr.Vec3.ZERO).forGetter(ShapePoint::offset),
                BedrockShapeDirection.CODEC.optionalFieldOf("direction", BedrockShapeDirection.OUTWARDS)
                        .forGetter(ShapePoint::direction)
        ).apply(i, ShapePoint::new));
    }

    public record ShapeSphere(MolangExpr.Vec3 offset, MolangExpr radius, boolean surfaceOnly,
                              BedrockShapeDirection direction) {
        public static final Codec<ShapeSphere> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.Vec3.CODEC.optionalFieldOf("offset", MolangExpr.Vec3.ZERO).forGetter(ShapeSphere::offset),
                MolangExpr.CODEC.optionalFieldOf("radius", MolangExpr.ONE).forGetter(ShapeSphere::radius),
                Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(ShapeSphere::surfaceOnly),
                BedrockShapeDirection.CODEC.optionalFieldOf("direction", BedrockShapeDirection.OUTWARDS)
                        .forGetter(ShapeSphere::direction)
        ).apply(i, ShapeSphere::new));
    }

    public record ShapeBox(MolangExpr.Vec3 offset, MolangExpr.Vec3 halfDimensions, boolean surfaceOnly,
                           BedrockShapeDirection direction) {
        public static final Codec<ShapeBox> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.Vec3.CODEC.optionalFieldOf("offset", MolangExpr.Vec3.ZERO).forGetter(ShapeBox::offset),
                MolangExpr.Vec3.CODEC.optionalFieldOf("half_dimensions", MolangExpr.Vec3.ZERO)
                        .forGetter(ShapeBox::halfDimensions),
                Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(ShapeBox::surfaceOnly),
                BedrockShapeDirection.CODEC.optionalFieldOf("direction", BedrockShapeDirection.OUTWARDS)
                        .forGetter(ShapeBox::direction)
        ).apply(i, ShapeBox::new));
    }

    /** {@code plane_normal} accepts the axis keywords {@code x}/{@code y}/{@code z} as well as a vector. */
    public record ShapeDisc(MolangExpr.Vec3 planeNormal, MolangExpr.Vec3 offset, MolangExpr radius,
                            boolean surfaceOnly, BedrockShapeDirection direction) {
        private static final Codec<MolangExpr.Vec3> PLANE_NORMAL = Codec.withAlternative(
                MolangExpr.Vec3.CODEC, Codec.STRING.comapFlatMap(ShapeDisc::axis, v -> "y"));

        public static final Codec<ShapeDisc> CODEC = RecordCodecBuilder.create(i -> i.group(
                PLANE_NORMAL.optionalFieldOf("plane_normal", MolangExpr.Vec3.of(0, 1, 0)).forGetter(ShapeDisc::planeNormal),
                MolangExpr.Vec3.CODEC.optionalFieldOf("offset", MolangExpr.Vec3.ZERO).forGetter(ShapeDisc::offset),
                MolangExpr.CODEC.optionalFieldOf("radius", MolangExpr.ONE).forGetter(ShapeDisc::radius),
                Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(ShapeDisc::surfaceOnly),
                BedrockShapeDirection.CODEC.optionalFieldOf("direction", BedrockShapeDirection.OUTWARDS)
                        .forGetter(ShapeDisc::direction)
        ).apply(i, ShapeDisc::new));

        private static DataResult<MolangExpr.Vec3> axis(String name) {
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "x" -> DataResult.success(MolangExpr.Vec3.of(1, 0, 0));
                case "y" -> DataResult.success(MolangExpr.Vec3.of(0, 1, 0));
                case "z" -> DataResult.success(MolangExpr.Vec3.of(0, 0, 1));
                default -> DataResult.error(() -> "Unknown plane normal '" + name + "'");
            };
        }
    }

    public record ShapeEntityAabb(boolean surfaceOnly, BedrockShapeDirection direction) {
        public static final Codec<ShapeEntityAabb> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("surface_only", false).forGetter(ShapeEntityAabb::surfaceOnly),
                BedrockShapeDirection.CODEC.optionalFieldOf("direction", BedrockShapeDirection.OUTWARDS)
                        .forGetter(ShapeEntityAabb::direction)
        ).apply(i, ShapeEntityAabb::new));
    }

    /** The one shape that maps cleanly: three expressions, one per axis. */
    public record ShapeCustom(MolangExpr.Vec3 offset, BedrockShapeDirection direction) {
        public static final Codec<ShapeCustom> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.Vec3.CODEC.optionalFieldOf("offset", MolangExpr.Vec3.ZERO).forGetter(ShapeCustom::offset),
                BedrockShapeDirection.CODEC.optionalFieldOf("direction", BedrockShapeDirection.OUTWARDS)
                        .forGetter(ShapeCustom::direction)
        ).apply(i, ShapeCustom::new));
    }
}
