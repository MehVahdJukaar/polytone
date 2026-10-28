package net.mehvahdjukaar.polytone.content.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.StrUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.BlockExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.common.expressions.impl.IColormapExp;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class Colormap implements IColorGetter, ColorResolver {

    private final IColormapExp xGetter;
    private final IColormapExp yGetter;
    private final BiomeIdMapper biomeMapper;
    private final boolean triangular;
    private final boolean rounds;
    private final boolean hasBiomeBlend; //if this should be used as ColorResolver, allowing for biome blend
    private final boolean usesBiome;
    private final boolean usesPos;
    private final boolean usesState;
    private final boolean usePlayerPosition;

    public boolean inlined = true;
    public ResourceLocation debugID = null;

    private Integer defaultColor;
    private ArrayImage image = null;
    private @Nullable ResourceLocation explicitTargetTexture; //explicit targets
    private final @Nullable ColormapColorModulatorExpression colorMult;
    private final IColorGetter fallback;

    private final ThreadLocal<ColormapContext> contextHack = ThreadLocal.withInitial(ColormapContext::new);

    static final Codec<Colormap> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            ColorUtils.CODEC.optionalFieldOf("default_color").forGetter(c -> Optional.ofNullable(c.defaultColor)),
            IColormapExp.CODEC_LEGACY.fieldOf("x_axis").forGetter(c -> c.xGetter),
            IColormapExp.CODEC_LEGACY.fieldOf("y_axis").forGetter(c -> c.yGetter),
            Codec.BOOL.optionalFieldOf("triangular", false).forGetter(c -> c.triangular),
            Codec.BOOL.optionalFieldOf("rounds", true).forGetter(c -> c.rounds),
            Codec.BOOL.optionalFieldOf("biome_blend").forGetter(c -> Optional.of(c.hasBiomeBlend)),
            BiomeIdMapper.CODEC.optionalFieldOf("biome_id_mapper").forGetter(c -> Optional.of(c.biomeMapper)),
            ResourceLocation.CODEC.optionalFieldOf("texture_path").forGetter(c -> Optional.ofNullable(c.explicitTargetTexture)),
            ColormapColorModulatorExpression.CODEC.optionalFieldOf("color_modifier").forGetter(c -> Optional.ofNullable(c.colorMult)),
            Codec.BOOL.optionalFieldOf("use_player_position", true).forGetter(c -> c.usePlayerPosition),
            Codec.lazyInitialized(() -> Colormap.REFERENCE_OR_EXPRESSION).optionalFieldOf("fallback_colormap", ColormapsManager.ORIGINAL_MARKER).forGetter(c -> c.fallback)
    ).apply(i, Colormap::new));

    public static final Codec<IColorGetter> SINGLE_COLOR_CODEC = ColorUtils.CODEC.xmap(
            Colormap::singleColor, c -> c instanceof Colormap cm ? cm.defaultColor : 0);

    public static final Codec<IColorGetter> EXPRESSION_CODEC = IBlockExp.CODEC_RAND_SEEDED.xmap(
            IColorGetter.ExpressionColor::new,
            g -> g instanceof IColorGetter.ExpressionColor(IBlockExp exp) ? exp : IBlockExp.ZERO);

    public static final Codec<IColorGetter> DIRECT_REFERENCE_OR_EXPRESSION = SchemaCodecs.labeled(
            SchemaCodecs.alternatives(SINGLE_COLOR_CODEC,
                    SchemaCodecs.referenceOrDirect(Polytone.COLORMAPS.byNameCodec(), DIRECT_CODEC),
                    EXPRESSION_CODEC),
            SchemaCodecs.alt("single color", ColorUtils.CODEC),
            SchemaCodecs.alt("colormap reference", ResourceLocation.CODEC),
            SchemaCodecs.alt("inline colormap", DIRECT_CODEC),
            SchemaCodecs.alt("expression", BlockExp.TYPE_RAND_SEEDED.codec()));

    public static final Codec<IColorGetter> REFERENCE_OR_EXPRESSION = SchemaCodecs.labeled(
            SchemaCodecs.alternatives(SINGLE_COLOR_CODEC, Polytone.COLORMAPS.byNameCodec(), EXPRESSION_CODEC),
            SchemaCodecs.alt("single color", ColorUtils.CODEC),
            SchemaCodecs.alt("colormap reference", ResourceLocation.CODEC),
            SchemaCodecs.alt("expression", BlockExp.TYPE_RAND_SEEDED.codec()));

    public static final Codec<IColorGetter> CODEC = SchemaCodecs.labeled(
            Codec.withAlternative(Colormap.DIRECT_REFERENCE_OR_EXPRESSION, BiomeCompoundColorGetter.CODEC),
            SchemaCodecs.alt("colormap", DIRECT_REFERENCE_OR_EXPRESSION),
            SchemaCodecs.alt("biome compound", BiomeCompoundColorGetter.CODEC));

    private Colormap(Optional<Integer> defaultColor, IColormapExp xGetter, IColormapExp yGetter,
                     boolean triangular, boolean rounds, Optional<Boolean> biomeBlend, Optional<BiomeIdMapper> biomeMapper,
                     Optional<ResourceLocation> explicitTargetTexture, Optional<ColormapColorModulatorExpression> colorMult,
                     boolean usePlayerPosition, IColorGetter fallback) {
        this.defaultColor = defaultColor.orElse(null);
        this.usePlayerPosition = usePlayerPosition;
        this.xGetter = xGetter;
        this.yGetter = yGetter;
        this.triangular = triangular;
        this.rounds = rounds;
        this.colorMult = colorMult.orElse(null);
        this.usesBiome = (xGetter.usesBiome() || yGetter.usesBiome());
        this.usesPos = usesBiome || (xGetter.usesPos() || yGetter.usesPos());
        this.usesState = (xGetter.usesState() || yGetter.usesState());
        this.hasBiomeBlend = biomeBlend.orElse(usesBiome);
        this.biomeMapper = biomeMapper.orElse(BiomeIdMapper.BY_INDEX);
        this.explicitTargetTexture = explicitTargetTexture.orElse(null);
        this.fallback = fallback;

        if (this.hasBiomeBlend) PlatStuff.registerColorResolver(this);
    }

    private Colormap(IColormapExp xGetter, IColormapExp yGetter, boolean triangular) {
        this(Optional.empty(), xGetter, yGetter, triangular, true, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), true, ColormapsManager.ORIGINAL_MARKER);
    }

    public Colormap makeConcurrent() {
        Colormap concurrentColormap = new Colormap(Optional.ofNullable(this.defaultColor), this.xGetter.createConcurrent(),
                this.yGetter.createConcurrent(), this.triangular,
                this.rounds, Optional.of(this.hasBiomeBlend), Optional.of(this.biomeMapper),
                Optional.ofNullable(this.explicitTargetTexture), Optional.ofNullable(this.colorMult == null ? null : this.colorMult.createConcurrent()),
                this.usePlayerPosition, this.fallback.makeConcurrent());
        if (this.image != null) concurrentColormap.acceptTexture(this.image);
        return concurrentColormap;
    }

    @Override
    public String toString() {
        return "Colormap{" +
                "ID=" + debugID +
                ", triangular=" + triangular +
                ", rounds=" + rounds +
                ", hasBiomeBlend=" + hasBiomeBlend +
                ", inlined=" + inlined +
                '}';
    }

    public void acceptTexture(@NotNull ArrayImage image) {
        this.image = image;
        if (defaultColor == null) {
            this.defaultColor = sample(0.5f, 0.5f) | 0xFF000000;
        }
    }

    @Override
    public boolean needsToFillTexture() {
        return image == null;
    }

    public ResourceLocation getExplicitTargetTexture() {
        return explicitTargetTexture;
    }

    public ResourceLocation getTargetTexture(ResourceLocation def) {
        return explicitTargetTexture != null ? explicitTargetTexture : def;
    }

    public void setExplicitTargetTexture(ResourceLocation imageTarget) {
        this.explicitTargetTexture = imageTarget.withPath(StrUtils.stripExtension(imageTarget.getPath()));
    }

    /**
     * BlockColor method. called by vanilla. with biome blend on it goes through the level tint cache, so the result is
     * whatever is cached at that pos. use sampleColor instead when you need a fresh color, like particles or fog
     */
    @Override
    public int getColor(@Nullable BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int i) {
        BlockColor original = fallback == ColormapsManager.ORIGINAL_MARKER && state != null ?
                Polytone.BLOCK_MODIFIERS.getVanillaColor(state.getBlock()) : null;
        ColormapContext ctx = contextHack.get();
        ctx.startBlock(level, state, pos, i, original);
        try {
            return getColorInternal(state, level, pos);
        } finally {
            ctx.endBlock();
        }
    }

    // Dont use tint index
    private int getColorInternal(@Nullable BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos) {
        if (level == null) return defaultColor;
        if (pos == null && (usesPos || usesBiome)) {
            return defaultColor;
        }
        if (state == null && usesState) return defaultColor;
        if (hasBiomeBlend) {
            // ask the world to calculate color with blend using this.
            // this will intern call getColor(biome, x, z) for each sample
            try {
                return level.getBlockTint(pos, this);
            } catch (Exception e) {
                Polytone.LOGGER.error("Error getting block tint at {} with colormap {}. Was it during a reload?", pos, this, e);
                return 0;
            }
        }
        //else we sample normally
        Biome biome = null;
        if (usesBiome) {
            //RenderChunkRegion (or sodium slice) when meshing... could be a bad idea..
            LevelReader reader = level instanceof LevelReader l ? l : Minecraft.getInstance().level;
            if (reader != null) {
                biome = reader.getBiome(pos).value();
            }
        }

        return sampleColor(level, state, pos, biome, null);
    }

    @Override
    public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable BlockPos pos,
                           @Nullable Biome biome, @Nullable ItemStack item) {
        return sampleColorInternal(level, state, pos, biome, item, null, null);
    }

    @Override
    public int sampleColorOver(int original, @Nullable BlockAndTintGetter level, @Nullable BlockState state,
                               @Nullable BlockPos pos, @Nullable Biome biome, @Nullable ItemStack item) {
        return sampleColorInternal(level, state, pos, biome, item, original, null);
    }

    public int sampleColorForPreview(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome,
                                     @Nullable ItemStack item, @Nullable BlockAndTintGetter level,
                                     @Nullable SampleSink sink) {
        return sampleColorInternal(level, state, pos, biome, item, null, sink);
    }

    private int sampleColorInternal(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable BlockPos pos,
                                    @Nullable Biome biome, @Nullable ItemStack item, @Nullable Integer original,
                                    @Nullable SampleSink sink) {
        Vec3 vPos = pos == null ? null : pos.getCenter();
        float temperature = Mth.clamp(xGetter.evaluate(level, state, vPos, biome, biomeMapper, item), 0, 1);
        float humidity = Mth.clamp(yGetter.evaluate(level, state, vPos, biome, biomeMapper, item), 0, 1);

        int sampled = sample(humidity, temperature);

        boolean isHole = FastColor.ARGB32.alpha(sampled) == 0;

        boolean didSampleFallback = false;
        if (isHole) {
            if (fallback == ColormapsManager.ORIGINAL_MARKER) {
                int originalColor = sampleOriginal(original, biome, pos);
                if (originalColor != 0) {
                    sampled = originalColor;
                    didSampleFallback = true;
                }
            } else {
                sampled = sampleFallback(level, state, pos, biome, item, original);
                didSampleFallback = true;
            }
        }

        if (!didSampleFallback) {
            sampled |= 0xFF000000;
            if (colorMult != null) {
                sampled = colorMult.getValue(sampled, state, pos, biome, biomeMapper, item);
            }
        }
        if (sink != null) {
            long pixel = pixelIndex(humidity, temperature);
            sink.report(temperature, humidity, (int) (pixel >>> 32), (int) (pixel & 0xFFFFFFFFL), sampled);
        }
        return sampled;
    }

    //0 when theres none
    private int sampleOriginal(@Nullable Integer passedOriginal, @Nullable Biome biome, @Nullable BlockPos pos) {
        if (passedOriginal != null) return passedOriginal | 0xFF000000;

        int original = contextHack.get().sampleOriginal();
        if (original != 0) return original;
        if (biome == null || pos == null) return 0;

        ColorResolver replacement = Polytone.BLOCK_MODIFIERS.getVanillaFoliageOrGrass(this);
        if (replacement == null) return 0;
        return replacement.getColor(biome, pos.getX(), pos.getZ()) | 0xFF000000;
    }

    private int sampleFallback(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable BlockPos pos,
                               @Nullable Biome biome, @Nullable ItemStack item, @Nullable Integer original) {
        if (original != null) return fallback.sampleColorOver(original, level, state, pos, biome, item);
        return fallback.sampleColor(level, state, pos, biome, item);
    }

    public interface SampleSink {
        void report(float x, float y, int col, int row, int argb);
    }

    // gets color for blend
    @Override
    public int getColor(Biome biome, double x, double z) {
        //called per sample by the blend, vanilla's or sodium's
        ColormapContext ctx = contextHack.get();
        BlockAndTintGetter level = ctx.level() != null ? ctx.level() : Minecraft.getInstance().level;
        return sampleColor(level, ctx.state(), BlockPos.containing(x, ctx.y(), z), biome, null);
    }


    private int sample(float textY, float textX) {
        // dont ask questions here
        //if (Polytone.sodiumOn) return defValue;
        long pixel = pixelIndex(textY, textX);
        return image.pixels()[(int) (pixel & 0xFFFFFFFFL)][(int) (pixel >>> 32)];
    }

    private long pixelIndex(float textY, float textX) {
        if (triangular) textY *= textX;
        int wm = image.width() - 1;
        int hm = image.height() - 1;

        //gets rid of floating point errors for biome id map stuff
        int scaledW = rounds ? Math.round(textX * wm) : Mth.floor(textX * wm);
        int scaledH = rounds ? Math.round(textY * hm) : Mth.floor(textY * hm);
        int w = Math.max(wm - scaledW, 0);
        int h = Math.max(hm - scaledH, 0);
        return ((long) w << 32) | (h & 0xFFFFFFFFL);
    }


    //for items
    @Override
    public int getColor(ItemStack itemStack, int i) {
        BlockPos pos = null;
        Biome biome = null;
        if (usesPos) {
            // items have no position so we sample where the player stands. with use_player_position off we
            // instead act like vanilla does for grass/leaves items and give a fixed color
            if (!usePlayerPosition) return defaultColor;
            var player = Minecraft.getInstance().player;
            if (player == null) return defaultColor;
            pos = player.blockPosition();
            //we never ue blending. its overkill here
            if (usesBiome) {
                var level = Minecraft.getInstance().level;
                if (level == null) return defaultColor;
                biome = level.getBiome(pos).value();
            }
        }
        return sampleColor(Minecraft.getInstance().level, null, pos, biome, itemStack);
    }


    //factories


    //Square map with those 2 getters
    public static Colormap simple(IColormapExp xGetter, IColormapExp yGetter) {
        return new Colormap(xGetter, yGetter, false);
    }

    public static Colormap createFixed() {
        return new Colormap(Optional.empty(), IColormapExp.ZERO,
                IColormapExp.ZERO, false, true, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), true, ColormapsManager.ORIGINAL_MARKER);
    }

    //this is dumb. dont use
    public static Colormap singleColor(int color) {
        var c = new Colormap(IColormapExp.ZERO, IColormapExp.ZERO, false);
        c.acceptTexture(new ArrayImage(new int[][]{{color}}));
        return c;
    }

    public static Colormap createDefSquare() {
        return new Colormap(IColormapExp.TEMPERATURE, IColormapExp.DOWNFALL, false);
    }

    public static Colormap createDefTriangle() {
        return new Colormap(IColormapExp.TEMPERATURE, IColormapExp.DOWNFALL, true);
    }

    public static Colormap createTimeStrip() {
        return new Colormap(IColormapExp.DAY_TIME, IColormapExp.ZERO, false);
    }

    public static Colormap createBiomeId() {
        return new Colormap(Optional.empty(),
                IColormapExp.BIOME_ID,
                IColormapExp.Y_LEVEL,
                false, false, Optional.of(Boolean.TRUE), Optional.empty(), Optional.empty(),
                Optional.empty(), true, ColormapsManager.ORIGINAL_MARKER);
    }

    public static Colormap createDamage() {
        return new Colormap(IColormapExp.DAMAGE, IColormapExp.ZERO, false);
    }

}
