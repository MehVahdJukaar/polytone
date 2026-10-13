package net.mehvahdjukaar.polytone.content.surfacemap;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.attributes.IExtendedAttrInterpolator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;

import it.unimi.dsi.fastutil.ints.IntSet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// std140 PolySurfaceBiome: ivec4 SurfaceBiomeWindow (xy min block, z blocks per texel, w texels per side),
// ivec4 SurfaceBiomeInfo (x attributes per slot, y highest slot, z climate present),
// vec4 SurfaceBiomePalette[MAX_SLOTS * MAX_ATTRIBUTES]. each slot's last entry holds the climate
public class SurfaceBiomePalette implements AutoCloseable {

    public static final String UBO_NAME = "PolySurfaceBiome";
    public static final int MAX_SLOTS = 64;   // slot 0 means "not filled yet", 255 means "ran out"
    public static final int OVERFLOW_SLOT = 255;

    private static final int PALETTE_ENTRIES = MAX_SLOTS * SurfaceMapSettings.BiomeLayer.MAX_ATTRIBUTES;

    public static final int UBO_SIZE = paletteSize();

    private static int paletteSize() {
        Std140SizeCalculator size = new Std140SizeCalculator().putIVec4().putIVec4();
        for (int i = 0; i < PALETTE_ENTRIES; i++) size.putVec4();
        return size.get();
    }

    // render thread only
    private final Map<Biome, Integer> slots = new IdentityHashMap<>();
    // indexed by slot with holes, the numbers are already written in the texels
    private final List<Holder<Biome>> bySlot = new ArrayList<>(Collections.nCopies(MAX_SLOTS, null));
    private int highestSlot = 0;
    private boolean warnedOverflow = false;
    private GpuBuffer buffer = null;

    // per slot and attribute, last tick and this tick. frames lerp between them like vanilla's ValueProbe
    private final Object[][] lastValues = new Object[MAX_SLOTS][];
    private final Object[][] newValues = new Object[MAX_SLOTS][];
    private long lastTick = Long.MIN_VALUE;
    // only re-uploaded when something changed
    private boolean uploadPending = true;
    private boolean wasTransitioning = false;
    private int uploadedMinX, uploadedMinZ, uploadedTexels = -1;

    public boolean isFull() {
        return slots.size() + 1 >= MAX_SLOTS;
    }

    public int slotFor(Holder<Biome> biome) {
        Integer existing = slots.get(biome.value());
        if (existing != null) return existing;
        for (int slot = 1; slot < MAX_SLOTS; slot++) {   // slot 0 stays "not filled yet"
            if (bySlot.get(slot) != null) continue;
            bySlot.set(slot, biome);
            slots.put(biome.value(), slot);
            highestSlot = Math.max(highestSlot, slot);
            forget(slot);   // evaluated at the next update, not at the next tick
            return slot;
        }
        if (!warnedOverflow) {
            warnedOverflow = true;
            Polytone.LOGGER.warn("Surface map: more than {} biomes are in the window at once, so cells beyond "
                    + "that read as overflow and consumers fall back to the camera. Reduce the biome layer's "
                    + "coverage if this dimension really has this many.", MAX_SLOTS - 1);
        }
        return OVERFLOW_SLOT;
    }

    // frees slots no texel uses anymore, otherwise they fill up and every new cell overflows
    public void retainOnly(IntSet used) {
        int highest = 0;
        for (int slot = 1; slot < MAX_SLOTS; slot++) {
            Holder<Biome> biome = bySlot.get(slot);
            if (biome == null) continue;
            if (used.contains(slot)) {
                highest = slot;
                continue;
            }
            bySlot.set(slot, null);
            slots.remove(biome.value());
            forget(slot);
        }
        highestSlot = highest;
    }

    public void clear() {
        slots.clear();
        Collections.fill(bySlot, null);
        Arrays.fill(lastValues, null);
        Arrays.fill(newValues, null);
        lastTick = Long.MIN_VALUE;
        uploadPending = true;
        highestSlot = 0;
        warnedOverflow = false;
    }

    private void forget(int slot) {
        lastValues[slot] = null;
        newValues[slot] = null;
        uploadPending = true;
    }

    // evaluated per tick through the whole layer stack so biome_modifiers apply, frames lerp between ticks
    public void update(ClientLevel level, Vec3 camPos, List<EnvironmentAttribute<?>> attributes,
                       int windowMinX, int windowMinZ, int texelSize, int texels, float partialTick) {
        int attrCount = attributes.size();
        int maxAttr = SurfaceMapSettings.BiomeLayer.MAX_ATTRIBUTES;
        boolean climate = attrCount < maxAttr;   // the last entry is free: carry the climate there

        long tick = level.getGameTime();
        boolean ticked = tick != lastTick;
        lastTick = tick;
        boolean transitioning = false;
        for (int slot = 1; slot < MAX_SLOTS; slot++) {
            Holder<Biome> biome = bySlot.get(slot);
            if (biome == null) continue;
            Object[] current = newValues[slot];
            if (current == null || current.length != attrCount) {
                current = evaluateAll(level, camPos, attributes, biome);
                newValues[slot] = current;
                lastValues[slot] = current;   // appear AT the value, not fade in from zero
                uploadPending = true;
            } else if (ticked) {
                lastValues[slot] = current;
                newValues[slot] = evaluateAll(level, camPos, attributes, biome);
            }
            if (!Arrays.equals(lastValues[slot], newValues[slot])) transitioning = true;
        }
        if (windowMinX != uploadedMinX || windowMinZ != uploadedMinZ || texels != uploadedTexels) {
            uploadPending = true;
        }
        // one more upload after a transition so it lands on the settled value
        boolean upload = buffer == null || uploadPending || transitioning || wasTransitioning;
        wasTransitioning = transitioning;
        if (!upload) return;
        uploadPending = false;
        uploadedMinX = windowMinX;
        uploadedMinZ = windowMinZ;
        uploadedTexels = texels;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder builder = Std140Builder.onStack(stack, UBO_SIZE)
                    .putIVec4(windowMinX, windowMinZ, texelSize, texels)
                    // y is the highest live slot, not a count, retainOnly leaves holes
                    .putIVec4(attrCount, highestSlot, climate ? 1 : 0, 0);
            // slot 0 is written as zeros too so the index stays slot * MAX_ATTRIBUTES
            for (int slot = 0; slot < MAX_SLOTS; slot++) {
                Holder<Biome> biome = bySlot.get(slot);
                for (int i = 0; i < maxAttr; i++) {
                    if (biome == null) {
                        builder.putVec4(0, 0, 0, 0);
                    } else if (i < attrCount) {
                        putValue(builder, lerp(attributes.get(i), partialTick, lastValues[slot][i], newValues[slot][i]));
                    } else if (climate && i == maxAttr - 1) {
                        putClimate(builder, biome.value());
                    } else {
                        builder.putVec4(0, 0, 0, 0);
                    }
                }
            }
            ByteBuffer bb = builder.get();
            if (buffer == null) {
                buffer = RenderSystem.getDevice().createBuffer(() -> "Polytone surface biome palette",
                        GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);
            }
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), bb);
        }
    }

    private static Object[] evaluateAll(ClientLevel level, Vec3 camPos, List<EnvironmentAttribute<?>> attributes,
                                        Holder<Biome> biome) {
        Object[] values = new Object[attributes.size()];
        for (int i = 0; i < values.length; i++) values[i] = evaluate(level, camPos, attributes.get(i), biome);
        return values;
    }

    // the type's own interpolation, like the camera probe
    @SuppressWarnings("unchecked")
    private static <Value> Object lerp(EnvironmentAttribute<Value> attribute, float partialTick, Object from, Object to) {
        if (from == null || to == null || from.equals(to)) return to;
        return attribute.type().partialTickLerp().apply(partialTick, (Value) from, (Value) to);
    }

    private static <Value> Object evaluate(ClientLevel level, Vec3 camPos,
                                           EnvironmentAttribute<Value> attribute, Holder<Biome> biome) {
        SpatialAttributeInterpolator interpolator = new SpatialAttributeInterpolator();
        interpolator.accumulate(1.0, biome.value().getAttributes());
        // same post layer the camera probe feeds, so biome_modifiers apply here too
        SpatialAttributeInterpolator post = ((IExtendedAttrInterpolator) interpolator)
                .polytone$getOrCreatePostInterpolator();
        if (post != null) post.accumulate(1.0, Polytone.BIOME_MODIFIERS.getPostAttributes(biome.value()));
        return level.environmentAttributes().getValue(attribute, camPos, interpolator);
    }

    // base temperature, downfall, precipitation. biome fields, not attributes
    private static void putClimate(Std140Builder builder, Biome biome) {
        Biome.ClimateSettings c = ColorUtils.getClimateSettings(biome);
        builder.putVec4(c.temperature(), c.downfall(), c.hasPrecipitation() ? 1 : 0, 1);
    }

    private static void putValue(Std140Builder builder, Object value) {
        switch (value) {
            case Float f -> builder.putVec4(f, 0, 0, 0);
            case Integer color -> builder.putVec4(
                    ((color >> 16) & 0xFF) / 255f,
                    ((color >> 8) & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    ((color >>> 24) & 0xFF) / 255f);
            case Boolean b -> builder.putVec4(b ? 1 : 0, 0, 0, 0);
            case null, default -> builder.putVec4(0, 0, 0, 0);
        }
    }

    // null until the first update
    public GpuBufferSlice slice() {
        return buffer == null ? null : buffer.slice();
    }

    public static boolean isSupported(EnvironmentAttribute<?> attribute) {
        var type = attribute.type();
        return type == AttributeTypes.FLOAT || type == AttributeTypes.ANGLE_DEGREES
                || type == AttributeTypes.RGB_COLOR || type == AttributeTypes.ARGB_COLOR
                || type == AttributeTypes.BOOLEAN;
    }

    @Override
    public void close() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
        clear();
    }
}
