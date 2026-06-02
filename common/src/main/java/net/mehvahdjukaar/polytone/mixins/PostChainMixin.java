package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.shaders.PostChainJsonRewriter;
import net.mehvahdjukaar.polytone.content.shaders.PostShadersManager;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rewrites the JSON loaded by {@link PostChain} from the new (1.21.2+) post-effect schema into
 * the old (1.21.1) one, only when {@link PostShadersManager#POLYTONE_LOADING} is set on the
 * current thread. Vanilla / other-mod PostChain loads pass through untouched.
 */
@Mixin(PostChain.class)
public abstract class PostChainMixin {

    @ModifyExpressionValue(method = "load",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/GsonHelper;parse(Ljava/io/Reader;)Lcom/google/gson/JsonObject;"))
    private JsonObject polytone$rewriteNewFormatJson(JsonObject parsed) {
        if (!Boolean.TRUE.equals(PostShadersManager.POLYTONE_LOADING.get())) return parsed;
        if (parsed == null) return parsed;
        if (!PostChainJsonRewriter.isNewFormat(parsed)) return parsed;
        try {
            PostChainJsonRewriter.rewrite(parsed);
        } catch (Exception e) {
            Polytone.LOGGER.error("Failed to rewrite post chain JSON from new format", e);
        }
        return parsed;
    }
}
