package net.mehvahdjukaar.polytone.moonlight_configs.fabric;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.mehvahdjukaar.polytone.moonlight_configs.ConfigSpec;
import net.mehvahdjukaar.polytone.moonlight_configs.ConfigType;
import net.mehvahdjukaar.polytone.moonlight_configs.yacl.YACLCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.io.*;
import java.nio.charset.StandardCharsets;

import static net.mehvahdjukaar.polytone.moonlight_configs.fabric.ConfigBuilderImpl.getReadableName;

public class FabricConfigSpec extends ConfigSpec {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final ResourceLocation res;
    private final ConfigSubCategory mainEntry;
    private final File file;

    public FabricConfigSpec(ResourceLocation name, ConfigSubCategory mainEntry, ConfigType type, boolean synced, Runnable changeCallback) {
        super(name, FabricLoader.getInstance().getConfigDir(), type, synced, changeCallback);
        this.file = this.getFullPath().toFile();
        this.mainEntry = mainEntry;
        this.res = name;
        if (this.isSynced()) {
            ServerPlayConnectionEvents.JOIN.register(this::onPlayerLoggedIn);
        }
    }

    public ConfigSubCategory getMainEntry() {
        return mainEntry;
    }

    @Override
    public void register() {
        FabricConfigSpec.addTrackedSpec(this);
    }

    @Override
    public void loadFromFile() {
        JsonElement config = null;

        if (file.exists() && file.isFile()) {
            try (FileInputStream fileInputStream = new FileInputStream(file);
                 InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {

                config = GSON.fromJson(bufferedReader, JsonElement.class);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load config", e);
            }
        }

        if (config instanceof JsonObject jo) {
            //don't call load directly so we skip the main category name
            mainEntry.getEntries().forEach(e -> e.loadFromJson(jo));
        }
    }

    public void saveConfig() {
        try (FileOutputStream stream = new FileOutputStream(this.file);
             Writer writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {

            JsonObject jo = new JsonObject();
            mainEntry.getEntries().forEach(e -> e.saveToJson(jo));

            GSON.toJson(jo, writer);
        } catch (IOException ignored) {
        }
        this.onRefresh();
    }

    public Component getName() {
        return Component.literal(getReadableName(this.res.getPath()+"_configs"));
    }

    private static final boolean YACL = FabricLoader.getInstance().isModLoaded("yet-another-config-lib");
    private static final boolean CLOTH_CONFIG = FabricLoader.getInstance().isModLoaded("cloth-config");

    @Override
    @Environment(EnvType.CLIENT)
    public Screen makeScreen(Screen parent, ResourceLocation background) {
        if (CLOTH_CONFIG) {
           // return ClothConfigCompat.makeScreen(parent, this, background);
        } if(YACL){
            return YACLCompat.makeScreen(parent, this, background);
        }
        return null;
    }

    @Override
    public boolean hasConfigScreen() {
        return CLOTH_CONFIG || YACL;
    }

    @Override
    public void loadFromBytes(InputStream stream) {
        InputStreamReader inputStreamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        JsonElement config = GSON.fromJson(bufferedReader, JsonElement.class);
        if (config instanceof JsonObject jo) {
            //don't call load directly, so we skip the main category name
            mainEntry.getEntries().forEach(e -> e.loadFromJson(jo));
        }
        this.onRefresh();
    }


    private void onPlayerLoggedIn(ServerGamePacketListenerImpl listener, PacketSender sender, MinecraftServer minecraftServer) {
        //send this configuration to connected clients
        syncConfigsToPlayer(listener.player);
    }

}
