package net.mehvahdjukaar.polytone.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public interface IRenderProperties {

    Codec<IRenderProperties> CODEC = Types.CODEC.flatXmap(
            DataResult::success,
            irp -> {
                if (irp instanceof Types t) {
                    return DataResult.success(t);
                }
                return DataResult.error(() -> "Tried to serialize non serializable render type: " + irp.getClass());
            }
    );

    Object toVanilla();

    static IRenderProperties wrapVanilla(Object obj){
        return () -> obj;
    }

    enum Types implements StringRepresentable, IRenderProperties {
        SOLID,
        CUTOUT,
        CUTOUT_MIPPED,
        TRIPWIRE,
        TRANSLUCENT;

        private static final Codec<Types> CODEC = StringRepresentable.fromEnum(Types::values);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        @Override
        public Object toVanilla() {
            return switch (this) {
                case SOLID -> RenderType.solid();
                case CUTOUT_MIPPED -> RenderType.cutoutMipped();
                case TRIPWIRE -> RenderType.tripwire();
                case CUTOUT -> RenderType.cutout();
                case TRANSLUCENT -> RenderType.translucent();
            };
        }
    }
}

