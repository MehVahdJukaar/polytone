package net.mehvahdjukaar.polytone.content.entity;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.polytone.common.Parsed;
import net.mehvahdjukaar.polytone.common.reloader.JsonPartialReloader;
import net.mehvahdjukaar.polytone.content.dimension.DimensionEffectsModifier;
import net.mehvahdjukaar.polytone.content.global_expressions.GlobalExpression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class EntityModifiersManager extends JsonPartialReloader {


    private final Map<EntityRenderer<?,?>, EntityParticleEmitter> emittersPerRenderer = new HashMap<>();
    private final Map<EntityType<?>, EntityParticleEmitter> emittersPerEntity = new HashMap<>();

    @Override
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        for (var j : Parsed.batchParseOnlyEnabled(jsons, EntityParticleEmitter.CODEC,
                ops, "Entity Modifiers")) {
            if (j != null) {
                emitters.put(j.getKey(), j.getValue());
            }
        }
    }

    private void addModifier(Identifier fileId, DimensionEffectsModifier mod, HolderLookup.Provider registryAccess) {
        for (var h : mod.targets().compute(fileId, registryAccess)) {
            emittersPerEntity.merge(h.unwrapKey().get().identifier(), mod, EntityParticleEmitter::merge);
        }
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        for(var entry : emitters.entrySet()){
            entry.tar
        }
        dispatcher.getRenderer()
    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        emittersPerRenderer.clear();
    }

    public void onTick(Level level) {

    }


    public <S extends LivingEntityRenderState> void onEntityRender(
            LivingEntityRenderer<?, S, ?> renderer, PoseStack poseStack, S renderState) {

    }
}
