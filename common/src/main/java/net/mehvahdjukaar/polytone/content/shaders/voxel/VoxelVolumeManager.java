package net.mehvahdjukaar.polytone.content.shaders.voxel;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.mehvahdjukaar.polytone.content.shaders.IShaderModifier;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3i;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

//TODO: this only works for main render pass. add support (via configs) for secondary (far away) passes
public class VoxelVolumeManager extends ContentManager<Void> implements IShaderModifier {

    public static final String SAMPLER = "InVoxelLight";
    public static final String ORIGIN = "PolyVoxelVolumeOrigin";
    public static final String ORIGIN_TEXEL = "PolyVoxelVolumeOriginTexel";
    //size in blocks of the cube
    public static final String SIZE = "PolyVoxelVolumeSize";
    public static final String HEIGHT = "PolyVoxelVolumeHeight";
    public static final String CELLS_SAMPLER = "InVoxelCells";
    public static final String PALETTE_SAMPLER = "InVoxelPalette";
    public static final String FLAG_PREFIX = "PolyVoxelFlag_";
    public static final String FAR_SAMPLER = "InFarLight";
    public static final String FAR_ORIGIN = "PolyFarLightOrigin";
    public static final String FAR_ORIGIN_TEXEL = "PolyFarLightOriginTexel";
    public static final String FAR_SIZE = "PolyFarLightSize";

    private static final List<String> UNIFORM_NAMES = List.of(ORIGIN, ORIGIN_TEXEL, SIZE, HEIGHT);
    //vanilla only binds up to 12 unit so these 2 arbitrary should probably be fine
    private static final int LIGHT_UNIT = 30;
    private static final int CELLS_UNIT = 31;
    private static final int FAR_LIGHT_UNIT = 29;

    private final Map<BlockEntityType<?>, VoxelDataProvider<?>> blockEntityData = new ConcurrentHashMap<>();
    private final CellPalette palette = new CellPalette(blockEntityData);
    private final NearLightSpreadComputeShader nearLightSpread = new NearLightSpreadComputeShader();
    private final FarLightSpreadComputeShader farLightSpread = new FarLightSpreadComputeShader();
    //volatile so its in sync with its palette
    @Nullable
    private volatile VoxelVolume volume = null;

    private final ConcurrentLinkedQueue<BlockChange> changedBlocks = new ConcurrentLinkedQueue<>();
    private boolean boundThisFrame = false;
    private Boolean gpuSupported = null;

    public VoxelVolumeManager() {
        super("Voxel Volume");
    }

    @Override
    protected void applyWithLevel(RegistryAccess access, boolean isLogIn) {
        closeVolume();
        nearLightSpread.close();
        farLightSpread.close();
        //set by shaders from before the reload, the new ones might not want the volume at all
        boundThisFrame = false;
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        closeVolume();
        if (logOff) palette.close();
        nearLightSpread.close();
        farLightSpread.close();
        boundThisFrame = false;
    }

    // TODO: moving pistons when we add per entity off grid lights. pushed glowstone goes dark for 2 ticks rn
    //mods call this during client setup
    public <T extends BlockEntity> void registerBlockEntityData(BlockEntityType<T> type, VoxelDataProvider<T> data) {
        blockEntityData.put(type, data);
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
        boolean isDynamic = palette.hasBlockEntityData() && (oldState.hasBlockEntity() || newState.hasBlockEntity());
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

    //a 3d sampler left on unit 0 next to a 2d one fails the whole draw, so any shader that samples these gets bound
    @Override
    public boolean isEnabledFor(IShader shader) {
        return IShaderModifier.super.isEnabledFor(shader) || GLHelper.usesUniform(shader.programId(), SAMPLER)
                || GLHelper.usesUniform(shader.programId(), CELLS_SAMPLER) || GLHelper.usesUniform(shader.programId(), FAR_SAMPLER);
    }

    @Override
    public void bindTo(IShader shader) {
        boolean irisOn = CompatHandler.irisShaderPackActive();
        if (!irisOn) boundThisFrame = true;
        GLHelper.setProgramUniform(shader.programId(), SAMPLER, LIGHT_UNIT);
        GLHelper.setProgramUniform(shader.programId(), CELLS_SAMPLER, CELLS_UNIT);
        GLHelper.setProgramUniform(shader.programId(), FAR_SAMPLER, FAR_LIGHT_UNIT);
        //GUI items and the hand use the same core shaders but aren't in world space, size 0 gives them vanilla light
        boolean insideLevelRenderer = LevelRenderPassTracker.isInLevelPass() || Polytone.POST_CHAINS.isProcessing();
        VoxelVolume v = insideLevelRenderer && !irisOn ? volumeForCurrentPass() : null;
        if (v == null) {
            shader.setSampler(PALETTE_SAMPLER, 0);
            bindPlacement(shader, 0f, 0f, 0f, 0, 0, 0, 0, 0);
            bindFarLight(shader.programId(), null, Vec3.ZERO);
            return;
        }
        PaletteGrid grid = v.grid();
        BlockPos origin = grid.origin();
        Vector3i originTexel = grid.originTexel();
        Vec3 cameraPos = LevelRenderPassTracker.currentCamera().getPosition();

        GLHelper.bindTextureUnit(LIGHT_UNIT, v.nearLight().lightTextureId());
        GLHelper.bindTextureUnit(CELLS_UNIT, grid.textureId());
        shader.setSampler(PALETTE_SAMPLER, palette.textureId());
        bindPlacement(shader,
                (float) (origin.getX() - cameraPos.x),
                (float) (origin.getY() - cameraPos.y),
                (float) (origin.getZ() - cameraPos.z),
                originTexel.x, originTexel.y, originTexel.z, grid.widthInBlocks(), grid.heightInBlocks());
        bindFarLight(shader.programId(), v.farLight(), cameraPos);

        for (var e : palette.flagBitByName().entrySet()) {
            String uniform = FLAG_PREFIX + e.getKey();
            if (shader.hasUniform(uniform)) {
                shader.getUniform(uniform).set(e.getValue() + 1);
            }
        }
    }

    private static void bindPlacement(IShader shader, float originX, float originY, float originZ,
                                      int texelX, int texelY, int texelZ, int width, int height) {
        shader.getUniform(ORIGIN).set(originX, originY, originZ);
        shader.getUniform(ORIGIN_TEXEL).set(texelX, texelY, texelZ);
        shader.getUniform(SIZE).set(width);
        shader.getUniform(HEIGHT).set(height);
        //lightmap patched vanilla shaders dont have these in their json
        int program = shader.programId();
        GLHelper.setProgramUniform(program, ORIGIN, originX, originY, originZ);
        GLHelper.setProgramUniform(program, ORIGIN_TEXEL, texelX, texelY, texelZ);
        GLHelper.setProgramUniform(program, SIZE, width);
        GLHelper.setProgramUniform(program, HEIGHT, height);
    }

    private static void bindFarLight(int program, @Nullable FarLightGrid far, Vec3 cameraPos) {
        if (far == null) {
            GLHelper.setProgramUniform(program, FAR_SIZE, 0, 0, 0);
            return;
        }
        GLHelper.bindTextureUnit(FAR_LIGHT_UNIT, far.lightTextureId());
        BlockPos origin = far.origin();
        Vector3i originTexel = far.originTexel();
        Vector3i size = far.sizeInCells();
        GLHelper.setProgramUniform(program, FAR_ORIGIN, (float) (origin.getX() - cameraPos.x),
                (float) (origin.getY() - cameraPos.y), (float) (origin.getZ() - cameraPos.z));
        GLHelper.setProgramUniform(program, FAR_ORIGIN_TEXEL, originTexel.x, originTexel.y, originTexel.z);
        GLHelper.setProgramUniform(program, FAR_SIZE, size.x, size.y, size.z);
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
        volume.recenterAndRefill(level, camera.getBlockPosition());
        consumeBlockChanges(level);
        volume.spreadLight(nearLightSpread, farLightSpread.ensureLoaded(mc.getResourceManager()) ? farLightSpread : null);
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
}
