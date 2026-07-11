package net.mehvahdjukaar.polytone.common.attributes;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.codecui.SchemaCodecs;
import net.mehvahdjukaar.polytone.common.expressions.impl.IBlockExp;
import net.mehvahdjukaar.polytone.content.colormap.Colormap;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;

import java.util.function.Supplier;

public class ExtendedAttributeMod {

    public static <A, Value> Codec<Either<A, Supplier<A>>> addDynamicValueCodec(Codec<A> originalCodec, AttributeType<Value> type) {
        if (type == AttributeTypes.ARGB_COLOR || type == AttributeTypes.RGB_COLOR) {
            Codec<Supplier<Integer>> intCodec = Colormap.REFERENCE_OR_EXPRESSION
                    .xmap(c -> () -> {
                                ClientLevel level = Minecraft.getInstance().level;
                                if (level == null) return 0;
                                return c.sampleColor(
                                        level,
                                        null,
                                        ClientFrameTicker.getCameraPos(), ClientFrameTicker.getCameraBiome().value(), null);
                            },
                            supplier -> new IColorGetter.StaticColor(supplier.get()));

            return Codec.either(originalCodec, (Codec) intCodec);
        } else if (type == AttributeTypes.FLOAT || type == AttributeTypes.ANGLE_DEGREES) {
            Codec<Supplier<Float>> flaotCodec = IBlockExp.CODEC
                    .xmap(e -> () -> {
                                ClientLevel level = Minecraft.getInstance().level;
                                if (level == null) return 0f;
                                return (float) e.evaluate(level, ClientFrameTicker.getCameraPos(), null);
                            },
                            ex -> IBlockExp.ZERO);
            return Codec.either(originalCodec, (Codec) flaotCodec);
        }
        return SchemaCodecs.eitherLeft(originalCodec);
    }


}
