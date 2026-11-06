package net.mehvahdjukaar.polytone.compat.platform;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.forge.event.ForgeVeilAddShaderProcessorsEvent;
import net.mehvahdjukaar.polytone.Polytone;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;

public class VeilCompatImpl {

    public static void addShaderPreProcessorLast(ShaderPreProcessor processor) {
        var modBus = ModList.get().getModContainerById(Polytone.MOD_ID).orElseThrow().getEventBus();
        modBus.addListener(EventPriority.LOWEST, ForgeVeilAddShaderProcessorsEvent.class,
                event -> event.addPreprocessor(processor, false));
    }

}
