package net.mehvahdjukaar.polytone.content.entity;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityModifiersManager extends JsonPartialReloader {

    private final Map<EntityType<?>, EntityModifier> emittersPerEntity = new HashMap<>();

    private final Int2ObjectOpenHashMap<List<ParticleSpawnRecord>> spawnRecords = new Int2ObjectOpenHashMap<>();

    public EntityModifiersManager(){
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

        for (var entry : spawnRecords.int2ObjectEntrySet()) {
            Entity entity = level.getEntity(entry.getIntKey());
            if (entity != null) {

                var value = entry.getValue();
                for ( ParticleSpawnRecord record : value) {
                    record.emitter().tick(entity, record.transform());
                }
            }
        }
        spawnRecords.clear();
    }


    //render thread
    public <S extends LivingEntityRenderState> void onEntityRender(
            LivingEntityRenderer<?, S, ?> renderer, PoseStack poseStack, S renderState, CameraRenderState cameraState) {
        EntityModifier mod = emittersPerEntity.get(renderState.entityType);

        if (mod != null) {
            int id = ((IRenderStateWithId) renderState).polytone$getId();
            if (spawnRecords.containsKey(id)) return;

            var particleSpawns = mod.gatherParticleSpawns(renderer, poseStack, renderState, cameraState);

            if(!particleSpawns.isEmpty())
                spawnRecords.put(id, particleSpawns);
        }
    }

    public void onEntityTick(Entity entity) {
    }
}
