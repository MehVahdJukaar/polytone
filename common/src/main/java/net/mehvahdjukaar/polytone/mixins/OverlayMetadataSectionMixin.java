package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DataResult;
import net.minecraft.server.packs.OverlayMetadataSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.regex.Pattern;

@Mixin(OverlayMetadataSection.class)
public class OverlayMetadataSectionMixin {

    @ModifyReturnValue(method = "validateOverlayDir", at = @At("RETURN"))
    private static DataResult<String> polytone$allowNestedOverlayDirs(DataResult<String> original, @Local(argsOnly = true) String dir) {
        return original.result().isEmpty() && polytone$isNestedOverlayDir(dir) ? DataResult.success(dir) : original;
    }

    @Unique
    private static boolean polytone$isNestedOverlayDir(String dir) {
        if (dir.isEmpty() || dir.charAt(0) == '/' || dir.charAt(dir.length() - 1) == '/') {
            return false;
        }
        for (String segment : dir.split("/")) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..") || !Pattern.matches("[-_a-zA-Z0-9.]+", segment)) {
                return false;
            }
        }
        return true;
    }
}
