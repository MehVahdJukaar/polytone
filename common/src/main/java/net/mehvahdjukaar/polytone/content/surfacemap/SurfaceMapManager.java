package net.mehvahdjukaar.polytone.content.surfacemap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.reloader.SingleFileContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneBuiltInUniformsSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// registered before POST_CHAINS so the sampler names exist when programs link
public class SurfaceMapManager extends SingleFileContentManager<SurfaceMapSettings> {

    private final SurfaceMap map = new SurfaceMap();

    private volatile List<String> samplerNames = List.of();
    private final Set<String> declaredSamplers = new HashSet<>();
    private SurfaceMapSettings parsedSettings = SurfaceMapSettings.NONE;
    private GpuBuffer emptyPalette = null;
    private DynamicTexture emptyLayer = null;

    public SurfaceMapManager() {
        super("Surface Map", "surface_map.properties", "surface_map.json", Polytone.MOD_ID);
    }

    public SurfaceMap map() {
        return map;
    }

    @Override
    protected AssetsFiles prepare(PreparableReloadListener.SharedState sharedState) {
        AssetsFiles resources = super.prepare(sharedState);
        // needed before programs compile, parsing only happens later with the level
        List<String> names = new ArrayList<>();
        for (JsonElement e : resources.jsons().values()) {
            if (!(e instanceof JsonObject obj)) continue;
            if (obj.get("heights") instanceof JsonObject h) {
                for (String name : h.keySet()) {
                    if (!name.isEmpty() && !names.contains(name)) names.add(name);
                }
            }
            if (obj.has("biome")) {
                if (!names.contains(SurfaceMap.BIOME_SAMPLER)) names.add(SurfaceMap.BIOME_SAMPLER);
                PolytoneBuiltInUniformsSet.register(SurfaceBiomePalette.UBO_NAME);
            }
        }
        samplerNames = List.copyOf(names);
        return resources;
    }

    public List<String> samplerNames() {
        return samplerNames;
    }

    public void onSamplerDeclared(String name) {
        if (samplerNames.contains(name)) declaredSamplers.add(name);
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        // merged, not last wins: every pack shares one map
        SurfaceMapSettings result = SurfaceMapSettings.NONE;
        for (var entry : resources.jsons().entrySet()) {
            try {
                result = result.mergedWith(SurfaceMapSettings.CODEC.parse(ops, entry.getValue()).getOrThrow(),
                        entry.getKey());
            } catch (Exception e) {
                Polytone.LOGGER.error("Failed to parse surface_map.json in file {}", entry.getKey(), e);
            }
        }
        this.parsedSettings = result;
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        map.setSettings(parsedSettings);
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        this.parsedSettings = SurfaceMapSettings.NONE;
        map.setSettings(SurfaceMapSettings.NONE);
    }

    // LevelRenderer.render HEAD, no render pass open
    public void update(ClientLevel level, Vec3 camPos, float partialTick) {
        if (map.isEmpty() || declaredSamplers.isEmpty()) return;
        int renderDistance = Minecraft.getInstance().options.renderDistance().get();
        map.update(level, camPos, partialTick, renderDistance, List.copyOf(declaredSamplers));
    }

    public void markChunkDirty(int chunkX, int chunkZ) {
        map.markChunkDirty(chunkX, chunkZ);
    }

    public void markColumnDirty(BlockPos pos) {
        map.markColumnDirty(pos);
    }

    public boolean hasDeclaredSamplers() {
        return !declaredSamplers.isEmpty();
    }

    // unallocated layers get an empty stand in
    public void bindSamplers(RenderPass pass, Set<String> declaredUniforms) {
        if (samplerNames.isEmpty()) return;
        for (String name : samplerNames) {
            if (!declaredUniforms.contains(name)) continue;
            GpuTextureView texture = map.texture(name);
            if (texture == null) texture = emptyLayer();
            pass.bindTexture(name, texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        }
    }

    // a declared block must get a buffer, and the palette is still null on the first frame
    public void bindUniformBlocks(RenderPass pass, Set<String> declaredUniforms) {
        if (!declaredUniforms.contains(SurfaceBiomePalette.UBO_NAME)) return;
        GpuBufferSlice palette = map.paletteSlice();
        pass.setUniform(SurfaceBiomePalette.UBO_NAME, palette != null ? palette : emptyPalette());
    }

    // zeros, shaders see no slots in use
    private GpuBufferSlice emptyPalette() {
        if (emptyPalette == null) {
            emptyPalette = RenderSystem.getDevice().createBuffer(() -> "Polytone empty surface biome palette",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, SurfaceBiomePalette.UBO_SIZE);
            try (MemoryStack stack = MemoryStack.stackPush()) {
                RenderSystem.getDevice().createCommandEncoder()
                        .writeToBuffer(emptyPalette.slice(), stack.calloc(SurfaceBiomePalette.UBO_SIZE));
            }
        }
        return emptyPalette.slice();
    }

    // zeros, not the missing texture: that one is opaque and would pass the alpha test as data
    private GpuTextureView emptyLayer() {
        if (emptyLayer == null) {
            emptyLayer = new DynamicTexture(() -> "Polytone empty surface map layer", 1, 1, true);
            emptyLayer.upload();
        }
        return emptyLayer.getTextureView();
    }

    public void onClose() {
        map.close();
        if (emptyPalette != null) {
            emptyPalette.close();
            emptyPalette = null;
        }
        if (emptyLayer != null) {
            emptyLayer.close();
            emptyLayer = null;
        }
    }

    public Map<String, SurfaceMapSettings.HeightLayer> heightLayers() {
        return parsedSettings.heights();
    }
}
