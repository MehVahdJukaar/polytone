package net.mehvahdjukaar.polytone.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.TreeMap;

public class DayTimeTextureTicker implements SpriteTicker {

    private final SpriteContents.AnimatedTexture animationInfo;
    @Nullable
    private final InterpolationData interpolationData;
    private final float animationScaleFactor;
    private final TreeMap<Float, Integer> frameMap = new TreeMap<>();
    private final int dayDuration;
    private final DayTimeTexture.Mode mode;

    private int lastFrameIndex = 0;

    public DayTimeTextureTicker(SpriteContents.AnimatedTexture animationInfo,
                                SpriteContents spriteContents, boolean interpolateFrames,
                                int dayDuration, DayTimeTexture.Mode mode) {
        this.animationInfo = animationInfo;
        this.dayDuration = dayDuration;
        this.mode = mode;
        if (interpolateFrames) {
            this.interpolationData = new InterpolationData(spriteContents);
        } else {
            this.interpolationData = null;
        }
        int totalDuration = 0;
        for (SpriteContents.FrameInfo frameInfo : animationInfo.frames) {
            totalDuration += frameInfo.time;
        }
        this.animationScaleFactor = 1f / totalDuration;

        // Populate TreeMap with cumulative durations
        float accumulatedTime = 0.0F;
        for (int i = 0; i < animationInfo.frames.size(); i++) {
            SpriteContents.FrameInfo frameInfo = animationInfo.frames.get(i);
            float scaledDuration = frameInfo.time * this.animationScaleFactor;
            frameMap.put(accumulatedTime, i);
            accumulatedTime += scaledDuration;
        }
    }

    @Override
    public void tickAndUpload(int x, int y) {
        Float delta = getDelta();
        if (delta == null) return;
        // Calculate the current frame based on the day cycle
        var currentFrame = frameMap.floorEntry(delta);


        Integer frameOrdinal = currentFrame.getValue();
        var frames = this.animationInfo.frames;
        SpriteContents.FrameInfo frameInfo = frames.get(frameOrdinal);

        if (frameInfo.index != lastFrameIndex) {
            this.animationInfo.uploadFrame(x, y, frameInfo.index);
        }
        if (this.interpolationData != null) {
            var nextFrameInfo = frames.get((frameOrdinal + 1) % frames.size());
            float floorKey = currentFrame.getKey();
            float frameDelta = (delta - floorKey) / animationScaleFactor;

            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(() -> this.interpolationData.uploadInterpolatedFrame(x, y,
                        frameInfo, nextFrameInfo, frameDelta, animationInfo));
            } else {
                this.interpolationData.uploadInterpolatedFrame(x, y,
                        frameInfo, nextFrameInfo, frameDelta, animationInfo);
            }
        }
        lastFrameIndex = frameInfo.index;
    }

    private @Nullable Float getDelta() {
        Level level = Minecraft.getInstance().level;
        if (level == null) return null;

        if (mode == DayTimeTexture.Mode.WEATHER) {
            float partialTicks = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
            float max = level.isThundering() ? 2 / 3f : 1 / 3f;
            return level.getRainLevel(partialTicks) * max + 1 / 6; //needs to fall in between those 2 so we dont get interpolation as this stuff doesnt loop back
        }

        long dayTime = level.dayTime() % dayDuration;
        return dayTime / (float) dayDuration;
    }

    @Override
    public void close() {
        if (this.interpolationData != null) {
            this.interpolationData.close();
        }
    }

    //need to copy because its an inner non static class
    public static final class InterpolationData implements AutoCloseable {
        private final NativeImage[] activeFrame;
        private final SpriteContents spriteContents;

        InterpolationData(SpriteContents spriteContents) {
            this.spriteContents = spriteContents;
            this.activeFrame = new NativeImage[spriteContents.byMipLevel.length];

            for (int i = 0; i < this.activeFrame.length; ++i) {
                int j = spriteContents.width() >> i;
                int k = spriteContents.height() >> i;
                this.activeFrame[i] = new NativeImage(j, k, false);
            }

        }

        void uploadInterpolatedFrame(int x, int y, SpriteContents.FrameInfo currentFrame,
                                     SpriteContents.FrameInfo nextFrame, float frameDelta,
                                     SpriteContents.AnimatedTexture animatedTexture) {
            double time = currentFrame.time;
            double delta = 1.0 - (double) frameDelta / time;
            int currentFrameIndex = currentFrame.index;
            int nextFrameIndex = nextFrame.index;
            if (currentFrameIndex != nextFrameIndex) {
                for (int k = 0; k < this.activeFrame.length; ++k) {
                    int l = spriteContents.width() >> k;
                    int m = spriteContents.height() >> k;

                    for (int n = 0; n < m; ++n) {
                        for (int o = 0; o < l; ++o) {
                            int p = this.getPixel(animatedTexture, currentFrameIndex, k, o, n);
                            int q = this.getPixel(animatedTexture, nextFrameIndex, k, o, n);
                            int r = this.mix(delta, p >> 16 & 255, q >> 16 & 255);
                            int s = this.mix(delta, p >> 8 & 255, q >> 8 & 255);
                            int t = this.mix(delta, p & 255, q & 255);
                            this.activeFrame[k].setPixelRGBA(o, n, p & -16777216 | r << 16 | s << 8 | t);
                        }
                    }
                }

                spriteContents.upload(x, y, 0, 0, this.activeFrame);
            }
        }

        private int getPixel(SpriteContents.AnimatedTexture animatedTexture, int frameIndex, int mipLevel, int x, int y) {
            return spriteContents.byMipLevel[mipLevel].getPixelRGBA(x +
                            (animatedTexture.getFrameX(frameIndex) * spriteContents.width() >> mipLevel),
                    y + (animatedTexture.getFrameY(frameIndex) * spriteContents.height() >> mipLevel));
        }

        private int mix(double delta, int color1, int color2) {
            return (int) (delta * (double) color1 + (1.0 - delta) * (double) color2);
        }

        @Override
        public void close() {
            for (NativeImage nativeImage : this.activeFrame) {
                nativeImage.close();
            }
        }
    }

}
