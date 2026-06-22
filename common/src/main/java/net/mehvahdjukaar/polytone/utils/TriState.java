package net.mehvahdjukaar.polytone.utils;

/**
 * Minimal tri-state used by the config-driven overlay system. Vanilla 1.21.1 has no
 * {@code net.minecraft.util.TriState}, so we ship our own.
 */
public enum TriState {
    TRUE,
    FALSE,
    DEFAULT
}
