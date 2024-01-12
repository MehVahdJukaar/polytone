package net.mehvahdjukaar.polytone.lightmap;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.color.ColorManager;
import net.mehvahdjukaar.polytone.utils.ArrayImage;
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
import net.minecraft.world.level.dimension.DimensionType;
import org.joml.Vector3f;

public class Lightmap {
    private final ArrayImage image;
    private final boolean hasNightVision;

    public Lightmap(ArrayImage image) {
        this.image = image;
        hasNightVision = image.height() > 32;
    }


    public void applyToLightTexture(LightTexture instance,
                                    NativeImage lightPixels,
                                    DynamicTexture lightTexture,
                                    Minecraft minecraft, ClientLevel level,
                                    float flicker, float partialTicks) {

        Lighting.setupForFlatItems();
        RandomSource randomSource = RandomSource.create(Float.floatToIntBits(flicker));

        LocalPlayer player = minecraft.player;
        Options options = minecraft.options;

        //this wasn't using partial ticks for some reasons
        float skyDarken = level.getSkyDarken(partialTicks);
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

        float[][] torchLine = selectTorch(randomSource);


        for (int skyY = 0; skyY < 16; ++skyY) {
            float skyLightIntensity = LightTexture.getBrightness(dimensionType, skyY) * skyFlashTime;

            Vector3f skyBuffer = new Vector3f(skyColor)
                    .mul(skyLightIntensity);


            //torchBuffer.lerp(lightGray, lightGrayAmount);
            skyBuffer.mul(1 - lightGrayAmount);

            for (int torchX = 0; torchX < 16; ++torchX) {
                Vector3f addition = new Vector3f();
                Vector3f torchBuffer = new Vector3f(torchLine[torchX]);


                torchBuffer.mul(1 - lightGrayAmount);
                addition.add(new Vector3f(lightGray).mul(lightGrayAmount));


                //TODO:
                if (darkenWorldAmount > 0.0F) {
                    Vector3f discolored = (new Vector3f(torchBuffer)).mul(0.7F, 0.6F, 0.6F);
                    torchBuffer.lerp(discolored, darkenWorldAmount);
                }

                if (nightVisionScale > 0.0F) {
                    float maxVal = Math.max(torchBuffer.x(), Math.max(torchBuffer.y(), torchBuffer.z()));
                    if (maxVal < 1.0F) {
                        float percentage = 1.0F / maxVal;
                        Vector3f discolored = (new Vector3f(torchBuffer)).mul(percentage);
                        torchBuffer.lerp(discolored, nightVisionScale);
                    }
                }

                //we make botht hse happen in end too
                if (darknessSubtract > 0.0F) {
                    torchBuffer.add(-darknessSubtract, -darknessSubtract, -darknessSubtract);
                }


                Vector3f combined = new Vector3f();

                combined.add(torchBuffer).add(skyBuffer).add(addition);

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

    private float[][] selectTorch(RandomSource randomSource) {
        //simulate torch flicker
        float[][] torchLine = new float[16][];
        for (int i = 0; i < 16; i++) {
            torchLine[i] = ColorManager.unpack(image.pixels()[16 + i][0]);
        }
        return torchLine;
    }

    private static void clampColor(Vector3f color) {
        color.set(Mth.clamp(color.x, 0.0F, 1.0F),
                Mth.clamp(color.y, 0.0F, 1.0F),
                Mth.clamp(color.z, 0.0F, 1.0F));
    }
}
