package net.mehvahdjukaar.polytone.content.fluid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.mehvahdjukaar.polytone.common.Targets;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record FluidPropertyModifier(Optional<? extends BlockColor> colormap, Optional<IColorGetter> fogColormap,
                                    Optional<IBlockExp> fogRadius, Optional<IBlockExp> fogFade,
                                    Targets targets) {

    public static final Codec<FluidPropertyModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Colormap.CODEC.optionalFieldOf("colormap").forGetter(c -> (Optional<IColorGetter>) c.colormap),
                    Colormap.CODEC.optionalFieldOf("fog_colormap").forGetter(FluidPropertyModifier::fogColormap),
                    IBlockExp.CODEC_LEGACY.optionalFieldOf("fog_radius").forGetter(FluidPropertyModifier::fogRadius),
                    IBlockExp.CODEC_LEGACY.optionalFieldOf("fog_fade").forGetter(FluidPropertyModifier::fogFade),
                    Targets.CODEC.optionalFieldOf("targets", Targets.EMPTY).forGetter(FluidPropertyModifier::targets)
            ).apply(instance, FluidPropertyModifier::new));

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

    public static FluidPropertyModifier ofBlockColor(BlockColor colormap) {
        return new FluidPropertyModifier(Optional.of(colormap), Optional.empty(), Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    public static FluidPropertyModifier ofFogColor(IColorGetter colormap) {
        return new FluidPropertyModifier(Optional.empty(), Optional.of(colormap), Optional.empty(), Optional.empty(), Targets.EMPTY);
    }

    @Nullable
    public BlockColor getColormap() {
        return colormap.orElse(null);
    }

    @Nullable
    public BlockColor getFogColormap() {
        return fogColormap.orElse(null);
    }

    public boolean hasColormap() {
        return colormap.isPresent();
    }

    public boolean hasFogShape() {
        return fogRadius.isPresent() || fogFade.isPresent();
    }

    public static boolean isCameraSubmerged(Camera camera, ClientLevel level, FluidState fluid) {
        BlockPos pos = camera.getBlockPosition();
        return camera.getPosition().y < pos.getY() + fluid.getHeight(level, pos);
    }

    public void modifyFogShape(Camera camera, ClientLevel level) {
        float start = RenderSystem.getShaderFogStart();
        float end = RenderSystem.getShaderFogEnd();
        float span = end - start;
        float radius = fogRadius.isPresent() ? (float) fogRadius.get().evaluate(level, camera.getPosition(), null) : 1;
        float fade = fogFade.isPresent() ? (float) fogFade.get().evaluate(level, camera.getPosition(), null) : 1;
        end *= radius;
        RenderSystem.setShaderFogStart(end - span * radius * fade);
        RenderSystem.setShaderFogEnd(end);
    }
}
