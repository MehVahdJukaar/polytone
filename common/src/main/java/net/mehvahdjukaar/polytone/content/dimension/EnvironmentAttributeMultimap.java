package net.mehvahdjukaar.polytone.content.dimension;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Util;
import net.minecraft.world.attribute.*;
import net.minecraft.world.attribute.modifier.AttributeModifier;

import java.util.List;
import java.util.Map;

public class EnvironmentAttributeMultimap {

    public static final Codec< Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>>> CODEC = Codec.lazyInitialized(() -> 
            Codec.dispatchedMap(EnvironmentAttributes.CODEC, environmentAttribute ->
                    listOrSingleValue(environmentAttribute);

    private static <T> Codec<EnvironmentAttributeMap.Entry<T, ?>> createCodec(EnvironmentAttribute<T> environmentAttribute) {
        Codec<EnvironmentAttributeMap.Entry<T, ?>> fullCodec = environmentAttribute.type().modifierCodec()
                .dispatch("modifier", EnvironmentAttributeMap.Entry::modifier, Util.memoize((attributeModifier) -> createFullCodec(environmentAttribute, attributeModifier)));
        Codec<T> flatCodec = environmentAttribute.valueCodec();
        return Codec.either(flatCodec, fullCodec).xmap((either) ->
                either.map((object) -> new EnvironmentAttributeMap.Entry<>(object, AttributeModifier.override()),
                        (entry) -> entry),
                (entry) -> entry.modifier == AttributeModifier.override() ? Either.left(entry.argument()) :
                        Either.right(entry));
    }

    private static <T, Argument> MapCodec<EnvironmentAttributeMap.Entry<T, Argument>> createFullCodec(
            EnvironmentAttribute<T> environmentAttribute,
            AttributeModifier<T, Argument> mod) {
        return RecordCodecBuilder.mapCodec((i) -> i.group(
                mod.argumentCodec(environmentAttribute).fieldOf("argument").forGetter(EnvironmentAttributeMap.Entry::argument)
        ).apply(i, (object) -> new EnvironmentAttributeMap.Entry<>(object, mod)));
    }

    private static Codec<EnvironmentAttributeLayer<?>> listOrSingleValue(EnvironmentAttribute<?> environmentAttribute) {
        Codec<?> codec = environmentAttribute.valueCodec();
        Codec<List<EnvironmentAttributeLayer<?>>> listCodec = codec.listOf();
        Codec<EnvironmentAttributeLayer<?>> c = Codec.withAlternative(listCodec,
                codec.xmap(v -> List.of(v), l -> l.getFirst()));
    }


    private Map<EnvironmentAttribute<?>, List<EnvironmentAttributeLayer<?>>> map;



}
