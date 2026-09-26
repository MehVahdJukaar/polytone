package net.mehvahdjukaar.polytone.common;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

//can mods stop skipping resource locaiton validation?? It causes coutless issues for mods when actually invalid resource paths are shoved around FFS
public class IdentifierSanityCheck {

    public static void validateNamespace(String namespace, String path) {
        for (int i = 0; i < namespace.length(); i++) {
            char c = namespace.charAt(i);
            //no regex, hot path
            if (!(c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '.' || c == '-')) {
                logAppliedMixins();
                throw new IllegalStateException("Invalid Identifier namespace: " + namespace + ":" + path +
                        ". Some mod disabled vanilla id validation, check the mixins applied to Identifier. This is VERY BAD!");
            }
        }
    }

    private static void logAppliedMixins() {
        try {
            ClassInfo info = ClassInfo.forName(Identifier.class.getName());
            if (info == null) return;
            Polytone.LOGGER.error("Mixins applied to Identifier:");
            for (IMixinInfo mixin : info.getAppliedMixins()) {
                Polytone.LOGGER.error(" - {} ({})", mixin.getClassName(), mixin.getConfig().getName());
            }
        } catch (Exception ignored) {
        }
    }
}
