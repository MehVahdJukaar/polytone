package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
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
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

public class Colormap implements IColorGetter, ColorResolver {

    private final IColormapNumberProvider xGetter;
    private final IColormapNumberProvider yGetter;
    private final BiomeIdMapper biomeMapper;
    private final boolean triangular;
    private final boolean hasBiomeBlend; //if this should be used as ColorResolver, allowing for biome blend
    private final boolean usesBiome;
    private final boolean usesPos;
    private final boolean usesState;

    public boolean inlined = true;

    private Integer defaultColor;
    private ArrayImage image = null;
    private ResourceLocation explicitTargetTexture = null; //explicit targets

    private final ThreadLocal<BlockState> stateHack = new ThreadLocal<>();
    private final ThreadLocal<Integer> yHack = new ThreadLocal<>();

    public static final Codec<Colormap> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            ColorUtils.CODEC.optionalFieldOf("default_color").forGetter(c -> Optional.ofNullable(c.defaultColor)),
            IColormapNumberProvider.CODEC.fieldOf("x_axis").forGetter(c -> c.xGetter),
            IColormapNumberProvider.CODEC.fieldOf("y_axis").forGetter(c -> c.yGetter),
            Codec.BOOL.optionalFieldOf("triangular", false).forGetter(c -> c.triangular),
            Codec.BOOL.optionalFieldOf("biome_blend").forGetter(c -> Optional.of(c.hasBiomeBlend)),
            BiomeIdMapper.CODEC.optionalFieldOf("biome_id_mapper").forGetter(c -> Optional.of(c.biomeMapper)),
            ResourceLocation.CODEC.optionalFieldOf("texture_path").forGetter(c -> Optional.ofNullable(c.explicitTargetTexture))
    ).apply(i, Colormap::new));

    protected static final Codec<IColorGetter> SINGLE_COLOR_CODEC = ColorUtils.CODEC.xmap(
            Colormap::singleColor, c -> c instanceof Colormap cm ? cm.defaultColor : 0);

    public static final Codec<IColorGetter> COLORMAP_CODEC = Codec.withAlternative(SINGLE_COLOR_CODEC,
            new ReferenceOrDirectCodec<>(Polytone.COLORMAPS.byNameCodec(), DIRECT_CODEC),
            Function.identity());


    // single or biome compound
    public static final Codec<IColorGetter> CODEC = COLORMAP_CODEC;// Codec.either(BiomeCompoundBlockColors.DIRECT_CODEC, Colormap.COLORMAP_CODEC)
    //  .xmap(either -> either.map(Function.identity(), Function.identity()), Either::right);

    private Colormap(Optional<Integer> defaultColor, IColormapNumberProvider xGetter, IColormapNumberProvider yGetter,
                     boolean triangular, Optional<Boolean> biomeBlend, Optional<BiomeIdMapper> biomeMapper,
                     Optional<ResourceLocation> explicitTargetTexture) {
        this.defaultColor = defaultColor.orElse(null);
        this.xGetter = xGetter;
        this.yGetter = yGetter;
        this.triangular = triangular;
        this.usesBiome = (xGetter.usesBiome() || yGetter.usesBiome());
        this.usesPos = usesBiome || (xGetter.usesPos() || yGetter.usesPos());
        this.usesState = (xGetter.usesState() || yGetter.usesState());
        this.hasBiomeBlend = biomeBlend.orElse(usesBiome);
        this.biomeMapper = biomeMapper.orElse(BiomeIdMapper.BY_INDEX);
        this.explicitTargetTexture = explicitTargetTexture.orElse(null);
    }

    protected Colormap(IColormapNumberProvider xGetter, IColormapNumberProvider yGetter, boolean triangular) {
        this(Optional.empty(), xGetter, yGetter, triangular, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public void acceptTexture(ArrayImage image) {
        this.image = image;
        if (defaultColor == null) {
            this.defaultColor = sample(0.5f, 0.5f, -1);
        }
    }

    public boolean hasTexture() {
        return image != null;
    }

    protected ResourceLocation getExplicitTargetTexture() {
        return explicitTargetTexture;
    }

    public ResourceLocation getTargetTexture(ResourceLocation def) {
        return explicitTargetTexture != null ? explicitTargetTexture : def;
    }

    public void setExplicitTargetTexture(ResourceLocation imageTarget) {
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
            return level.getBlockTint(pos, this);
        }
        //else we sample normally
        Biome biome = null;
        if (usesBiome && level instanceof LevelReader l) {
            biome = l.getBiome(pos).value();
        }

        return sampleColor(state, pos, biome, null);
    }

    public int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome, @Nullable ItemStack item) {
        float temperature = Mth.clamp(xGetter.getValue(state, pos, biome, biomeMapper, item), 0, 1);
        float humidity = Mth.clamp(yGetter.getValue(state, pos, biome, biomeMapper, item), 0, 1);
        return sample(humidity, temperature, defaultColor);
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


    private int sample(float textY, float textX, int defValue) {
        // dont ask questions here
        //if (Polytone.sodiumOn) return defValue;
        if (triangular) textY *= textX;
        int width = image.width();
        int height = image.height();

        //gets rid of floating point errors for biome id map stuff
        int w = (int) Math.round((1.0 - textX) * (width - 1));
        int h = (int) Math.round((1.0 - textY) * (height - 1));

        return w >= width || h >= height ? defValue : image.pixels()[h][w];
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
        return sampleColor(null, pos, biome, itemStack);
    }


    //factories


    //Square map with those 2 getters
    public static Colormap simple(IColormapNumberProvider xGetter, IColormapNumberProvider yGetter) {
        return new Colormap(xGetter, yGetter, false);
    }

    public static Colormap createFixed() {
        return new Colormap(Optional.empty(), IColormapNumberProvider.ZERO,
                IColormapNumberProvider.ZERO, false, Optional.empty(), Optional.empty(), Optional.empty());
    }

    //this is dumb. dont use
    private static Colormap singleColor(int color) {
        var c = new Colormap(IColormapNumberProvider.ZERO, IColormapNumberProvider.ZERO, false);
        c.acceptTexture(new ArrayImage(new int[][]{{color}}));
        return c;
    }

    public static Colormap createDefSquare() {
        return new Colormap(IColormapNumberProvider.TEMPERATURE, IColormapNumberProvider.DOWNFALL, false);
    }

    public static Colormap createDefTriangle() {
        return new Colormap(IColormapNumberProvider.TEMPERATURE, IColormapNumberProvider.DOWNFALL, true);
    }

    public static Colormap createTimeStrip() {
        return new Colormap(IColormapNumberProvider.DAY_TIME, IColormapNumberProvider.ZERO, false);
    }

    public static Colormap createBiomeId() {
        return new Colormap(Optional.empty(),
                IColormapNumberProvider.BIOME_ID,
                IColormapNumberProvider.Y_LEVEL,
                false, Optional.of(Boolean.TRUE), Optional.empty(), Optional.empty());
    }

    public static Colormap createDamage() {
        return new Colormap(IColormapNumberProvider.DAMAGE, IColormapNumberProvider.ZERO, false);
    }
}