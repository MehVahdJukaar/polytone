package net.mehvahdjukaar.polytone.mixins.codec_ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.SchemaTags;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Tags the {@link Codec} returned by {@link Registry#byNameCodec()} with a
 * {@link Schema.ResourceId} carrying this registry's key. The resolver then routes the codec
 * to a {@code ResourceIdWidget} (dropdown / picker) instead of the inherited String schema.
 *
 * <p>Because {@code byNameCodec()} is a default method on {@code Registry}, this mixin fires
 * for every concrete registry implementation — vanilla, mod registries, custom ones.</p>
 */
@Mixin(Registry.class)
public interface RegistryByNameCodecMixin<T> {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @ModifyReturnValue(method = "byNameCodec", at = @At("RETURN"))
    private Codec<T> polytone$tagByNameCodec(Codec<T> wrapped) {
        try {
            ResourceKey<? extends Registry<T>> key = ((Registry<T>) this).key();
            Schema.ResourceId schema = new Schema.ResourceId(key);
            SchemaTags.tag((Codec) wrapped, (Schema) schema);
        } catch (Throwable ignored) {
            // Best-effort.
        }
        return wrapped;
    }
}
