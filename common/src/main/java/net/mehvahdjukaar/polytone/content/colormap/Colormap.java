package net.mehvahdjukaar.polytone.content.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.IColormapExp;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.StrUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

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

    public boolean inlined = true;
    public ResourceLocation debugID = null;

    private Integer defaultColor;
    private ArrayImage image = null;
    private @Nullable ResourceLocation explicitTargetTexture; //explicit targets
    private final @Nullable ColormapColorModulatorExpression colorMult;

    private final ThreadLocal<BlockState> stateHack = new ThreadLocal<>();
    private final ThreadLocal<Integer> yHack = new ThreadLocal<>();
    // MVEL axis getters need the level for the `o`/`object` block proxy; the biome-blend and Sodium
    // ColorResolver paths reach sampleColor without one, so stash it here like stateHack/yHack.
    private final ThreadLocal<BlockAndTintGetter> levelHack = new ThreadLocal<>();

    static final Codec<Colormap> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            ColorUtils.CODEC.optionalFieldOf("default_color").forGetter(c -> Optional.ofNullable(c.defaultColor)),
            IColormapExp.CODEC.fieldOf("x_axis").forGetter(c -> c.xGetter),
            IColormapExp.CODEC.fieldOf("y_axis").forGetter(c -> c.yGetter),
            Codec.BOOL.optionalFieldOf("triangular", false).forGetter(c -> c.triangular),
            Codec.BOOL.optionalFieldOf("rounds", true).forGetter(c -> c.rounds),
            Codec.BOOL.optionalFieldOf("biome_blend").forGetter(c -> Optional.of(c.hasBiomeBlend)),
            BiomeIdMapper.CODEC.optionalFieldOf("biome_id_mapper").forGetter(c -> Optional.of(c.biomeMapper)),
            ResourceLocation.CODEC.optionalFieldOf("texture_path").forGetter(c -> Optional.ofNullable(c.explicitTargetTexture)),
            ColormapColorModulatorExpression.CODEC.optionalFieldOf("color_modifier").forGetter(c -> Optional.ofNullable(c.colorMult))
    ).apply(i, Colormap::new));

    public static final Codec<IColorGetter> SINGLE_COLOR_CODEC = ColorUtils.CODEC.xmap(
            Colormap::singleColor, c -> c instanceof Colormap cm ? cm.defaultColor : 0);

    // The reference branch is shown as a plain id box (the wire codec resolves it against the
    // colormap registry, and in the editor against the open pack too).
    public static final Codec<IColorGetter> DIRECT_REFERENCE_OR_EXPRESSION = SchemaCodecs.labeled(
            Codec.withAlternative(SINGLE_COLOR_CODEC,
                    SchemaCodecs.referenceOrDirect(Polytone.COLORMAPS.byNameCodec(), DIRECT_CODEC),
                    Function.identity()),
            // label with the un-xmapped ColorUtils.CODEC: SINGLE_COLOR_CODEC is xmapped, which drops
            // the Schema.Color tag, so it would resolve to the raw int-or-string union and splice into
            // stray "integer"/"text" options instead of showing a color picker.
            SchemaCodecs.alt("single color", ColorUtils.CODEC),
            SchemaCodecs.alt("colormap reference", ResourceLocation.CODEC),
            SchemaCodecs.alt("inline colormap", DIRECT_CODEC));

    public static final Codec<IColorGetter> REFERENCE_OR_EXPRESSION = SchemaCodecs.labeled(
            Codec.withAlternative(SINGLE_COLOR_CODEC, Polytone.COLORMAPS.byNameCodec()),
            SchemaCodecs.alt("single color", ColorUtils.CODEC),
            SchemaCodecs.alt("colormap reference", ResourceLocation.CODEC));

    // single or biome compound; the labeled alternatives of the first branch splice flat
    public static final Codec<IColorGetter> CODEC = SchemaCodecs.labeled(
            Codec.withAlternative(Colormap.DIRECT_REFERENCE_OR_EXPRESSION, BiomeCompoundColorGetter.CODEC),
            SchemaCodecs.alt("colormap", DIRECT_REFERENCE_OR_EXPRESSION),
            SchemaCodecs.alt("biome compound", BiomeCompoundColorGetter.CODEC));

    private Colormap(Optional<Integer> defaultColor, IColormapExp xGetter, IColormapExp yGetter,
                     boolean triangular, boolean rounds, Optional<Boolean> biomeBlend, Optional<BiomeIdMapper> biomeMapper,
                     Optional<ResourceLocation> explicitTargetTexture, Optional<ColormapColorModulatorExpression> colorMult) {
        this.defaultColor = defaultColor.orElse(null);
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

        if (this.hasBiomeBlend) PlatStuff.registerColorResolver(this);
    }

    private Colormap(IColormapExp xGetter, IColormapExp yGetter, boolean triangular) {
        this(Optional.empty(), xGetter, yGetter, triangular, true, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty());
    }

    // block tint, fluid tint need to have a concurrent expression.expression variable list needs to be thread safe
    public Colormap makeConcurrent() {
        Colormap concurrentColormap = new Colormap(Optional.ofNullable(this.defaultColor), this.xGetter.createConcurrent(),
                this.yGetter.createConcurrent(), this.triangular,
                this.rounds, Optional.of(this.hasBiomeBlend), Optional.of(this.biomeMapper),
                Optional.ofNullable(this.explicitTargetTexture), Optional.ofNullable(this.colorMult == null ? null : this.colorMult.createConcurrent()));
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
            this.defaultColor = sample(0.5f, 0.5f);
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

    // Dont use tint index
    @Override
    public int getColor(@Nullable BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int i) {
        if (level == null) return defaultColor;
        levelHack.set(level); // so sampleColor (incl. the biome-blend/Sodium detours) can feed MVEL axes the level
        if (pos == null && (usesPos || usesBiome)) {
            return defaultColor;
        }
        if (state == null && usesState) return defaultColor;
        if (hasBiomeBlend) {
            // ask the world to calculate color with blend using this.
            // this will intern call calculateBlendedColor which will call getColor/sampleColor
            stateHack.set(state); //pass block state arg like this
            yHack.set(pos != null ? pos.getY() : 0);
            try {
                return level.getBlockTint(pos, this);
            } catch (Exception e) {
                Polytone.LOGGER.error("Error getting block tint at {} with colormap {}. Was it during a reload?", pos, this, e);
                return 0;
            }
        }
        //else we sample normally
        Biome biome = null;
        if (usesBiome && level instanceof LevelReader l) {
            biome = l.getBiome(pos).value();
        }

        return sampleColor(state, pos, biome, null);
    }

    public int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable ItemStack item) {
        return sampleColor(state, pos, biome, item, null);
    }

    // A non-null sink reports the intermediates (axis outputs, sampled pixel, final argb) right where
    // they are computed, so tooling never needs a second copy of the sampling math. Null is the runtime
    // path and costs a null check.
    public int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome,
                           @Nullable ItemStack item, @Nullable SampleSink sink) {
        BlockAndTintGetter level = levelHack.get();
        Vec3 vPos = pos == null ? null : pos.getCenter();
        float temperature = Mth.clamp(xGetter.evaluate(level, state, vPos, biome, biomeMapper, item), 0, 1);
        float humidity = Mth.clamp(yGetter.evaluate(level, state, vPos, biome, biomeMapper, item), 0, 1);
        int sampled = sample(humidity, temperature);

        if (colorMult != null) {
            sampled = colorMult.getValue(sampled, state, pos, biome, biomeMapper, item);
        }
        if (sink != null) {
            long pixel = pixelIndex(humidity, temperature);
            sink.report(temperature, humidity, (int) (pixel >>> 32), (int) (pixel & 0xFFFFFFFFL), sampled);
        }
        return sampled;
    }

    public interface SampleSink {
        // x/y are the clamped axis outputs (0..1); col/row is the sampled source-image pixel; argb is the final tint.
        void report(float x, float y, int col, int row, int argb);
    }

    // Editor-preview entry: puts an explicit level into the same ThreadLocal the runtime paths use (so the
    // axis expressions and MVEL block proxy see it), then runs the instrumented sampler. Not a second
    // sampler - just level plumbing plus a delegate.
    public int sampleColorForPreview(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome,
                                     @Nullable ItemStack item, @Nullable BlockAndTintGetter level,
                                     @Nullable SampleSink sink) {
        this.levelHack.set(level);
        return sampleColor(state, pos, biome, item, sink);
    }

    // gets color for blend
    @Override
    public int getColor(Biome biome, double x, double z) {
        //this actually gets called when sodium is on as we cant define our own blend method
        Integer y = yHack.get();
        if (y == null) y = 0;
        return this.sampleColor(stateHack.get(), BlockPos.containing(x, y, z), biome, null);
    }

    //calculate color blend. could just use vanilla impl tbh since we got above hack for sodium anyway
    public int calculateBlendedColor(Level level, BlockPos pos) {
        //Same as vanilla impl. We could have just called it. Just here so we call sampleColor instead of getColor with pos instead of x z
        levelHack.set(level);
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        BlockState state = stateHack.get();
        if (i == 0) {
            return this.sampleColor(state, pos, level.getBiome(pos).value(), null);
        } else {
            int j = (i * 2 + 1) * (i * 2 + 1);
            int k = 0;
            int l = 0;
            int m = 0;
            Cursor3D cursor3D = new Cursor3D(pos.getX() - i, pos.getY(), pos.getZ() - i, pos.getX() + i, pos.getY(), pos.getZ() + i);

            int n;
            for (BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(); cursor3D.advance(); m += n & 255) {
                mutableBlockPos.set(cursor3D.nextX(), cursor3D.nextY(), cursor3D.nextZ());
                n = this.sampleColor(state, mutableBlockPos, level.getBiome(mutableBlockPos).value(), null);
                k += (n & 16711680) >> 16;
                l += (n & '\uff00') >> 8;
            }

            return (k / j & 255) << 16 | (l / j & 255) << 8 | m / j & 255;
        }
    }


    private int sample(float textY, float textX) {
        // dont ask questions here
        //if (Polytone.sodiumOn) return defValue;
        long pixel = pixelIndex(textY, textX);
        return image.pixels()[(int) (pixel & 0xFFFFFFFFL)][(int) (pixel >>> 32)];
    }

    // Maps the two axis outputs to a source-image pixel. High 32 bits = column (x/temperature axis),
    // low 32 bits = row (y/humidity axis). Single source of truth shared by sample() and the sink path.
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
        levelHack.set(Minecraft.getInstance().level);
        return sampleColor(null, pos, biome, itemStack);
    }


    //factories


    //Square map with those 2 getters
    public static Colormap simple(IColormapExp xGetter, IColormapExp yGetter) {
        return new Colormap(xGetter, yGetter, false);
    }

    public static Colormap createFixed() {
        return new Colormap(Optional.empty(), IColormapExp.ZERO,
                IColormapExp.ZERO, false, true, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    //this is dumb. dont use
    private static Colormap singleColor(int color) {
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
                Optional.empty());
    }

    public static Colormap createDamage() {
        return new Colormap(IColormapExp.DAMAGE, IColormapExp.ZERO, false);
    }


    //texture stuff
}
