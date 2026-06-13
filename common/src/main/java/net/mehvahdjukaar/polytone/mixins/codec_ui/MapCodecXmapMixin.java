package net.mehvahdjukaar.polytone.mixins.codec_ui;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.polytone.common.codec_ui.Schema;
import net.mehvahdjukaar.polytone.common.codec_ui.SchemaResolver;
import net.mehvahdjukaar.polytone.common.codec_ui.internal.SchemaTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mirror of {@link CodecXmapMixin} for {@code MapCodec.xmap} / {@code flatXmap}.
 * Many vanilla CODECs are built like {@code something.fieldOf("x").xmap(ctor, getter)} — the
 * xmap is on the MapCodec, not the Codec. Without this mixin, those outputs are opaque to the
 * resolver and the entire record falls to a raw JSON editor.
 */
@Mixin(MapCodec.class)
public abstract class MapCodecXmapMixin {

    @ModifyReturnValue(method = "xmap", at = @At("RETURN"))
    private MapCodec<?> polytone$tagXmap(MapCodec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "flatXmap", at = @At("RETURN"))
    private MapCodec<?> polytone$tagFlatXmap(MapCodec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @ModifyReturnValue(method = "validate", at = @At("RETURN"))
    private MapCodec<?> polytone$tagValidate(MapCodec<?> wrapped) {
        polytone$inheritInner(wrapped);
        return wrapped;
    }

    @Unique
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void polytone$inheritInner(MapCodec<?> wrapped) {
        if (wrapped == null || wrapped == (Object) this) return;
        try {
            MapCodec<Object> inner = (MapCodec<Object>)(Object) this;
            Schema<Object> innerSchema = SchemaTags.lookupMap(inner);
            if (innerSchema == null) innerSchema = SchemaResolver.get().resolveMap(inner);
            SchemaTags.tag((MapCodec) wrapped, (Schema) innerSchema);
        } catch (Throwable ignored) {
            // Best-effort.
        }
    }
}
