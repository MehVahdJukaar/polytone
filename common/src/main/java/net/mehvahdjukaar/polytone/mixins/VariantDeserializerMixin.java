package net.mehvahdjukaar.polytone.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.common.PolytoneModelCodecs;
import net.mehvahdjukaar.polytone.common.SimpleModelStateExtension;
import net.mehvahdjukaar.polytone.common.TransformationModelState;
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
import java.util.Optional;
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

        //needed since the x, y, z fields are made float and use the same name. could replace modded added stuff which is why we use it very carefully
        MapCodec<Variant.SimpleModelState> rotationUnlockedReplaceCodec = RecordCodecBuilder.mapCodec(
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

        MapCodec<Variant.SimpleModelState> wrapper = new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                List<T> l = new ArrayList<>();
                l.addAll(original.keys(ops).toList());
                l.addAll(ExtraData.CODEC.keys(ops).toList());
                return l.stream();
            }

            @Override
            public <T> DataResult<Variant.SimpleModelState> decode(DynamicOps<T> ops, MapLike<T> input) {
                DataResult<Variant.SimpleModelState> originalRes = original.decode(ops, input);
                DataResult<ExtraData> extraData = ExtraData.CODEC.decode(ops, input);
                if (extraData.isSuccess()) {
                    ExtraData data = extraData.getOrThrow();
                    if (!data.isEmpty()) {
                        if (originalRes.isSuccess()) {
                            SimpleModelStateExtension ext = (SimpleModelStateExtension) (Object) originalRes.getOrThrow();
                            data.xOffset().ifPresent(ext::polytone$setXOffset);
                            data.yOffset().ifPresent(ext::polytone$setYOffset);
                            data.zOffset().ifPresent(ext::polytone$setZOffset);
                            data.xRot().ifPresent(ext::polytone$setXRot);
                            data.yRot().ifPresent(ext::polytone$setYRot);
                            data.zRot().ifPresent(ext::polytone$setZRot);
                            return originalRes;
                        }
                        return rotationUnlockedReplaceCodec.decode(ops, input);
                    }

                }
                return originalRes;
            }

            @Override
            public <T> RecordBuilder<T> encode(Variant.SimpleModelState input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                RecordBuilder<T> builder = original.encode(input, ops, prefix);
                SimpleModelStateExtension ext = (SimpleModelStateExtension) (Object) input;
                // Re-emit the extra keys so an edit / re-save round-trip (e.g. the pack editor) doesn't
                // silently drop them. ExtraData keeps only genuinely custom (non-90) rotations; vanilla
                // quadrant rotations are already written by original.encode above, so no key collides.
                ExtraData extra = new ExtraData(
                        Optional.of(ext.polytone$getXRot()),
                        Optional.of(ext.polytone$getYRot()),
                        Optional.of(ext.polytone$getZRot()),
                        ext.polytone$getXOffset() != 0 ? Optional.of(ext.polytone$getXOffset()) : Optional.<Float>empty(),
                        ext.polytone$getYOffset() != 0 ? Optional.of(ext.polytone$getYOffset()) : Optional.<Float>empty(),
                        ext.polytone$getZOffset() != 0 ? Optional.of(ext.polytone$getZOffset()) : Optional.<Float>empty());
                return extra.isEmpty() ? builder : ExtraData.CODEC.encode(extra, ops, builder);
            }
        };
        PolytoneModelCodecs.WRAPPED = wrapper;
        return wrapper;
    }

    @Inject(method = "asModelState", at = @At(value = "HEAD"), cancellable = true)
    public void polytone$addTranslation(CallbackInfoReturnable<ModelState> cir) {

        if (polytone$xOffset != 0 || polytone$yOffset != 0 || polytone$zOffset != 0 ||
                polytone$xRot != 0 || polytone$yRot != 0 || polytone$zRot != 0) {
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
