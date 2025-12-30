package net.mehvahdjukaar.polytone.common.attributes;

import com.mojang.serialization.Codec;
import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.mehvahdjukaar.polytone.content.block.BlockContextExpression;
import net.mehvahdjukaar.polytone.content.colormap.IColorGetter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.attribute.AttributeType;
import net.minecraft.world.attribute.AttributeTypes;
import net.minecraft.world.level.block.Blocks;

public class ExtendedAttributeMod {

    public static <A, Value> Codec<A> extendValueCodec(Codec<A> argumentCodec, AttributeType<Value> type) {
        if (type == AttributeTypes.ARGB_COLOR || type == AttributeTypes.RGB_COLOR) {
            Codec<Integer> intCodec = IColorGetter.SINGLE_COLOR_OR_EXPRESSION.xmap(
                    c -> c.getColor(Blocks.AIR.defaultBlockState(),
                            Minecraft.getInstance().level,
                            ClientFrameTicker.getCameraPos(), 0),
                    IColorGetter.StaticColor::new
            );
            argumentCodec = (Codec<A>) Codec.either(argumentCodec, intCodec);
        } else if (type == AttributeTypes.FLOAT) {
            Codec<Float> flaotCodec = BlockContextExpression.CODEC
                    .xmap(e -> (float) e.getValue(Minecraft.getInstance().level,
                            ClientFrameTicker.getCameraPos(),
                            Blocks.AIR.defaultBlockState()), ex -> BlockContextExpression.ZERO);
            argumentCodec = (Codec<A>) Codec.either(argumentCodec, flaotCodec);
        }
        return argumentCodec;
    }

}
