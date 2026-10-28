package net.mehvahdjukaar.polytone.content.fluid;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record FluidPropertyModifier(Optional<IColorGetter> colormap, Optional<IColorGetter> fogColormap,
                                    Optional<IBlockExp> fogRadius, Optional<IBlockExp> fogFade,
                                    Targets targets) {

    public static final SchemaCodec<FluidPropertyModifier> CODEC = SchemaRecord.create(FluidPropertyModifier.class, i ->
            i.group(
                    i.optional("colormap", Colormap.CODEC, FluidPropertyModifier::colormap),
                    i.optional("fog_colormap", Colormap.CODEC, FluidPropertyModifier::fogColormap),
                    i.optional("fog_radius", IBlockExp.CODEC, FluidPropertyModifier::fogRadius),
                    i.optional("fog_fade", IBlockExp.CODEC, FluidPropertyModifier::fogFade),
                    i.optional("targets", Targets.CODEC, Targets.EMPTY, FluidPropertyModifier::targets)
            ).apply(i, FluidPropertyModifier::new));

    // Other has priority
    public FluidPropertyModifier merge(FluidPropertyModifier newMod) {
        return new FluidPropertyModifier(
                newMod.colormap.isPresent() ? newMod.colormap() : this.colormap(),
                newMod.fogColormap().isPresent() ? newMod.fogColormap() : this.fogColormap(),
                newMod.fogRadius.or(this::fogRadius),
                newMod.fogFade.or(this::fogFade),
                newMod.targets.merge(this.targets)
        );
    }

    public static FluidPropertyModifier ofBlockColor(IColorGetter colormap) {
        return new FluidPropertyModifier(Optional.of(colormap), Optional.empty(), Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    public static FluidPropertyModifier ofFogColor(IColorGetter colormap) {
        return new FluidPropertyModifier(Optional.empty(), Optional.of(colormap), Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    @Nullable
    public IColorGetter getColormap() {
        return colormap.orElse(null);
    }

    @Nullable
    public IColorGetter getFogColormap() {
        return fogColormap.orElse(null);
    }

    public boolean hasColormap() {
        return colormap.isPresent();
    }

    public boolean hasFogShape() {
        return fogRadius.isPresent() || fogFade.isPresent();
    }

    public static boolean isCameraSubmerged(Camera camera, ClientLevel level, FluidState fluid) {
        BlockPos pos = camera.blockPosition();
        return camera.position().y < pos.getY() + fluid.getHeight(level, pos);
    }

    public void modifyFogShape(FogData fog, Camera camera, ClientLevel level) {
        float span = fog.environmentalEnd - fog.environmentalStart;
        float radius = fogRadius.isPresent() ? (float) fogRadius.get().evaluate(level, camera.position(), null) : 1;
        float fade = fogFade.isPresent() ? (float) fogFade.get().evaluate(level, camera.position(), null) : 1;
        fog.environmentalEnd *= radius;
        fog.environmentalStart = fog.environmentalEnd - span * radius * fade;
        fog.skyEnd = fog.environmentalEnd;
        fog.cloudEnd = fog.environmentalEnd;
    }
}
