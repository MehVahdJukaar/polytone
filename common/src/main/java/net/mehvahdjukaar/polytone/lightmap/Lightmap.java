package net.mehvahdjukaar.polytone.lightmap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.ColorUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class Lightmap {

    protected static final double DEFAULT_SKY_LERP = 0.5;
    protected static final double DEFAULT_TORCH_LERP = 0;
    public static final Decoder<Lightmap> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ILightmapNumberProvider.CODEC.optionalFieldOf("sky_getter", ILightmapNumberProvider.DEFAULT)
                            .forGetter(l -> l.skyGetter),
                    ILightmapNumberProvider.CODEC.optionalFieldOf("torch_getter", ILightmapNumberProvider.DEFAULT)
                            .forGetter(l -> l.torchGetter),
                    Codec.BOOL.optionalFieldOf("lightning_strike_columns", true)
                            .forGetter(l -> l.hasLightningColumn),
                    doubleRange(0, 1).optionalFieldOf("sky_lerp_factor", DEFAULT_SKY_LERP)
                            .forGetter(l -> l.skyLerp),
                    doubleRange(0, 1).optionalFieldOf("torch_lerp_factor", DEFAULT_TORCH_LERP)
                            .forGetter(l -> l.torchLerp)
            ).apply(instance, Lightmap::new));

    public static Codec<Double> doubleRange(double min, double max) {
        return Codec.DOUBLE.validate(d -> d.compareTo(min) >= 0 && d.compareTo(max) <= 0 ?
                DataResult.success(d) : DataResult.error(() -> "Value must be within range [" + min + ";" + max + "]: " + d));
    }


    private final ILightmapNumberProvider skyGetter;
    private final ILightmapNumberProvider torchGetter;
    private final boolean hasLightningColumn;
    private final double skyLerp;
    private final double torchLerp;
    private final ArrayImage[] textures = new ArrayImage[3];

    private final float[][] lastSkyLine = new float[16][3];
    private final float[][] lastTorchLine = new float[16][3];


    public Lightmap(ILightmapNumberProvider skyGetter, ILightmapNumberProvider torchGetter,
                    boolean lightningColumn, double skyLerp, double torchLerp) {
        this.skyGetter = skyGetter;
        this.torchGetter = torchGetter;
        this.hasLightningColumn = lightningColumn;
        this.skyLerp = skyLerp;
        this.torchLerp = torchLerp;
    }

    //default impl
    public Lightmap() {
        this(ILightmapNumberProvider.DEFAULT, ILightmapNumberProvider.RANDOM, true, DEFAULT_SKY_LERP, DEFAULT_TORCH_LERP);
    }

    public void acceptImages(ArrayImage normal, ArrayImage rain, ArrayImage thunder) {
        this.textures[0] = normal;
        this.textures[1] = rain;
        this.textures[2] = thunder;
        for (var v : textures) {
            if (v != null && v.width() <= 2) {
                throw new IllegalStateException("Lightmap cannot have more with is too small! Was " + v.width());
            }
        }
    }


    public void applyToLightTexture(LightTexture instance,
                                    NativeImage lightPixels,
                                    DynamicTexture lightTexture,
                                    Minecraft minecraft, ClientLevel level,
                                    float flicker, float partialTicks) {

        // this makes a copy
        var oldTexture = lightPixels.getPixelsRGBA();
        boolean needsUpload = false;

        //this wasn't using partial ticks for some reasons
        float skyDarken = level.getSkyDarken(partialTicks);
        float rainLevel = level.getRainLevel(partialTicks);
        float thunderLevel = level.getThunderLevel(partialTicks);
        float time = level.getTimeOfDay(partialTicks);
        float deltaTime = minecraft.getDeltaFrameTime();

        LocalPlayer player = minecraft.player;
        Options options = minecraft.options;

        float skyLightIntensity;
        boolean skyFlashTime = level.getSkyFlashTime() > 0;
        if (skyFlashTime) {
            skyLightIntensity = 1.0F;
        } else {
            skyLightIntensity = skyDarken * 0.95F + 0.05F;
        }

        float darknessEffect = options.darknessEffectScale().get().floatValue();
        float darknessGamma = instance.getDarknessGamma(partialTicks) * darknessEffect;
        float darknessSubtract = instance.calculateDarknessScale(player, darknessGamma, partialTicks) * darknessEffect;

        float gamma = (options.gamma().get()).floatValue();
        float gammaAmount = Math.max(0.0F, gamma - darknessGamma);

        float waterVision = player.getWaterVision();
        float nightVisionScale;
        if (player.hasEffect(MobEffects.NIGHT_VISION)) {
            nightVisionScale = GameRenderer.getNightVisionScale(player, partialTicks);
        } else if (waterVision > 0.0F && player.hasEffect(MobEffects.CONDUIT_POWER)) {
            nightVisionScale = waterVision;
        } else {
            nightVisionScale = 0.0F;
        }

        Vector3f skyColor = (new Vector3f(skyDarken, skyDarken, 1.0F))
                .lerp(new Vector3f(1.0F, 1.0F, 1.0F), 0.35F);
        float blockLightFlicker = flicker + 1.5F;
        //boolean endBright = level.effects().forceBrightLightmap();
        DimensionType dimensionType = level.dimensionType();

        float darkenWorldAmount = minecraft.gameRenderer.getDarkenWorldAmount(partialTicks);


        Vector3f lightGray = new Vector3f(0.75F, 0.75F, 0.75F);
        float lightGrayAmount = 0.04F;

        ArrayImage image = selectImage(rainLevel, thunderLevel);
        float[][] torchLine = selectTorch(image, nightVisionScale, time, rainLevel, thunderLevel);
        float[][] skyLine = selectSky(image, nightVisionScale, time, rainLevel, thunderLevel, skyFlashTime);

        // we need pow to simulate multiple lerps

        //lerp!

        if (torchLine.length != 0 && lastTorchLine.length != 0 && torchLerp != 1) {
            float lerpDelta = 1 - (float) Math.pow(torchLerp, deltaTime);
            lerpInplace(lastTorchLine, torchLine, deltaTime);
        }
        if (skyLine.length != 0 && lastSkyLine.length != 0 && skyLerp != 1) {
            float lerpDelta = 1 - (float) Math.pow(skyLerp, deltaTime);
            lerpInplace(lastSkyLine, skyLine, deltaTime);
        }

        for (int skyY = 0; skyY < 16; ++skyY) {
            Vector3f skyBuffer = new Vector3f();

            if (skyLine.length != 0) {
                skyBuffer.add(new Vector3f(skyLine[skyY]));
            } else {
                // we have no colors. use vanilla logic
                float skyBrightness = LightTexture.getBrightness(dimensionType, skyY) * skyLightIntensity;
                skyBuffer.add(skyColor).mul(skyBrightness);
                skyBuffer.mul(1 - lightGrayAmount);
            }

            for (int torchX = 0; torchX < 16; ++torchX) {
                Vector3f addition = new Vector3f();
                Vector3f torchBuffer = new Vector3f();

                if (torchLine.length != 0) {
                    torchBuffer.add(new Vector3f(torchLine[torchX]));
                } else {
                    // we have no colors. use vanilla logic
                    float torchR = LightTexture.getBrightness(dimensionType, torchX) * blockLightFlicker;
                    float torchG = torchR * ((torchR * 0.6F + 0.4F) * 0.6F + 0.4F);
                    float torchB = torchR * (torchR * torchR * 0.6F + 0.4F);
                    torchBuffer.set(torchR, torchG, torchB);

                    //vanilla logic
                    addition.add(new Vector3f(lightGray).mul(lightGrayAmount));

                    //torchBuffer.lerp(lightGray, lightGrayAmount);
                    torchBuffer.mul(1 - lightGrayAmount);
                }


                Vector3f combined = new Vector3f();

                combined.add(torchBuffer).add(skyBuffer).add(addition);

                if (darkenWorldAmount > 0.0F) {
                    Vector3f discolored = (new Vector3f(combined)).mul(0.7F, 0.6F, 0.6F);
                    combined.lerp(discolored, darkenWorldAmount);
                }

                if (nightVisionScale > 0.0F && (image == null || image.height() < 32)) {
                    float maxVal = Math.max(combined.x(), Math.max(combined.y(), combined.z()));
                    if (maxVal < 1.0F) {
                        float percentage = 1.0F / maxVal;
                        Vector3f discolored = (new Vector3f(combined)).mul(percentage);
                        combined.lerp(discolored, nightVisionScale);
                    }
                }

                //we make both hse happen in end too
                if (darknessSubtract > 0.0F) {
                    combined.add(-darknessSubtract, -darknessSubtract, -darknessSubtract);
                }

                clampColor(combined);

                //apply gamma
                Vector3f notGamma = new Vector3f(
                        instance.notGamma(combined.x),
                        instance.notGamma(combined.y),
                        instance.notGamma(combined.z));
                combined.lerp(notGamma, gammaAmount);
                //guess this makes it so its never pitch dark
                combined.lerp(lightGray, lightGrayAmount);
                clampColor(combined);
                combined.mul(255.0F);

                int x = (int) combined.x();
                int y = (int) combined.y();
                int z = (int) combined.z();
                int newColor = -16777216 | z << 16 | y << 8 | x;
                lightPixels.setPixelRGBA(torchX, skyY, newColor);

                if (newColor != oldTexture[skyY * 16 + torchX]) {
                    needsUpload = true;
                }
            }
        }

        if (needsUpload) {
            lightTexture.upload();
        }
    }

    private float[][] selectSky(ArrayImage image, float nightVision, float time, float rain, float thunder,
                                boolean isThunderFlash) {
        if (image == null) {
            return new float[][]{};
        }
        float xVal = skyGetter.getValue(time, rain, thunder);

        float[][] skyLine = new float[16][];

        int usableSkyWidth = image.width() - 1 - (this.hasLightningColumn ? 1 : 0);
        int w;
        if (!isThunderFlash || !hasLightningColumn) {
            if (isThunderFlash) {
                //default thunder lightmap
                w = usableSkyWidth;
            } else {
                //no thunder
                w = Math.round(xVal * usableSkyWidth);
            }
        } else {
            //thunder lightmap
            w = usableSkyWidth + 1;
        }
        int h = ((nightVision != 0 && image.height() == 64) ? 32 : 0);
        for (int i = 0; i < 16; i++) {
            skyLine[i] = ColorUtils.unpack(image.pixels()[h + i][w]);
        }
        return skyLine;
    }


    private float[][] selectTorch(ArrayImage image, float nightVision, float time, float rain, float thunder) {

        if (image == null || image.height() < 32) {
            return new float[][]{};
        }
        float xVal = torchGetter.getValue(time, rain, thunder);
        //simulate torch flicker
        float[][] torchLine = new float[16][];
        int h = 16 + ((nightVision != 0 && image.height() == 64) ? 32 : 0);
        for (int i = 0; i < 16; i++) {
            torchLine[i] = ColorUtils.unpack(image.pixels()[h + i][(int) (xVal * (image.width() - 1))]);
        }
        return torchLine;
    }

    @Nullable
    private ArrayImage selectImage(float rain, float thunder) {
        ArrayImage image;
        if (thunder != 0) {
            image = textures[2];
            if (image == null) {
                image = textures[0];
            }
        } else if (rain != 0) {
            image = textures[1];
            if (image == null) {
                image = textures[0];
            }
        } else image = textures[0];
        return image;
    }

    private static void clampColor(Vector3f color) {
        color.set(Mth.clamp(color.x, 0.0F, 1.0F),
                Mth.clamp(color.y, 0.0F, 1.0F),
                Mth.clamp(color.z, 0.0F, 1.0F));
    }

    public static void lerpInplace(float[][] oldColors, float[][] newColors, float delta) {
        if (oldColors.length != newColors.length || oldColors[0].length != newColors[0].length) {
            throw new IllegalArgumentException("Input arrays must have the same dimensions.");
        }

        int numRows = oldColors.length;
        int numCols = oldColors[0].length;

        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                newColors[i][j] = Mth.lerp(delta, oldColors[i][j], newColors[i][j]);
            }
        }
        //saves for next cycle
        for (int i = 0; i < numRows; i++) {
            System.arraycopy(newColors[i], 0, oldColors[i], 0, numCols);
        }
    }

}
