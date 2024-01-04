package net.mehvahdjukaar.polytone.moonlight_configs.forge;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.mehvahdjukaar.polytone.moonlight_configs.ConfigSpec;
import net.mehvahdjukaar.polytone.moonlight_configs.ConfigType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

public class ConfigSpecWrapper extends ConfigSpec {

    private final ForgeConfigSpec spec;

    private final ModConfig modConfig;

    public ConfigSpecWrapper(ResourceLocation name, ForgeConfigSpec spec, ConfigType type, boolean synced, @javax.annotation.Nullable Runnable onChange) {
        super(name, FMLPaths.CONFIGDIR.get(), type, synced, onChange);
        this.spec = spec;

        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        if (onChange != null || this.isSynced()) bus.addListener(this::onConfigChange);
        if (this.isSynced()) {

            MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
            MinecraftForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        }

        ModConfig.Type t = this.getConfigType() == ConfigType.COMMON ? ModConfig.Type.COMMON : ModConfig.Type.CLIENT;

        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        this.modConfig = new ModConfig(t, spec, modContainer, name.getNamespace() + "-" + name.getPath() + ".toml");
        //for event
        ConfigSpec.addTrackedSpec(this);
    }

    @Override
    public String getFileName() {
        return modConfig.getFileName();
    }

    @Override
    public Path getFullPath() {
        return FMLPaths.CONFIGDIR.get().resolve(this.getFileName());
        // return modConfig.getFullPath();
    }

    @Override
    public void register() {
        ModContainer modContainer = ModLoadingContext.get().getActiveContainer();
        modContainer.addConfig(this.modConfig);
    }

    @Override
    public void loadFromFile() {
        try {
            CommentedFileConfig configFile = CommentedFileConfig
                    .builder(this.getFullPath())
                    .sync()
                    .preserveInsertionOrder()
                    .writingMode(WritingMode.REPLACE)
                    .build();
            configFile.load();
            configFile.save();

            spec.setConfig(configFile);
        } catch (Exception e) {
            throw new RuntimeException(
                    new IOException("Failed to load " + this.getFileName() + " config. Try deleting it: " + e));
        }
    }

    public ForgeConfigSpec getSpec() {
        return spec;
    }

    @Nullable
    public ModConfig getModConfig() {
        return modConfig;
    }

    public ModConfig.Type getModConfigType() {
        return this.getConfigType() == ConfigType.CLIENT ? ModConfig.Type.CLIENT : ModConfig.Type.COMMON;
    }

    @Override
    public boolean isLoaded() {
        return spec.isLoaded();
    }

    @Nullable
    @Override
    @OnlyIn(Dist.CLIENT)
    public Screen makeScreen(Screen parent, @Nullable ResourceLocation background) {
        var container = ModList.get().getModContainerById(this.getModId());
        if (container.isPresent()) {
            var factory = container.get().getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class);
            if (factory.isPresent()) return factory.get().screenFunction().apply(Minecraft.getInstance(), parent);
        }
        return null;
    }

    @Override
    public boolean hasConfigScreen() {
        return ModList.get().getModContainerById(this.getModId())
                .map(container -> container.getCustomExtension(ConfigScreenHandler.ConfigScreenFactory.class)
                        .isPresent()).orElse(false);
    }

    protected void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            //send this configuration to connected clients
            syncConfigsToPlayer(serverPlayer);
        }
    }

    protected void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity().level().isClientSide) {
            onRefresh();
        }
    }

    protected void onConfigChange(ModConfigEvent event) {
        if (event.getConfig().getSpec() == this.getSpec()) {
            //send this configuration to connected clients
            if (this.isSynced()) sendSyncedConfigsToAllPlayers();
            onRefresh();
        }
    }

    @Override
    public void loadFromBytes(InputStream stream) {
        // try { //this should work the same as below
        //      var b = stream.readAllBytes();
        //     this.modConfig.acceptSyncedConfig(b);
        // } catch (Exception ignored) {
        // }

        //using this isntead so we dont fire the config changes event otherwise this will loop
        this.getSpec().setConfig(TomlFormat.instance().createParser().parse(stream));
        this.onRefresh();
    }


}
