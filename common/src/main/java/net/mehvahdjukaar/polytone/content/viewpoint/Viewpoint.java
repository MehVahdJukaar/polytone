package net.mehvahdjukaar.polytone.content.viewpoint;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

import java.util.List;
import java.util.Locale;

// rotations are degrees, unlike particles, so x_rot = c.pitch() looks where the player does
public record Viewpoint(ISimpleExp x, ISimpleExp y, ISimpleExp z,
                        ISimpleExp xRot, ISimpleExp yRot, ISimpleExp zRot,
                        ISimpleExp orthographic, ISimpleExp fov,
                        ISimpleExp near, ISimpleExp far,
                        List<ChunkSectionLayer> terrainLayers,
                        boolean renderEntities, boolean renderBlockEntities, boolean renderCameraEntity,
                        int resolution,
                        String depthSampler, String colorSampler,
                        String uniformBlock,
                        ISimpleExp updateInterval,
                        ISimpleExp activationCondition) {

    // same names block_modifiers.render_type takes
    private static final Codec<ChunkSectionLayer> LAYER_CODEC = Codec.STRING.xmap(
            s -> ChunkSectionLayer.valueOf(s.toUpperCase(Locale.ROOT)), ChunkSectionLayer::label);

    private static final List<ChunkSectionLayer> DEFAULT_LAYERS =
            List.of(ChunkSectionLayer.SOLID, ChunkSectionLayer.CUTOUT);

    private static final ISimpleExp DEFAULT_FOV = () -> 70.0;
    private static final ISimpleExp DEFAULT_NEAR = () -> 0.05;
    private static final ISimpleExp DEFAULT_FAR = () -> 256.0;

    public static final SchemaCodec<Viewpoint> CODEC = SchemaRecord.create(Viewpoint.class,
            i -> i.group(
                    i.optional("x", ISimpleExp.CODEC, ISimpleExp.ZERO, Viewpoint::x),
                    i.optional("y", ISimpleExp.CODEC, ISimpleExp.ZERO, Viewpoint::y),
                    i.optional("z", ISimpleExp.CODEC, ISimpleExp.ZERO, Viewpoint::z),
                    i.optional("x_rot", ISimpleExp.CODEC, ISimpleExp.ZERO, Viewpoint::xRot),
                    i.optional("y_rot", ISimpleExp.CODEC, ISimpleExp.ZERO, Viewpoint::yRot),
                    i.optional("z_rot", ISimpleExp.CODEC, ISimpleExp.ZERO, Viewpoint::zRot),
                    // > 0 is orthographic, that many blocks across. 0 is perspective using fov
                    i.optional("orthographic", ISimpleExp.CODEC, ISimpleExp.ZERO, Viewpoint::orthographic),
                    i.optional("fov", ISimpleExp.CODEC, DEFAULT_FOV, Viewpoint::fov),
                    i.optional("near", ISimpleExp.CODEC, DEFAULT_NEAR, Viewpoint::near),
                    i.optional("far", ISimpleExp.CODEC, DEFAULT_FAR, Viewpoint::far),
                    i.optional("terrain", LAYER_CODEC.listOf(), DEFAULT_LAYERS, Viewpoint::terrainLayers),
                    // off by default unlike the shadow map, entity state is rebuilt per viewpoint per render
                    i.optional("render_entities", Codec.BOOL, false, Viewpoint::renderEntities),
                    i.optional("render_block_entities", Codec.BOOL, false, Viewpoint::renderBlockEntities),
                    // the entity the camera views from, drawn even in first person
                    i.optional("render_camera_entity", Codec.BOOL, true, Viewpoint::renderCameraEntity),
                    i.optional("resolution", Codec.INT, 1024, Viewpoint::resolution),
                    i.optional("depth_sampler", Codec.STRING, "", Viewpoint::depthSampler),
                    // lit, unfogged, alpha 0 where nothing was drawn
                    i.optional("color_sampler", Codec.STRING, "", Viewpoint::colorSampler),
                    // block name, the shader picks its own instance name
                    i.optional("uniform_block", Codec.STRING, "", Viewpoint::uniformBlock),
                    // can change at runtime, never cache anything derived from it
                    i.optional("update_interval", ISimpleExp.CODEC, ISimpleExp.ZERO, Viewpoint::updateInterval),
                    i.optional("activation_condition", ISimpleExp.CODEC, ISimpleExp.ONE, Viewpoint::activationCondition)
            ).apply(i, Viewpoint::new));

    public boolean isActive() {
        return activationCondition.evaluate() > 0;
    }

    public double orthographicSize() {
        return orthographic.evaluate();
    }

    public boolean isSamplable() {
        return !depthSampler.isEmpty() || !colorSampler.isEmpty();
    }

    public boolean hasUniformBlock() {
        return !uniformBlock.isEmpty();
    }
}
