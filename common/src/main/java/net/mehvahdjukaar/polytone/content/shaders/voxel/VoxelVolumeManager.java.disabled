package net.mehvahdjukaar.polytone.content.shaders.voxel;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.api.VoxelDataProvider;
import net.mehvahdjukaar.polytone.api.VoxelVolumeApi;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import com.google.gson.JsonElement;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

//TODO: this only works for main render pass. add support (via configs) for secondary (far away) passes
public class VoxelVolumeManager extends ContentManager<Void> implements IShaderModifier {

    public static final String BLOCK_NAME = "PolyVoxelVolume"; //matches polytone_voxel_volume.glsl
    public static final String SAMPLER = "InVoxelLight";
    public static final String CELLS_SAMPLER = "InVoxelCells";
    public static final String PALETTE_SAMPLER = "InVoxelPalette";
    public static final String FLAG_PREFIX = "PolyVoxelFlag_";
    public static final String FAR_SAMPLER = "InFarLight";

    private static final List<String> UNIFORM_NAMES = List.of(BLOCK_NAME);
    //blaze3d binds 2d only, on units counting up from 0. these are ours, bound by hand
    private static final int PALETTE_UNIT = 28;
    private static final int FAR_LIGHT_UNIT = 29;
    private static final int LIGHT_UNIT = 30;
    private static final int CELLS_UNIT = 31;

    private static final int UBO_SIZE = new Std140SizeCalculator().putIVec3().putIVec4().putInt().putIVec3().putIVec3().putIVec3().get();

    private final Map<BlockEntityType<?>, VoxelDataProvider<?>> blockEntityData = new ConcurrentHashMap<>();
    private final List<RegisterCallback> registerCallbacks = new CopyOnWriteArrayList<>();
    private final Map<Block, List<String>> modBlockFlags = new ConcurrentHashMap<>();
    private final CellPalette palette = new CellPalette(blockEntityData, modBlockFlags);
    private final NearLightSpreadComputeShader nearLightSpread = new NearLightSpreadComputeShader();
    private final FarLightSpreadComputeShader farLightSpread = new FarLightSpreadComputeShader();
    //volatile so its in sync with its palette
    @Nullable
    private volatile VoxelVolume volume = null;

    private final ConcurrentLinkedQueue<BlockChange> changedBlocks = new ConcurrentLinkedQueue<>();
    private boolean boundThisFrame = false;
    private Boolean gpuSupported = null;
    @Nullable
    private GpuBuffer placement = null;
    @Nullable
    private GpuBuffer emptyPlacement = null;
    //program ids get reused so this is cleared on reload
    private final Int2IntMap flagsVersionByProgram = new Int2IntOpenHashMap();

    public VoxelVolumeManager() {
        super("Voxel Volume");
    }

    //after colored lights parsed the packs but before they apply, so mod lights get picked up
    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        blockEntityData.clear();
        modBlockFlags.clear();
        var event = new VoxelVolumeApi.RegisterEvent();
        boolean packsHaveColoredLights = Polytone.COLORED_LIGHTS.hasAnyLights();
        for (var c : registerCallbacks) {
            if (c.activation() == VoxelVolumeApi.Activation.ALWAYS || packsHaveColoredLights) {
                c.callback().accept(event);
            }
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        closeVolume();
        nearLightSpread.close();
        farLightSpread.close();
        //set by shaders from before the reload, the new ones might not want the volume at all
        boundThisFrame = false;
        flagsVersionByProgram.clear();
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        closeVolume();
        if (logOff) palette.close();
        nearLightSpread.close();
        farLightSpread.close();
        boundThisFrame = false;
        flagsVersionByProgram.clear();
    }

    public void close() {
        closeVolume();
        palette.close();
        nearLightSpread.close();
        farLightSpread.close();
        if (placement != null) placement.close();
        if (emptyPlacement != null) emptyPlacement.close();
        placement = null;
        emptyPlacement = null;
    }

    //Vanilla never sets these, an unset sampler3D sits on unit 0 next to a sampler2D and the draw fails
    public static void assignTextureUnits(int program) {
        GLHelper.setProgramUniform(program, SAMPLER, LIGHT_UNIT);
        GLHelper.setProgramUniform(program, CELLS_SAMPLER, CELLS_UNIT);
        GLHelper.setProgramUniform(program, FAR_SAMPLER, FAR_LIGHT_UNIT);
        GLHelper.setProgramUniform(program, PALETTE_SAMPLER, PALETTE_UNIT);
    }

    public <T extends BlockEntity> void addBlockEntityData(BlockEntityType<T> type, VoxelDataProvider<T> data) {
        blockEntityData.put(type, data);
    }

    public void addVoxelFlag(Block block, String flag) {
        modBlockFlags.computeIfAbsent(block, b -> new ArrayList<>()).add(flag);
    }

    public void registerCallback(VoxelVolumeApi.Activation activation, Consumer<VoxelVolumeApi.RegisterEvent> callback) {
        registerCallbacks.add(new RegisterCallback(activation, callback));
    }

    //For block entity data that changed on the client without a block update. any thread
    public void markDirty(BlockPos pos) {
        if (volume == null) return;
        changedBlocks.add(new BlockChange(pos.asLong(), true));
    }

    public void onBlockEntityDataPacket(BlockPos pos, BlockEntityType<?> type) {
        if (blockEntityData.containsKey(type)) markDirty(pos);
    }

    //queued since the upload needs the render thread and any thread can call this
    public void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState) {
        if (volume == null) return;
        boolean isDynamic = palette.shouldFetchBlockEntityOf(oldState) || palette.shouldFetchBlockEntityOf(newState);
        char oldIndex = palette.getBlockStateIndexOf(oldState);
        char newIndex = palette.getBlockStateIndexOf(newState);
        if (!isDynamic && oldIndex == newIndex) return;
        boolean changesLight = isDynamic || !palette.spreadsLightLike(oldIndex, newIndex);
        changedBlocks.add(new BlockChange(pos.asLong(), changesLight));
    }

    @Override
    public List<String> getEnablingUniforms() {
        return UNIFORM_NAMES;
    }

    //raw samplers arent in the program uniforms, ask gl
    @Override
    public boolean isEnabledFor(IShader shader) {
        if (!shader.isGlProgram()) return false;
        return IShaderModifier.super.isEnabledFor(shader) || GLHelper.usesUniform(shader.programId(), SAMPLER)
                || GLHelper.usesUniform(shader.programId(), CELLS_SAMPLER) || GLHelper.usesUniform(shader.programId(), FAR_SAMPLER)
                || GLHelper.usesUniform(shader.programId(), PALETTE_SAMPLER);
    }

    @Override
    public void bindTo(IShader shader) {
        boolean irisOn = CompatHandler.irisShaderPackActive();
        if (!irisOn) boundThisFrame = true;
        //GUI items and the hand use the same core shaders but aren't in world space, size 0 gives them vanilla light
        boolean insideLevelRenderer = LevelRenderPassTracker.isInLevelPass() || Polytone.POST_CHAINS.isProcessing();
        VoxelVolume v = insideLevelRenderer && !irisOn ? volumeForCurrentPass() : null;
        if (v == null || placement == null) {
            shader.setUniform(BLOCK_NAME, emptyPlacement());
            return;
        }
        shader.setUniform(BLOCK_NAME, placement.slice());
        GLHelper.bindTextureUnit(LIGHT_UNIT, v.nearLight().lightTextureId());
        GLHelper.bindTextureUnit(CELLS_UNIT, v.grid().textureId());
        GLHelper.bindTextureUnit(PALETTE_UNIT, palette.textureId());
        if (v.farLight() != null) GLHelper.bindTextureUnit(FAR_LIGHT_UNIT, v.farLight().lightTextureId());
        int program = shader.programId();
        int flagsVersion = palette.flagsVersion();
        if (flagsVersionByProgram.getOrDefault(program, -1) != flagsVersion) {
            flagsVersionByProgram.put(program, flagsVersion);
            for (var e : palette.flagBitByName().entrySet()) {
                GLHelper.setProgramUniform(program, FLAG_PREFIX + e.getKey(), e.getValue() + 1);
            }
        }
    }

    //we are inside a render pass when this is needed so no writeToBuffer, the zeros go in at creation
    private GpuBufferSlice emptyPlacement() {
        if (emptyPlacement == null) {
            ByteBuffer zeros = MemoryUtil.memCalloc(UBO_SIZE);
            emptyPlacement = RenderSystem.getDevice().createBuffer(() -> "Polytone empty voxel volume UBO", GpuBuffer.USAGE_UNIFORM, zeros);
            MemoryUtil.memFree(zeros);
        }
        return emptyPlacement.slice();
    }

    // no render pass open here. world coords so it holds for every camera until the volume moves again
    private void uploadPlacement(VoxelVolume v) {
        if (placement == null) {
            placement = RenderSystem.getDevice().createBuffer(() -> "Polytone voxel volume UBO",
                    GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM, UBO_SIZE);
        }
        PaletteGrid grid = v.grid();
        BlockPos origin = grid.origin();
        Vector3i originTexel = grid.originTexel();
        FarLightGrid far = v.farLight();
        BlockPos farOrigin = far == null ? BlockPos.ZERO : far.origin();
        Vector3i farOriginTexel = far == null ? new Vector3i() : far.originTexel();
        Vector3i farSize = far == null ? new Vector3i() : far.sizeInCells();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, UBO_SIZE)
                    .putIVec3(origin.getX(), origin.getY(), origin.getZ())
                    //glsl packs the int after an ivec3 into its padding, putIVec3 skips that slot
                    .putIVec4(originTexel.x, originTexel.y, originTexel.z, grid.widthInBlocks())
                    .putInt(grid.heightInBlocks())
                    .putIVec3(farOrigin.getX(), farOrigin.getY(), farOrigin.getZ())
                    .putIVec3(farOriginTexel.x, farOriginTexel.y, farOriginTexel.z)
                    .putIVec3(farSize.x, farSize.y, farSize.z)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(placement.slice(), data);
        }
    }

    @Nullable
    private VoxelVolume volumeForCurrentPass() {
        VoxelVolume v = volume;
        if (v == null || !v.isFor(LevelRenderPassTracker.currentLevel())) return null;
        return v;
    }

    //main pass only, LevelRendererMixin filters the rest
    public void updateAfterRenderLevel(Camera camera) {
        //PostPass binds after this runs so post chains get it a frame late
        boolean wanted = boundThisFrame;
        boundThisFrame = false;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        //levels dont match. probably changed dimension. close and rebuild
        if (volume != null && !volume.isFor(level)) {
            closeVolume();
        }

        boolean canRun = wanted && camera.isInitialized() && isSupportedByGpu() &&
                nearLightSpread.ensureLoaded(mc.getResourceManager());
        if (!canRun) {
            //cheap enough to keep the grid current while nobody samples it so it doeesnt pile up
            if (volumeForCurrentPass() != null) {
                consumeBlockChanges(level);
            }
            return;
        }

        if (volume == null) {
            //requested so we create a new one
            palette.rebuild(level);
            int widthInSections = Mth.positiveCeilDiv(Math.round(Polytone.CONFIGS.voxelVolumeWidth.get()), SectionPos.SECTION_SIZE);
            int heightInSections = Mth.positiveCeilDiv(Math.round(Polytone.CONFIGS.voxelVolumeHeight.get()), SectionPos.SECTION_SIZE);
            //post packs dont read it, only the lightmap patch does
            int farWidthInSections = Polytone.CONFIGS.coloredLightsBackend.get().tintsVanillaBlockLight() ?
                    Math.round(Polytone.CONFIGS.farLightRange.get()) / SectionPos.SECTION_SIZE : 0;
            int spreadSteps = Math.round(Polytone.CONFIGS.lightSpreadSteps.get());
            volume = new VoxelVolume(level, palette, widthInSections, heightInSections, farWidthInSections, spreadSteps);
        }
        volume.recenterAndRefill(level, camera.blockPosition());
        consumeBlockChanges(level);
        volume.spreadLight(nearLightSpread, farLightSpread.ensureLoaded(mc.getResourceManager()) ? farLightSpread : null);
        uploadPlacement(volume);
    }

    private boolean isSupportedByGpu() {
        if (gpuSupported == null) {
            gpuSupported = GLHelper.supportsComputeShadersAndDSA();
            if (!gpuSupported) {
                Polytone.LOGGER.warn("A resource pack uses Polytone's voxel volume but this GPU/driver has no compute shaders or direct state access (got {}). It will stay dark",
                        GLHelper.getContextDescription());
            }
        }
        return gpuSupported;
    }

    private void consumeBlockChanges(ClientLevel level) {
        BlockChange change;
        while ((change = changedBlocks.poll()) != null) {
            volume.updateBlock(level, BlockPos.of(change.pos()), change.changesLight());
        }
    }

    private void closeVolume() {
        if (volume == null) return;
        volume.close();
        volume = null;
        changedBlocks.clear();
    }

    private record BlockChange(long pos, boolean changesLight) {
    }

    private record RegisterCallback(VoxelVolumeApi.Activation activation, Consumer<VoxelVolumeApi.RegisterEvent> callback) {
    }
}
