package net.mehvahdjukaar.polytone.compat.platform;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.fabric.event.FabricVeilAddShaderPreProcessorsEvent;
import net.fabricmc.fabric.api.event.Event;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;

public class VeilCompatImpl {

    public static void addShaderPreProcessorLast(ShaderPreProcessor processor) {
        ResourceLocation late = Polytone.res("late");
        FabricVeilAddShaderPreProcessorsEvent.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, late);
        FabricVeilAddShaderPreProcessorsEvent.EVENT.register(late, (provider, registry) -> registry.addPreprocessor(processor, false));
    }
}