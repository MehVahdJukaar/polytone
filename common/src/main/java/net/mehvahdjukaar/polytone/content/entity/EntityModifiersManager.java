package net.mehvahdjukaar.polytone.content.entity;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
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

public class EntityModifiersManager extends JsonPartialReloader {

    private final Map<EntityType<?>, EntityModifier> emittersPerEntity = new HashMap<>();

    private final Int2ObjectOpenHashMap<List<ParticleSpawnRecord>> spawnRecords = new Int2ObjectOpenHashMap<>();

    public EntityModifiersManager() {
        super("entity_modifiers");
    }

    @Override
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var j : Parsed.batchParseOnlyEnabled(jsons, EntityModifier.CODEC,
                ops, "Entity Modifiers")) {
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
                    Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
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
