package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.bedrock.molang.MolangExpr;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@code particle_*} half of the component list: what an individual particle looks like, how it
 * moves once it exists, and when it dies.
 */
public class ParticleComponents {

    /** The particle-side counterpart of {@code emitter_initialization}. */
    public record Initialization(Optional<MolangExpr> creationExpression,
                                 Optional<MolangExpr> perUpdateExpression) {
        public static final Codec<Initialization> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("creation_expression").forGetter(Initialization::creationExpression),
                MolangExpr.CODEC.optionalFieldOf("per_update_expression").forGetter(Initialization::perUpdateExpression)
        ).apply(i, Initialization::new));
    }

    // ---- initial state ----

    /** Speed along the direction the emitter shape handed out. Bedrock accepts a scalar or a vector. */
    public record InitialSpeed(Optional<MolangExpr> scalar, Optional<MolangExpr.Vec3> vector) {
        public static final Codec<InitialSpeed> CODEC = Codec.withAlternative(
                MolangExpr.Vec3.CODEC.xmap(v -> new InitialSpeed(Optional.empty(), Optional.of(v)),
                        s -> s.vector.orElse(MolangExpr.Vec3.ZERO)),
                MolangExpr.CODEC.xmap(e -> new InitialSpeed(Optional.of(e), Optional.empty()),
                        s -> s.scalar.orElse(MolangExpr.ZERO)));
    }

    /** Degrees, and {@code rotation_rate} is degrees per second. */
    public record InitialSpin(MolangExpr rotation, MolangExpr rotationRate) {
        public static final Codec<InitialSpin> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("rotation", MolangExpr.ZERO).forGetter(InitialSpin::rotation),
                MolangExpr.CODEC.optionalFieldOf("rotation_rate", MolangExpr.ZERO).forGetter(InitialSpin::rotationRate)
        ).apply(i, InitialSpin::new));
    }

    // ---- motion ----

    public record MotionDynamic(MolangExpr.Vec3 linearAcceleration, MolangExpr linearDragCoefficient,
                                MolangExpr rotationAcceleration, MolangExpr rotationDragCoefficient) {
        public static final Codec<MotionDynamic> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.Vec3.CODEC.optionalFieldOf("linear_acceleration", MolangExpr.Vec3.ZERO)
                        .forGetter(MotionDynamic::linearAcceleration),
                MolangExpr.CODEC.optionalFieldOf("linear_drag_coefficient", MolangExpr.ZERO)
                        .forGetter(MotionDynamic::linearDragCoefficient),
                MolangExpr.CODEC.optionalFieldOf("rotation_acceleration", MolangExpr.ZERO)
                        .forGetter(MotionDynamic::rotationAcceleration),
                MolangExpr.CODEC.optionalFieldOf("rotation_drag_coefficient", MolangExpr.ZERO)
                        .forGetter(MotionDynamic::rotationDragCoefficient)
        ).apply(i, MotionDynamic::new));
    }

    /** Position driven directly, relative to the emitter, instead of by forces. */
    public record MotionParametric(MolangExpr.Vec3 relativePosition, Optional<MolangExpr.Vec3> direction,
                                   MolangExpr rotation) {
        public static final Codec<MotionParametric> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.Vec3.CODEC.optionalFieldOf("relative_position", MolangExpr.Vec3.ZERO)
                        .forGetter(MotionParametric::relativePosition),
                MolangExpr.Vec3.CODEC.optionalFieldOf("direction").forGetter(MotionParametric::direction),
                MolangExpr.CODEC.optionalFieldOf("rotation", MolangExpr.ZERO).forGetter(MotionParametric::rotation)
        ).apply(i, MotionParametric::new));
    }

    public record MotionCollision(MolangExpr enabled, double collisionDrag, double coefficientOfRestitution,
                                  double collisionRadius, boolean expireOnContact, List<CollisionEvent> events) {
        public static final Codec<MotionCollision> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("enabled", MolangExpr.ONE).forGetter(MotionCollision::enabled),
                Codec.DOUBLE.optionalFieldOf("collision_drag", 0.0).forGetter(MotionCollision::collisionDrag),
                Codec.DOUBLE.optionalFieldOf("coefficient_of_restitution", 0.0)
                        .forGetter(MotionCollision::coefficientOfRestitution),
                Codec.DOUBLE.optionalFieldOf("collision_radius", 0.01).forGetter(MotionCollision::collisionRadius),
                Codec.BOOL.optionalFieldOf("expire_on_contact", false).forGetter(MotionCollision::expireOnContact),
                CollisionEvent.CODEC.listOf().optionalFieldOf("events", List.of()).forGetter(MotionCollision::events)
        ).apply(i, MotionCollision::new));

        public record CollisionEvent(String event, Optional<Double> minSpeed) {
            public static final Codec<CollisionEvent> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.fieldOf("event").forGetter(CollisionEvent::event),
                    Codec.DOUBLE.optionalFieldOf("min_speed").forGetter(CollisionEvent::minSpeed)
            ).apply(i, CollisionEvent::new));
        }
    }

    // ---- appearance ----

    public record AppearanceBillboard(MolangExpr.Vec2 size, FacingMode facingCameraMode,
                                      Optional<Direction> direction, Optional<Uv> uv) {
        public static final Codec<AppearanceBillboard> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.Vec2.CODEC.optionalFieldOf("size", MolangExpr.Vec2.of(1, 1))
                        .forGetter(AppearanceBillboard::size),
                FacingMode.CODEC.optionalFieldOf("facing_camera_mode", FacingMode.ROTATE_XYZ)
                        .forGetter(AppearanceBillboard::facingCameraMode),
                Direction.CODEC.optionalFieldOf("direction").forGetter(AppearanceBillboard::direction),
                Uv.CODEC.optionalFieldOf("uv").forGetter(AppearanceBillboard::uv)
        ).apply(i, AppearanceBillboard::new));

        /** Frame count of the flipbook animation, when there is one, as written in the file. */
        public Optional<String> flipbookFrames() {
            return uv.flatMap(Uv::flipbook).map(f -> f.maxFrame().source());
        }

        public record Direction(String mode, double minSpeedThreshold, Optional<MolangExpr.Vec3> customDirection) {
            public static final Codec<Direction> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.optionalFieldOf("mode", "derive_from_velocity").forGetter(Direction::mode),
                    Codec.DOUBLE.optionalFieldOf("min_speed_threshold", 0.01).forGetter(Direction::minSpeedThreshold),
                    MolangExpr.Vec3.CODEC.optionalFieldOf("custom_direction").forGetter(Direction::customDirection)
            ).apply(i, Direction::new));
        }

        /** The rect of the effect's texture this particle draws, in pixels. */
        public record Uv(double textureWidth, double textureHeight, MolangExpr.Vec2 uv, MolangExpr.Vec2 uvSize,
                         Optional<Flipbook> flipbook) {
            public static final Codec<Uv> CODEC = RecordCodecBuilder.create(i -> i.group(
                    Codec.DOUBLE.optionalFieldOf("texture_width", 1.0).forGetter(Uv::textureWidth),
                    Codec.DOUBLE.optionalFieldOf("texture_height", 1.0).forGetter(Uv::textureHeight),
                    MolangExpr.Vec2.CODEC.optionalFieldOf("uv", MolangExpr.Vec2.of(0, 0)).forGetter(Uv::uv),
                    MolangExpr.Vec2.CODEC.optionalFieldOf("uv_size", MolangExpr.Vec2.of(1, 1)).forGetter(Uv::uvSize),
                    Flipbook.CODEC.optionalFieldOf("flipbook").forGetter(Uv::flipbook)
            ).apply(i, Uv::new));
        }

        public record Flipbook(MolangExpr.Vec2 baseUv, MolangExpr.Vec2 sizeUv, MolangExpr.Vec2 stepUv,
                               double framesPerSecond, MolangExpr maxFrame, boolean stretchToLifetime, boolean loop) {
            public static final Codec<Flipbook> CODEC = RecordCodecBuilder.create(i -> i.group(
                    MolangExpr.Vec2.CODEC.optionalFieldOf("base_UV", MolangExpr.Vec2.of(0, 0)).forGetter(Flipbook::baseUv),
                    MolangExpr.Vec2.CODEC.optionalFieldOf("size_UV", MolangExpr.Vec2.of(1, 1)).forGetter(Flipbook::sizeUv),
                    MolangExpr.Vec2.CODEC.optionalFieldOf("step_UV", MolangExpr.Vec2.of(0, 0)).forGetter(Flipbook::stepUv),
                    Codec.DOUBLE.optionalFieldOf("frames_per_second", 1.0).forGetter(Flipbook::framesPerSecond),
                    MolangExpr.CODEC.optionalFieldOf("max_frame", MolangExpr.ONE).forGetter(Flipbook::maxFrame),
                    Codec.BOOL.optionalFieldOf("stretch_to_lifetime", false).forGetter(Flipbook::stretchToLifetime),
                    Codec.BOOL.optionalFieldOf("loop", false).forGetter(Flipbook::loop)
            ).apply(i, Flipbook::new));
        }

        public enum FacingMode implements StringRepresentable {
            ROTATE_XYZ("rotate_xyz"),
            ROTATE_Y("rotate_y"),
            LOOKAT_XYZ("lookat_xyz"),
            LOOKAT_Y("lookat_y"),
            LOOKAT_DIRECTION("lookat_direction"),
            DIRECTION_X("direction_x"),
            DIRECTION_Y("direction_y"),
            DIRECTION_Z("direction_z"),
            EMITTER_TRANSFORM_XY("emitter_transform_xy"),
            EMITTER_TRANSFORM_XZ("emitter_transform_xz"),
            EMITTER_TRANSFORM_YZ("emitter_transform_yz");

            public static final Codec<FacingMode> CODEC = StringRepresentable.fromEnum(FacingMode::values);

            private final String name;

            FacingMode(String name) {
                this.name = name;
            }

            @Override
            public String getSerializedName() {
                return name;
            }
        }
    }

    public record AppearanceTinting(BedrockColor color) {
        public static final Codec<AppearanceTinting> CODEC = RecordCodecBuilder.create(i -> i.group(
                BedrockColor.CODEC.fieldOf("color").forGetter(AppearanceTinting::color)
        ).apply(i, AppearanceTinting::new));
    }

    /** Marker component: the particle is tinted by the light level at its position. */
    public record AppearanceLighting() {
        public static final Codec<AppearanceLighting> CODEC =
                RecordCodecBuilder.create(i -> i.point(new AppearanceLighting()));
    }

    // ---- lifetime ----

    public record LifetimeExpression(MolangExpr expirationExpression, Optional<MolangExpr> maxLifetime) {
        public static final Codec<LifetimeExpression> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("expiration_expression", MolangExpr.ZERO)
                        .forGetter(LifetimeExpression::expirationExpression),
                MolangExpr.CODEC.optionalFieldOf("max_lifetime").forGetter(LifetimeExpression::maxLifetime)
        ).apply(i, LifetimeExpression::new));
    }

    public record LifetimeEvents(List<String> creationEvent, List<String> expirationEvent,
                                 Map<String, List<String>> timeline) {
        public static final Codec<LifetimeEvents> CODEC = RecordCodecBuilder.create(i -> i.group(
                BedrockEvent.NAMES.optionalFieldOf("creation_event", List.of()).forGetter(LifetimeEvents::creationEvent),
                BedrockEvent.NAMES.optionalFieldOf("expiration_event", List.of()).forGetter(LifetimeEvents::expirationEvent),
                BedrockEvent.TIMELINE.optionalFieldOf("timeline", Map.of()).forGetter(LifetimeEvents::timeline)
        ).apply(i, LifetimeEvents::new));
    }

    /** {@code [a, b, c, d]} of the plane equation; particles crossing to the negative side expire. */
    public record KillPlane(double a, double b, double c, double d) {
        public static final Codec<KillPlane> CODEC = Codec.DOUBLE.listOf().comapFlatMap(
                list -> list.size() == 4
                        ? DataResult.success(new KillPlane(list.getFirst(), list.get(1), list.get(2), list.get(3)))
                        : DataResult.error(() -> "Kill plane needs 4 values"),
                p -> List.of(p.a, p.b, p.c, p.d));
    }

    public record ExpireInBlocks(List<String> blocks) {
        public static final Codec<ExpireInBlocks> CODEC = Codec.STRING.listOf()
                .xmap(ExpireInBlocks::new, ExpireInBlocks::blocks);
    }

    public record ExpireNotInBlocks(List<String> blocks) {
        public static final Codec<ExpireNotInBlocks> CODEC = Codec.STRING.listOf()
                .xmap(ExpireNotInBlocks::new, ExpireNotInBlocks::blocks);
    }
}
