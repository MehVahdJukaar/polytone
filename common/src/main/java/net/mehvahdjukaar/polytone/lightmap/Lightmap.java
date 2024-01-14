package net.mehvahdjukaar.polytone.lightmap;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.color.ColorManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3f;

public class Lightmap {

    public static final Decoder<Lightmap> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(LightmapNumberProvider.CODEC, "sky_getter", LightmapNumberProvider.DEFAULT)
                            .forGetter(l -> l.skyGetter),
                    StrOpt.of(LightmapNumberProvider.CODEC, "torch_getter", LightmapNumberProvider.DEFAULT)
                            .forGetter(l -> l.torchGetter)
            ).apply(instance, Lightmap::new));


    private final LightmapNumberProvider skyGetter;
    private final LightmapNumberProvider torchGetter;
    private final ArrayImage[] textures = new ArrayImage[3];


    public Lightmap(LightmapNumberProvider skyGetter, LightmapNumberProvider torchGetter) {
        this.skyGetter = skyGetter;
        this.torchGetter = torchGetter;
    }

    //default impl
    public Lightmap() {
        this(LightmapNumberProvider.DEFAULT, LightmapNumberProvider.RANDOM);
    }

    public void acceptImages(ArrayImage normal, ArrayImage rain, ArrayImage thunder) {
        this.textures[0] = normal;
        this.textures[1] = rain;
        this.textures[2] = thunder;
    }


    public float getSkyDarken(Level level, float partialTick) {
        float f = level.getTimeOfDay(partialTick);
        float g = 1.0F - (Mth.cos(f * Mth.TWO_PI) * 2.0F + 0.2F);
        g = Mth.clamp(g, 0.0F, 1.0F);
        g = 1.0F - g;
        g *= 1.0F - level.getRainLevel(partialTick) * 5.0F / 16.0F;
        g *= 1.0F - level.getThunderLevel(partialTick) * 5.0F / 16.0F;
        return g * 0.8F + 0.2F;
    }

    public boolean applyToLightTexture(LightTexture instance,
                                       NativeImage lightPixels,
                                       DynamicTexture lightTexture,
                                       Minecraft minecraft, ClientLevel level,
                                       float flicker, float partialTicks) {

        //this wasn't using partial ticks for some reasons
        float skyDarken = level.getSkyDarken(partialTicks);
        float rainLevel = level.getRainLevel(partialTicks);
        float thunderLevel = level.getThunderLevel(partialTicks);


        ArrayImage targetTexture = textures;

        RandomSource randomSource = RandomSource.create(Float.floatToIntBits(flicker));

        LocalPlayer player = minecraft.player;
        Options options = minecraft.options;

        float skyFlashTime;
        if (level.getSkyFlashTime() > 0) {
            skyFlashTime = 1.0F;
        } else {
            skyFlashTime = skyDarken * 0.95F + 0.05F;
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
        boolean endBright = level.effects().forceBrightLightmap();
        DimensionType dimensionType = level.dimensionType();

        float darkenWorldAmount = minecraft.gameRenderer.getDarkenWorldAmount(partialTicks);


        Vector3f lightGray = new Vector3f(0.75F, 0.75F, 0.75F);
        float lightGrayAmount = 0.04F;

        float[][] torchLine = selectTorch(targetTexture, randomSource);

        float[][] skyLine = selectSky(targetTexture, level);


        for (int skyY = 0; skyY < 16; ++skyY) {
            float skyLightIntensity = LightTexture.getBrightness(dimensionType, skyY) * skyFlashTime;

            Vector3f skyBuffer = new Vector3f(skyColor)
                    .mul(skyLightIntensity);

            skyBuffer.mul(1 - lightGrayAmount);

            for (int torchX = 0; torchX < 16; ++torchX) {
                Vector3f addition = new Vector3f();
                Vector3f torchBuffer = new Vector3f();

                if (torchBuffer.length() != 0) {
                    new Vector3f(torchLine[torchX]).mul(blockLightFlicker);
                } else {
                    float torchR = LightTexture.getBrightness(dimensionType, torchX) * blockLightFlicker;
                    float torchG = torchR * ((torchR * 0.6F + 0.4F) * 0.6F + 0.4F);
                    float torchB = torchR * (torchR * torchR * 0.6F + 0.4F);
                    torchBuffer.set(torchR, torchG, torchB);
                }

                //vanilla logic
                addition.add(new Vector3f(lightGray).mul(lightGrayAmount));

                //torchBuffer.lerp(lightGray, lightGrayAmount);
                torchBuffer.mul(1 - lightGrayAmount);


                Vector3f combined = new Vector3f();

                combined.add(torchBuffer).add(skyBuffer).add(addition);


                //TODO:
                if (darkenWorldAmount > 0.0F) {
                    Vector3f discolored = (new Vector3f(combined)).mul(0.7F, 0.6F, 0.6F);
                    combined.lerp(discolored, darkenWorldAmount);
                }

                if (nightVisionScale > 0.0F) {
                    float maxVal = Math.max(combined.x(), Math.max(combined.y(), combined.z()));
                    if (maxVal < 1.0F) {
                        float percentage = 1.0F / maxVal;
                        Vector3f discolored = (new Vector3f(combined)).mul(percentage);
                        combined.lerp(discolored, nightVisionScale);
                    }
                }

                //we make botht hse happen in end too
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
                lightPixels.setPixelRGBA(torchX, skyY, -16777216 | z << 16 | y << 8 | x);
            }

            lightTexture.upload();
            minecraft.getProfiler().pop();
        }
    }

    private float[][] selectSky(boolean nightVision, float time, float rain, float thunder,
                                boolean thunderFlash) {

        ArrayImage image = textures[0];

        float xVal = skyGetter.getValue(time, rain, thunder);

        float[][] skyLine = new float[16][];
        int h = ((nightVision && image.height() == 64) ? 32 : 0);
        for (int i = 0; i < 16; i++) {
            skyLine[i] = ColorManager.unpack(image.pixels()[h + i][(int) (xVal * image.width())]);
        }
        return skyLine;
    }

    private float[][] selectTorch(boolean nightVision, float time, float rain, float thunder) {
        ArrayImage image = textures[0];

        if (image.height() < 32) {
            return new float[][]{};
        }
        float xVal = torchGetter.getValue(time, rain, thunder);
        //simulate torch flicker
        float[][] torchLine = new float[16][];
        int h = 16 + ((nightVision && image.height() == 64) ? 32 : 0);
        for (int i = 0; i < 16; i++) {
            torchLine[i] = ColorManager.unpack(image.pixels()[h + i][(int) (xVal * image.width())]);
        }
        return torchLine;
    }

    private static void clampColor(Vector3f color) {
        color.set(Mth.clamp(color.x, 0.0F, 1.0F),
                Mth.clamp(color.y, 0.0F, 1.0F),
                Mth.clamp(color.z, 0.0F, 1.0F));
    }


}
