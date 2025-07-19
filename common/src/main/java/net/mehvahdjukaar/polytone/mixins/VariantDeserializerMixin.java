package net.mehvahdjukaar.polytone.mixins;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.utils.SimpleModelStateExtension;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.util.GsonHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mixin(Variant.SimpleModelState.class)
public class VariantDeserializerMixin implements SimpleModelStateExtension {

    private float polytone$xOffset = 0;
    private float polytone$yOffset = 0;
    private float polytone$zOffset = 0;
    private float polytone$xRot = 0;
    private float polytone$yRot = 0;
    private float polytone$zRot = 0;

    @ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"))
    private static MapCodec<Variant.SimpleModelState> polytone$modifyCodec(MapCodec<Variant.SimpleModelState> original) {

        MapCodec<Variant.SimpleModelState> betterCodec = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                                Codec.FLOAT.optionalFieldOf("x", 0f).forGetter(a -> ((SimpleModelStateExtension) (Object) a).polytone$getXRot()),
                                Codec.FLOAT.optionalFieldOf("y", 0f).forGetter(a -> ((SimpleModelStateExtension) (Object) a).polytone$getYRot()),
                                Codec.FLOAT.optionalFieldOf("z", 0f).forGetter(a -> ((SimpleModelStateExtension) (Object) a).polytone$getZRot()),
                                Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(Variant.SimpleModelState::uvLock),
                                Codec.FLOAT.optionalFieldOf("xoffset", 0f).forGetter(a -> ((SimpleModelStateExtension) (Object) a).polytone$getXOffset()),
                                Codec.FLOAT.optionalFieldOf("yoffset", 0f).forGetter(a -> ((SimpleModelStateExtension) (Object) a).polytone$getYOffset()),
                                Codec.FLOAT.optionalFieldOf("zoffset", 0f).forGetter(a -> ((SimpleModelStateExtension) (Object) a).polytone$getZOffset())
                        )
                        .apply(instance, (t1, t2, t3, t4, t5, t6, t8) -> {
                            var state = new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, t4);
                            ((SimpleModelStateExtension) (Object) state).polytone$setXRot(t1);
                            ((SimpleModelStateExtension) (Object) state).polytone$setYRot(t2);
                            ((SimpleModelStateExtension) (Object) state).polytone$setZRot(t3);
                            ((SimpleModelStateExtension) (Object) state).polytone$setXOffset(t5);
                            ((SimpleModelStateExtension) (Object) state).polytone$setYOffset(t6);
                            ((SimpleModelStateExtension) (Object) state).polytone$setZOffset(t8);
                            return state;
                        })
        );

        return new MapCodec<Variant.SimpleModelState>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                List<T> l = new ArrayList<>();
                l.addAll(original.keys(ops).toList());
                l.addAll(betterCodec.keys(ops).toList());
                return l.stream();
            }

            @Override
            public <T> DataResult<Variant.SimpleModelState> decode(DynamicOps<T> ops, MapLike<T> input) {
                DataResult<Variant.SimpleModelState> betterResult = betterCodec.decode(ops, input);
                if (betterResult.isSuccess()) return betterResult;
                DataResult<Variant.SimpleModelState> originalResult = original.decode(ops, input);
                if (originalResult.isSuccess()) return originalResult;
                return betterResult;
            }

            @Override
            public <T> RecordBuilder<T> encode(Variant.SimpleModelState input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return betterCodec.encode(input, ops, prefix);
            }
        };
    }

    @ModifyExpressionValue(method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/Variant;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/model/BlockModelRotation;getRotation()Lcom/mojang/math/Transformation;")
    )
    public Transformation polytone$addTranslation(Transformation original, @Local JsonObject jsonObject) {
        float x = GsonHelper.getAsFloat(jsonObject, "xoffset", 0);
        float y = GsonHelper.getAsFloat(jsonObject, "yoffset", 0);
        float z = GsonHelper.getAsFloat(jsonObject, "zoffset", 0);
        if (x == 0 && y == 0 && z == 0) return original;
        Matrix4f mat = new Matrix4f();
        mat.translate(x / 16f, y / 16f, z / 16f);
        return new Transformation(mat).compose(original);
    }

    @Override
    public void polytone$setXOffset(float xOffset) {
        this.polytone$xOffset = xOffset;
    }

    @Override
    public void polytone$setYOffset(float yOffset) {
        this.polytone$yOffset = yOffset;
    }

    @Override
    public void polytone$setZOffset(float zOffset) {
        this.polytone$zOffset = zOffset;
    }

    @Override
    public void polytone$setXRot(float xRot) {
        this.polytone$xRot = xRot;
    }

    @Override
    public void polytone$setYRot(float yRot) {
        this.polytone$yRot = yRot;
    }

    @Override
    public void polytone$setZRot(float zRot) {
        this.polytone$zRot = zRot;
    }

    @Override
    public float polytone$getXOffset() {
        return polytone$xOffset;
    }

    @Override
    public float polytone$getYOffset() {
        return polytone$yOffset;
    }

    @Override
    public float polytone$getZOffset() {
        return polytone$zOffset;
    }

    @Override
    public float polytone$getXRot() {
        return polytone$xRot;
    }

    @Override
    public float polytone$getYRot() {
        return polytone$yRot;
    }

    @Override
    public float polytone$getZRot() {
        return polytone$zRot;
    }
}
