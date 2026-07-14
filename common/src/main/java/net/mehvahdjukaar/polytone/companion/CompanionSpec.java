package net.mehvahdjukaar.polytone.companion;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The companion-file half of a content type's contract with the pack: files that live next
 * to a content JSON by naming convention and that no codec field can see - a colormap's
 * {@code .png} textures today; tied shader stages, models, and other (possibly softly-tied)
 * assets tomorrow. ONE spec per content type is the single source of truth: the runtime
 * reloaders fill textures with the same naming primitives and the editor renders
 * {@link #expectedSlots} directly, so the two can't drift apart.
 *
 * <p>Pure logic over simple file names (with extension) and the JSON's {@code stem} (its file
 * name without extension): no IO and no Minecraft types, so both the {@code ResourceLocation}-keyed
 * runtime and the {@code Path}-keyed editor can consume it. All matching is case-insensitive;
 * implementations must not assume lowercase input.</p>
 */
public interface CompanionSpec<V> {

    /**
     * The role {@code fileName} plays for content named {@code stem} - a short display label
     * like {@code "default"} or {@code "tint 3"} - or null when the file is not associated
     * with that stem by this convention at all.
     */
    @Nullable
    String classify(String fileName, String stem);

    /**
     * The slots this content actually expects given its parsed value: what the runtime will
     * look up and, when {@link CompanionSlot#required()}, error about when absent.
     * {@code parsedValue} null = the JSON is not currently parseable - return the most
     * permissive guess (typically a single optional slot).
     */
    List<CompanionSlot> expectedSlots(@Nullable V parsedValue, String stem);
}
