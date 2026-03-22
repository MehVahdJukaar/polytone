package net.mehvahdjukaar.polytone.content.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.codec.CodecUtils;
import net.mehvahdjukaar.polytone.common.expressions.impl.IColormapExp;
import net.mehvahdjukaar.polytone.common.struc.ArrayImage;
import net.mehvahdjukaar.polytone.content.biome.BiomeIdMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.resources.Identifier;
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
    Identifier debugID = null;

    private Integer defaultColor;
    private ArrayImage image = null;
    private @Nullable Identifier explicitTargetTexture; //explicit targets
    private final @Nullable ColormapColorModulator colorMult;

    private final ThreadLocal<BlockState> stateHack = new ThreadLocal<>();
    private final ThreadLocal<Integer> yHack = new ThreadLocal<>();
    private final ThreadLocal<BlockAndTintGetter> levelHack = new ThreadLocal<>();

    static final Codec<Colormap> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            ColorUtils.COLOR.optionalFieldOf("default_color").forGetter(c -> Optional.ofNullable(c.defaultColor)),
            IColormapExp.CODEC.fieldOf("x_axis").forGetter(c -> c.xGetter),
            IColormapExp.CODEC.fieldOf("y_axis").forGetter(c -> c.yGetter),
            Codec.BOOL.optionalFieldOf("triangular", false).forGetter(c -> c.triangular),
            Codec.BOOL.optionalFieldOf("rounds", true).forGetter(c -> c.rounds),
            Codec.BOOL.optionalFieldOf("biome_blend").forGetter(c -> Optional.of(c.hasBiomeBlend)),
            BiomeIdMapper.CODEC.optionalFieldOf("biome_id_mapper").forGetter(c -> Optional.of(c.biomeMapper)),
            Identifier.CODEC.optionalFieldOf("texture_path").forGetter(c -> Optional.ofNullable(c.explicitTargetTexture)),
            ColormapColorModulator.CODEC.optionalFieldOf("color_modifier").forGetter(c -> Optional.ofNullable(c.colorMult))
    ).apply(i, Colormap::new));

    public static final Codec<IColorGetter> REFERENCE_OR_EXPRESSION = Codec.withAlternative(
            Polytone.COLORMAPS.byNameCodec(), SINGLE_COLOR_OR_EXPRESSION);


    //direct reference or expression
    public static final Codec<IColorGetter> CODEC = CodecUtils.alternatives(
            CodecUtils.referenceOrDirect(Polytone.COLORMAPS.byNameCodec(), DIRECT_CODEC),
            SINGLE_COLOR_OR_EXPRESSION,
            BiomeCompoundColorGetter.CODEC);

    private Colormap(Optional<Integer> defaultColor, IColormapExp xGetter, IColormapExp yGetter,
                     boolean triangular, boolean rounds, Optional<Boolean> biomeBlend, Optional<BiomeIdMapper> biomeMapper,
                     Optional<Identifier> explicitTargetTexture, Optional<ColormapColorModulator> colorMult) {
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
        this.biomeMapper = biomeMapper.orElse(BiomeIdMapper.LEGACY);
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

    Identifier getExplicitTargetTexture() {
        return explicitTargetTexture;
    }

    public Identifier getTargetTexture(Identifier def) {
        return explicitTargetTexture != null ? explicitTargetTexture : def;
    }

    public void setExplicitTargetTexture(Identifier imageTarget) {
        this.explicitTargetTexture = imageTarget.withPath(imageTarget.getPath().replace(".png", ""));
    }

    // Dont use tint index
    @Override
    public int getColor(@Nullable BlockState state, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos, int i) {
        if (level == null) return defaultColor;
        if (pos == null && (usesPos || usesBiome)) {
            return defaultColor;
        }
        if (state == null && usesState) return defaultColor;
        if (hasBiomeBlend) {
            // ask the world to calculate color with blend using this.
            // this will intern call calculateBlendedColor which will call getColor/sampleColor
            stateHack.set(state); //pass block state arg like this
            yHack.set(pos != null ? pos.getY() : 0);
            levelHack.set(level);
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

        return sampleColor(level, state, pos == null ? null : pos.getCenter(), biome, null);
    }

    @Override
    public int sampleColor(@Nullable BlockAndTintGetter level, @Nullable BlockState state, @Nullable Vec3 pos,
                           @Nullable Biome biome, @Nullable ItemStack item) {
        float temperature = Mth.clamp(xGetter.evaluate(level, state, pos, biome, biomeMapper, item), 0, 1);
        float humidity = Mth.clamp(yGetter.evaluate(level, state, pos, biome, biomeMapper, item), 0, 1);
        int sampled = sample(humidity, temperature);

        if (colorMult != null) {
            sampled = colorMult.evaluate(sampled, level, state, pos, biome, biomeMapper, item);
        }
        return sampled;
    }

    // gets color for blend
    @Override
    public int getColor(Biome biome, double x, double z) {
        //this actually gets called when sodium is on as we cant define our own blend method
        Integer y = yHack.get();
        if (y == null) y = 0;
        return this.sampleColor(levelHack.get(), stateHack.get(), new Vec3(x, y, z), biome, null);
    }

    //calculate color blend. could just use vanilla impl tbh since we got above hack for sodium anyway
    public int calculateBlendedColor(Level level, Vec3 p) {
        BlockPos pos = BlockPos.containing(p);
        //Same as vanilla impl. We could have just called it. Just here so we call sampleColor instead of getColor with pos instead of x z
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        BlockState state = stateHack.get();
        if (i == 0) {
            return this.sampleColor(level, state, p, level.getBiome(pos).value(), null);
        } else {
            int j = (i * 2 + 1) * (i * 2 + 1);
            int k = 0;
            int l = 0;
            int m = 0;
            Cursor3D cursor3D = new Cursor3D(pos.getX() - i, pos.getY(), pos.getZ() - i, pos.getX() + i, pos.getY(), pos.getZ() + i);

            int n;
            for (BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(); cursor3D.advance(); m += n & 255) {
                mutableBlockPos.set(cursor3D.nextX(), cursor3D.nextY(), cursor3D.nextZ());
                n = this.sampleColor(level, state, mutableBlockPos.getCenter(), level.getBiome(mutableBlockPos).value(), null);
                k += (n & 16711680) >> 16;
                l += (n & '\uff00') >> 8;
            }

            return (k / j & 255) << 16 | (l / j & 255) << 8 | m / j & 255;
        }
    }


    private int sample(float textY, float textX) {
        // dont ask questions here
        //if (Polytone.sodiumOn) return defValue;
        if (triangular) textY *= textX;
        int width = image.width();
        int height = image.height();

        int wm = width - 1;
        int hm = height - 1;

        //gets rid of floating point errors for biome id map stuff
        int scaledW = rounds ? Math.round(textX * wm) : Mth.floor(textX * wm);
        int scaledH = rounds ? Math.round(textY * hm) : Mth.floor(textY * hm);
        int w = Math.max(wm - scaledW, 0);
        int h = Math.max(hm - scaledH, 0);


        return image.pixels()[h][w];
    }


    //for items
    @Override
    public int getItemColor(ItemStack itemStack, int i) {
        Vec3 pos = null;
        Biome biome = null;
        var level = Minecraft.getInstance().level;
        var player = Minecraft.getInstance().player;
        if (player == null) return defaultColor;
        pos = player.position();
        //we never ue blending. its overkill here
        if (usesBiome) {
            if (level == null) return defaultColor;
            biome = level.getBiome(BlockPos.containing(pos)).value();
        }
        return sampleColor(player.level(), null, pos, biome, itemStack);
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

}
