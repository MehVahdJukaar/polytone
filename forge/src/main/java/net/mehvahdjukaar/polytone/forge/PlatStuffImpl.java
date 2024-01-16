package net.mehvahdjukaar.polytone.forge;

import cpw.mods.modlauncher.api.INameMappingService;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.fml.ModLoader;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PlatStuffImpl {

    public static boolean isModStateValid() {
        return ModLoader.isLoadingStateValid();
    }

    public static void addClientReloadListener(Supplier<PreparableReloadListener> listener, ResourceLocation location) {
        Consumer<RegisterClientReloadListenersEvent> eventConsumer = (event) -> {
            event.registerReloadListener(listener.get());
        };
        FMLJavaModLoadingContext.get().getModEventBus().addListener(eventConsumer);
    }

    private static final Field f;

    static {
        try {
            f = BlockColors.class.getDeclaredField("f_92571_");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static BlockColor getBlockColor(BlockColors colors, Block block) {
        try {
            return ( colors).f_92571_.get(block.delegate);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SoundEvent registerSoundEvent(ResourceLocation id) {
        SoundEvent variableRangeEvent = new SoundEvent(id);
        ForgeRegistry<SoundEvent> reg = (ForgeRegistry<SoundEvent>) ForgeRegistries.SOUND_EVENTS;
        boolean wasLocked = reg.isLocked();
        if (wasLocked) reg.unfreeze();
        variableRangeEvent.setRegistryName(id);
        ForgeRegistries.SOUND_EVENTS.register(variableRangeEvent);
        if (wasLocked) reg.freeze();
        return variableRangeEvent;
    }

    public static String maybeRemapName(String s) {
        return ObfuscationReflectionHelper.remapName(INameMappingService.Domain.CLASS, s);
    }
}
