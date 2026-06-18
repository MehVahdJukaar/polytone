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

/**
 * Decides when (and which) chat bubble to show on the Polytone config button, and remembers that
 * decision across sessions in a small json file next to the config options.
 * <p>
 * Two nags, in priority order:
 * <ol>
 *     <li><b>Support</b> — once the config page has been opened {@value #SUPPORT_THRESHOLD}+ times,
 *     a salmon-hearted "consider supporting" message shows on both the config button and the heart
 *     button inside the config screen.</li>
 *     <li><b>Configure me</b> — until the player first clicks the config button, and only while
 *     some pack actually contributed configs, a "configure me!" nudge shows on the config button.</li>
 * </ol>
 */
public class ConfigBubbleManager {

    private static final int SUPPORT_THRESHOLD = 5;
    private static final int SALMON = 0xFA8072;

    private static final String KEY_CLICKED = "config_button_clicked";
    private static final String KEY_OPEN_COUNT = "open_count";
    private static final String KEY_SUPPORT_SHOWN = "support_bubble_shown";
    private static final String KEY_SUPPORT_DISMISSED = "support_dismissed";

    private final File stateFile;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private boolean configClicked = false;
    private int openCount = 0;
    private boolean supportShown = false;
    private boolean supportDismissed = false;

    public ConfigBubbleManager() {
        this.stateFile = PlatStuff.getGamePath().resolve("polytone_popup.json").toFile();
        load();
    }

    // ----- persisted state -----

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
            FilesUtil.writeTextAtomically(stateFile.toPath(), writer ->
                    GsonHelper.writeValue(gson.newJsonWriter(writer), jo, null));
        } catch (Exception e) {
            Polytone.LOGGER.error("Error saving polytone popup state", e);
        }
    }

    // ----- events -----

    /** The player clicked the config button: silence the "configure me" nag forever. */
    public void onConfigButtonClicked() {
        if (!configClicked) {
            configClicked = true;
            save();
        }
    }

    /**
     * The config page was opened (any entry point). Only counts towards the support nag when a pack
     * actually contributed configs — opening just our builtin options doesn't make a packs user.
     */
    public void onConfigOpened(boolean hasPackConfigs) {
        if (hasPackConfigs) {
            openCount++;
            save();
        }
    }

    /** The support page was opened: retire the support nag, but only if it was actually shown first. */
    public void onSupportPageOpened() {
        if (supportShown && !supportDismissed) {
            supportDismissed = true;
            save();
        }
    }

    // ----- appearance decisions -----

    /** Message for the bubble on the pack-screen config button, or null to show nothing. */
    @Nullable
    public Component getConfigButtonMessage(boolean hasPackConfigs) {
        if (shouldShowSupport()) return supportMessage();
        if (!configClicked && hasPackConfigs) {
            return Component.translatable("screen.polytone.config_button.bubble");
        }
        return null;
    }

    /** Message for the bubble on the heart button inside the config screen, or null. */
    @Nullable
    public Component getHeartButtonMessage() {
        return shouldShowSupport() ? supportMessage() : null;
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
