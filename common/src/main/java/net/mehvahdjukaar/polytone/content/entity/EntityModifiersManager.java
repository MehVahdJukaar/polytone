package net.mehvahdjukaar.polytone.content.entity;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.common.reloader.ContentManager;
import net.mehvahdjukaar.polytone.common.struc.AssetsFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityModifiersManager extends ContentManager<EntityModifier> {

    private final Map<EntityType<?>, EntityModifier> emittersPerEntity = new HashMap<>();

    private final Int2ObjectOpenHashMap<List<ParticleSpawnRecord>> spawnRecords = new Int2ObjectOpenHashMap<>();

    public EntityModifiersManager() {
        super(Spec.of("Entity modifier", () -> EntityModifier.CODEC)
                .wikiPage("Entity-Modifiers")
                .folders("entity_modifiers"));
    }

    @Override
    protected void parseWithLevel(AssetsFiles resources, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        Map<Identifier, JsonElement> jsons = resources.jsons();
        for (var j : parseEnabledJsons(jsons, ops)) {
            if (j != null) {
                addModifier(j.getKey(), j.getValue());
            }
        }
    }

    private void addModifier(Identifier fileId, EntityModifier mod) {
        for (var h : mod.targets().compute(fileId, BuiltInRegistries.ENTITY_TYPE)) {
            emittersPerEntity.merge(h.value(), mod, EntityModifier::merge);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        emittersPerEntity.clear();
    }

    //client thread
    public void onTick(Level level) {

        //hack for local player
        //TODO: add hack for hand with item?
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused()) return;
        if (mc.options.getCameraType().isFirstPerson()) {
            LocalPlayer player = mc.player;
            if (player != null) {
                EntityModifier mod = emittersPerEntity.get(player.getType());
                if (mod != null) {
                    if (spawnRecords.containsKey(player.getId())) return;
                    Vec3 cameraPos = mc.gameRenderer.mainCamera().position();
                    var particleSpawns = mod.gatherParticleSpawnsWithoutModel(player, cameraPos);
                    spawnRecords.put(player.getId(), particleSpawns);
                }
            }
        }

        for (var entry : spawnRecords.int2ObjectEntrySet()) {
            Entity entity = level.getEntity(entry.getIntKey());
            if (entity != null) {

                var value = entry.getValue();
                for (ParticleSpawnRecord record : value) {
                    record.emitter().tick(entity, record.matrix());
                }
            }
        }
        spawnRecords.clear();


    }

    private WeakReference<EntityRenderer> lastLivingEntityState = new WeakReference<>(null);
    private WeakReference<CameraRenderState> lastCameraState = new WeakReference<>(null);

    public void captureRenderStates(CameraRenderState state, EntityRenderer<?, ?> renderer) {
        lastCameraState = new WeakReference<>(state);
        lastLivingEntityState = new WeakReference<>(renderer);
    }

    //render thread
    public <S extends EntityRenderState> void onEntityRender(
            Model<? super S> model, PoseStack poseStack, S renderState) {
        CameraRenderState cameraState = lastCameraState.get();
        if (cameraState == null) return;
        EntityModifier mod = emittersPerEntity.get(renderState.entityType);

        if (mod != null) {
            int id = ((IRenderStateWithId) renderState).polytone$getId();
            if (spawnRecords.containsKey(id)) return;

            List<ParticleSpawnRecord> particleSpawns = mod.gatherParticleSpawns(model, poseStack, renderState,
                    lastLivingEntityState.get(), cameraState.pos);

            if (!particleSpawns.isEmpty()) {
                spawnRecords.put(id, particleSpawns);
            }
        }
    }

    public void onEntityTick(Entity entity) {
    }
}
