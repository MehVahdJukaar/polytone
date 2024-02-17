package net.mehvahdjukaar.polytone.colormap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.biome.BiomeIdMapper;
import net.mehvahdjukaar.polytone.biome.BiomeIdMapperManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Colormap implements ColorResolver, BlockColor {

    private final IColormapNumberProvider xGetter;
    private final IColormapNumberProvider yGetter;
    private final BiomeIdMapper biomeMapper;
    private final boolean triangular;
    private final boolean hasBiomeBlend; //if this should be used as ColorResolver, allowing for biome blend
    private final boolean usesBiome;
    private final boolean usesPos;
    private final boolean usesState;

    private Integer defaultColor = null;
    private ArrayImage image = null;

    private final ThreadLocal<BlockState> stateHack = new ThreadLocal<>();
    private final ThreadLocal<Integer> yHack = new ThreadLocal<>();

    public static final Codec<Colormap> DIRECT_CODEC = RecordCodecBuilder.create(i -> i.group(
            StrOpt.of(Codec.INT, "default_color").forGetter(c -> Optional.ofNullable(c.defaultColor)),
            IColormapNumberProvider.CODEC.fieldOf("x_axis").forGetter(c -> c.xGetter),
            IColormapNumberProvider.CODEC.fieldOf("y_axis").forGetter(c -> c.yGetter),
            StrOpt.of(Codec.BOOL, "triangular", false).forGetter(c -> c.triangular),
            StrOpt.of(Codec.BOOL, "biome_blend").forGetter(c -> Optional.of(c.hasBiomeBlend)),
            StrOpt.of(BiomeIdMapperManager.CODEC, "biome_mapper").forGetter(c->Optional.of(c.biomeMapper))
    ).apply(i, Colormap::new));

    protected static final Codec<BlockColor> REFERENCE_CODEC = ResourceLocation.CODEC.flatXmap(
            id -> Optional.ofNullable(Polytone.COLORMAPS.get(id)).map(DataResult::success)
                    .orElse(DataResult.error( "Could not find a custom Colormap with id " + id +
                            " Did you place it in 'assets/[your pack]/polytone/colormaps/' ?")),
            object -> Optional.ofNullable(Polytone.COLORMAPS.getKey(object)).map(DataResult::success)
                    .orElse(DataResult.error( "Unknown Color Property: " + object)));

    public static final Codec<BlockColor> CODEC = new ReferenceOrDirectCodec<>(REFERENCE_CODEC, DIRECT_CODEC);

    private Colormap(Optional<Integer> defaultColor, IColormapNumberProvider xGetter, IColormapNumberProvider yGetter,
                     boolean triangular, Optional<Boolean> biomeBlend, Optional<BiomeIdMapper> biomeMapper) {
        this.defaultColor = defaultColor.orElse(null);
        this.xGetter = xGetter;
        this.yGetter = yGetter;
        this.triangular = triangular;
        this.usesBiome = (xGetter.usesBiome() || yGetter.usesBiome());
        this.usesPos = usesBiome || (xGetter.usesPos() || yGetter.usesPos());
        this.usesState = (xGetter.usesState() || yGetter.usesState());
        this.hasBiomeBlend = biomeBlend.orElse(usesBiome);
        this.biomeMapper = biomeMapper.orElse(BiomeIdMapperManager.BY_INDEX);
    }

    protected Colormap(IColormapNumberProvider xGetter, IColormapNumberProvider yGetter) {
        this(Optional.empty(), xGetter, yGetter, false, Optional.empty(), Optional.empty());
    }

    //Square map with those 2 getters
    public static Colormap simple(IColormapNumberProvider xGetter, IColormapNumberProvider yGetter) {
        return new Colormap(xGetter, yGetter);
    }

    public static Colormap fixed() {
        return new Colormap(Optional.empty(), IColormapNumberProvider.ZERO,
                IColormapNumberProvider.ZERO, false, Optional.empty(), Optional.empty());
    }

    public static Colormap defSquare() {
        return new Colormap(Optional.empty(),
                IColormapNumberProvider.TEMPERATURE, IColormapNumberProvider.DOWNFALL, false, Optional.empty(), Optional.empty());
    }

    public static Colormap defTriangle() {
        return new Colormap(Optional.empty(),
                IColormapNumberProvider.TEMPERATURE, IColormapNumberProvider.DOWNFALL, true, Optional.empty(), Optional.empty());
    }

    public static Colormap biomeId() {
        return new Colormap(Optional.empty(),
                IColormapNumberProvider.BIOME_ID,
                IColormapNumberProvider.Y_LEVEL,
                false, Optional.of(Boolean.TRUE),  Optional.empty());

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
            stateHack.set(state); //pass blockstate arg like this
            yHack.set(pos != null ? pos.getY() : 0);
            return level.getBlockTint(pos, this);
        }
        //else we sample normally
        return sampleColor(state, pos, null);
    }

    private int sampleColor(@Nullable BlockState state, @Nullable BlockPos pos, @Nullable Biome biome) {
        float humidity = Mth.clamp(xGetter.getValue(state, pos, biome), 0, 1);
        float temperature = Mth.clamp(yGetter.getValue(state, pos, biome), 0, 1);
        return sample(humidity, temperature, defaultColor);
    }

    //gets color for blend
    @Override
    public int getColor(Biome biome, double x, double z) {
        //this actually gets called when sodium is on as we cant define our own blend method
        Integer y = yHack.get();
        if (y == null) y = 0;
        return this.sampleColor(stateHack.get(), BlockPos.containing(x, y, z), biome);
    }

    //calculate color blend. could just use vanilla impl tbh since we got above hack for sodium anyway
    public int calculateBlendedColor(Level level, BlockPos pos) {
        //Same as vanilla impl. We could have just called it. Just here so we call sampleColor instead of getColor with pos instead of x z
        int i = Minecraft.getInstance().options.biomeBlendRadius().get();
        BlockState state = stateHack.get();
        if (i == 0) {
            return this.sampleColor(state, pos, level.getBiome(pos).value());
        } else {
            int j = (i * 2 + 1) * (i * 2 + 1);
            int k = 0;
            int l = 0;
            int m = 0;
            Cursor3D cursor3D = new Cursor3D(pos.getX() - i, pos.getY(), pos.getZ() - i, pos.getX() + i, pos.getY(), pos.getZ() + i);

            int n;
            for (BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(); cursor3D.advance(); m += n & 255) {
                mutableBlockPos.set(cursor3D.nextX(), cursor3D.nextY(), cursor3D.nextZ());
                n = this.sampleColor(state, mutableBlockPos, level.getBiome(mutableBlockPos).value());
                k += (n & 16711680) >> 16;
                l += (n & '\uff00') >> 8;
            }

            return (k / j & 255) << 16 | (l / j & 255) << 8 | m / j & 255;
        }
    }


    private int sample(float x, float y, int defValue) {
        //if (Polytone.sodiumOn) return defValue;
        if (triangular) x *= y;
        int width = image.width();
        int height = image.height();

        int w = (int) ((1.0 - y) * (width - 1));
        int h = (int) ((1.0 - x) * (height - 1));

        return w >= width || h >= height ? defValue : image.pixels()[h][w];
    }
}