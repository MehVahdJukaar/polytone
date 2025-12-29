package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.misc.SimpleModelStateExtension;
import net.mehvahdjukaar.polytone.misc.TransformationModelState;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Mixin(Variant.SimpleModelState.class)
public class VariantDeserializerMixin implements SimpleModelStateExtension {

    @Shadow
    @Final
    private boolean uvLock;
    @Unique
    private float polytone$xOffset = 0;
    @Unique
    private float polytone$yOffset = 0;
    @Unique
    private float polytone$zOffset = 0;
    @Unique
    private float polytone$xRot = 0;
    @Unique
    private float polytone$yRot = 0;
    @Unique
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
                            var state = new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, Quadrant.R0, t4);
                            ((SimpleModelStateExtension) (Object) state).polytone$setXRot(t1);
                            ((SimpleModelStateExtension) (Object) state).polytone$setYRot(t2);
                            ((SimpleModelStateExtension) (Object) state).polytone$setZRot(t3);
                            ((SimpleModelStateExtension) (Object) state).polytone$setXOffset(t5);
                            ((SimpleModelStateExtension) (Object) state).polytone$setYOffset(t6);
                            ((SimpleModelStateExtension) (Object) state).polytone$setZOffset(t8);
                            return state;
                        })
        );

        return new MapCodec<>() {
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

    @Inject(method = "asModelState", at = @At(value = "HEAD"), cancellable = true)
    public void polytone$addTranslation(CallbackInfoReturnable<ModelState> cir) {

        if (polytone$xOffset != 0 || polytone$yOffset != 0 || polytone$zOffset != 0 || polytone$xRot != 0 || polytone$yRot != 0 || polytone$zRot != 0) {
            Matrix4f mat = new Matrix4f();
            Quaternionf quaternionf = (new Quaternionf())
                    .rotateYXZ(-polytone$yRot * Mth.DEG_TO_RAD,
                            -polytone$xRot * Mth.DEG_TO_RAD, -polytone$zRot * Mth.DEG_TO_RAD);
            mat.translate(polytone$xOffset / 16f, polytone$yOffset / 16f, polytone$zOffset / 16f);
            mat.rotate(quaternionf);
            cir.setReturnValue(new TransformationModelState(new Transformation(mat), uvLock));
        }
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
