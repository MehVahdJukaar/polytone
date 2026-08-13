package net.mehvahdjukaar.polytone.content.config;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.FilesUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;

// Decides when (and which) chat bubble to show on the Polytone config button, and remembers that decision
// across sessions in a small json file next to the config options.
public class ConfigBubbleManager {

    private static final int SUPPORT_THRESHOLD = 5;
    private static final int SALMON = 0xFA8072;

    private static final String KEY_CLICKED = "config_button_clicked";
    private static final String KEY_OPEN_COUNT = "open_count";
    private static final String KEY_SUPPORT_SHOWN = "support_bubble_shown";
    private static final String KEY_SUPPORT_DISMISSED = "support_dismissed";
    private static final String KEY_EDITOR_CLICKED = "editor_button_clicked";

    private final File stateFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private boolean configClicked = false;
    private int openCount = 0;
    private boolean supportShown = false;
    private boolean supportDismissed = false;
    private boolean editorClicked = false;

    public ConfigBubbleManager() {
        this.stateFile = PlatStuff.getGamePath().resolve("polytone_popup.json").toFile();
        load();
    }

    private void load() {
        if (!stateFile.exists()) return;
        try (BufferedReader reader = Files.newReader(stateFile, StandardCharsets.UTF_8)) {
            JsonObject jo = GsonHelper.fromJson(gson, reader, JsonObject.class);
            this.configClicked = GsonHelper.getAsBoolean(jo, KEY_CLICKED, false);
            this.openCount = GsonHelper.getAsInt(jo, KEY_OPEN_COUNT, 0);
            this.supportShown = GsonHelper.getAsBoolean(jo, KEY_SUPPORT_SHOWN, false);
            this.supportDismissed = GsonHelper.getAsBoolean(jo, KEY_SUPPORT_DISMISSED, false);
            this.editorClicked = GsonHelper.getAsBoolean(jo, KEY_EDITOR_CLICKED, false);
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
            jo.addProperty(KEY_EDITOR_CLICKED, editorClicked);
            FilesUtil.writeTextAtomically(stateFile.toPath(), writer ->
                    GsonHelper.writeValue(gson.newJsonWriter(writer), jo, null));
        } catch (Exception e) {
            Polytone.LOGGER.error("Error saving polytone popup state", e);
        }
    }

    // The player clicked the config button: silence the "configure me" nag forever
    public void onConfigButtonClicked() {
        if (!configClicked) {
            configClicked = true;
            save();
        }
    }

    // The config page was opened (any entry point). Only counts towards the support nag when a pack actually
    // contributed configs - opening just our builtin options doesn't make a packs user.
    public void onConfigOpened(boolean hasPackConfigs) {
        if (hasPackConfigs) {
            openCount++;
            save();
        }
    }

    // The player tried the pack editor: silence the "try the editor" nudge forever
    public void onEditorButtonClicked() {
        if (!editorClicked) {
            editorClicked = true;
            save();
        }
    }

    // The support page was opened: retire the support nag, but only if it was actually shown first.
    public void onSupportPageOpened() {
        if (supportShown && !supportDismissed) {
            supportDismissed = true;
            save();
        }
    }

    // Message for the bubble on the pack-screen config button, or null to show nothing
    @Nullable
    public Component getConfigButtonMessage(boolean hasPackConfigs) {
        if (shouldShowSupport()) return supportMessage();
        if (!configClicked && hasPackConfigs) {
            return Component.translatable("screen.polytone.config_button.bubble");
        }
        return null;
    }

    // Message for the bubble on the heart button inside the config screen, or null
    @Nullable
    public Component getHeartButtonMessage() {
        return shouldShowSupport() ? supportMessage() : null;
    }

    // Message for the bubble on the pack-editor button, or null
    @Nullable
    public Component getEditorButtonMessage() {
        // Nudge the editor until the user tries it. Support bubble wins to avoid two bubbles at once.
        if (editorClicked || shouldShowSupport()) return null;
        return Component.translatable("screen.polytone.editor.bubble");
    }

    private boolean shouldShowSupport() {
        if (supportDismissed || openCount < SUPPORT_THRESHOLD) return false;
        // mark (and persist) that the player has now been shown the support nag
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
