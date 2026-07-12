package net.mehvahdjukaar.polytone.content.config;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.FilesUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * Decides when (and which) chat bubble to show on the Polytone config button, and remembers that
 * decision across sessions in a small json file next to the config options.
 */
public class ConfigBubbleManager {

    private static final int SUPPORT_THRESHOLD = 3;

    private static final String KEY_CLICKED = "config_button_clicked";
    private static final String KEY_OPEN_COUNT = "open_count";
    private static final String KEY_SUPPORT_SHOWN = "support_bubble_shown";
    private static final String KEY_SUPPORT_DISMISSED = "support_dismissed";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private File stateFile;

    private boolean configClicked = false;
    private int openCount = 0;
    private boolean supportShown = false;
    private boolean supportDismissed = false;

    public ConfigBubbleManager() {
    }

    //ugly lazy init.
    private File stateFile() {
        if (stateFile == null) {
            stateFile = Minecraft.getInstance().gameDirectory.toPath().resolve("polytone_popup.json").toFile();
            load();
        }
        return stateFile;
    }

    private void load() {
        if (!stateFile.exists()) return;
        try (BufferedReader reader = Files.newReader(stateFile, StandardCharsets.UTF_8)) {
            JsonObject jo = GsonHelper.fromJson(gson, reader, JsonObject.class);
            this.configClicked = GsonHelper.getAsBoolean(jo, KEY_CLICKED, false);
            this.openCount = GsonHelper.getAsInt(jo, KEY_OPEN_COUNT, 0);
            this.supportShown = GsonHelper.getAsBoolean(jo, KEY_SUPPORT_SHOWN, false);
            this.supportDismissed = GsonHelper.getAsBoolean(jo, KEY_SUPPORT_DISMISSED, false);
        } catch (Exception e) {
            Polytone.LOGGER.error("Error loading polytone popup state", e);
        }
    }

    private void save() {
        try {
            JsonObject jo = new JsonObject();
            jo.addProperty(KEY_CLICKED, configClicked);
            jo.addProperty(KEY_OPEN_COUNT, openCount);
            jo.addProperty(KEY_SUPPORT_SHOWN, supportShown);
            jo.addProperty(KEY_SUPPORT_DISMISSED, supportDismissed);
            FilesUtil.writeTextAtomically(stateFile().toPath(), writer ->
                    GsonHelper.writeValue(gson.newJsonWriter(writer), jo, null));
        } catch (Exception e) {
            Polytone.LOGGER.error("Error saving polytone popup state", e);
        }
    }

    public void onConfigButtonClicked() {
        stateFile();
        if (!configClicked) {
            configClicked = true;
            save();
        }
    }

    public void onConfigOpened(boolean hasPackConfigs) {
        stateFile();
        if (hasPackConfigs) {
            openCount++;
            save();
        }
    }

    public void onSupportPageOpened() {
        stateFile();
        if (supportShown && !supportDismissed) {
            supportDismissed = true;
            save();
        }
    }

    @Nullable
    public Component getConfigButtonMessage(boolean hasPackConfigs) {
        stateFile();
        if (shouldShowSupport()) return supportMessage();
        if (!configClicked && hasPackConfigs) {
            return Component.translatable("screen.polytone.config_button.bubble");
        }
        return null;
    }

    @Nullable
    public Component getHeartButtonMessage() {
        stateFile();
        return shouldShowSupport() ? supportMessage() : null;
    }

    private boolean shouldShowSupport() {
        if (supportDismissed || openCount < SUPPORT_THRESHOLD) return false;
        if (!supportShown) {
            supportShown = true;
            save();
        }
        return true;
    }

    private static Component supportMessage() {
        return Component.translatable("screen.polytone.config_button.support");
    }
}
