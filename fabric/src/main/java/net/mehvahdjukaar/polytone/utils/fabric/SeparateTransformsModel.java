package net.mehvahdjukaar.polytone.utils.fabric;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

import static net.minecraft.client.resources.model.ModelBakery.ITEM_MODEL_GENERATOR;

public class SeparateTransformsModel extends BlockModel {

    @Nullable
    public static BlockModel readModel(String loader, JsonDeserializationContext context,
                                       JsonObject jsonobject, BlockModel original) {
        if (loader.equals("forge:separate_transforms")) {
            BlockModel baseModel = context.deserialize(GsonHelper.getAsJsonObject(jsonobject,
                    "base"), BlockModel.class);

            JsonObject perspectiveData = GsonHelper.getAsJsonObject(jsonobject, "perspectives");

            Map<ItemDisplayContext, BlockModel> perspectives = new HashMap<>();
            for (ItemDisplayContext transform : ItemDisplayContext.values()) {
                if (perspectiveData.has(transform.getSerializedName())) {
                    BlockModel perspectiveModel = context.deserialize(GsonHelper.getAsJsonObject(perspectiveData, transform.getSerializedName()), BlockModel.class);
                    perspectives.put(transform, perspectiveModel);
                }
            }

            return new SeparateTransformsModel(baseModel, ImmutableMap.copyOf(perspectives), original);
        }
        return null;
    }


    private final BlockModel baseModel;
    private final ImmutableMap<ItemDisplayContext, BlockModel> perspectives;

    public SeparateTransformsModel(BlockModel baseModel, ImmutableMap<ItemDisplayContext, BlockModel> perspectives,
                                   BlockModel original) {
        super(null, List.of(), Map.of(), original.hasAmbientOcclusion(),
                original.getGuiLight(), original.getTransforms(), original.getOverrides());
        this.baseModel = baseModel;
        this.perspectives = perspectives;
    }

    @Override
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter,
                           ModelState modelState, ResourceLocation modelLocation) {
        boolean usesBlockLight = this.getGuiLight().lightLikeBlock();

        BakedModel base;
        if (baseModel.getRootModel() == ModelBakery.GENERATION_MARKER) {
            base = ITEM_MODEL_GENERATOR.generateBlockModel(spriteGetter, baseModel)
                    .bake(baker, baseModel, spriteGetter, modelState, modelLocation, false);
        } else {
            base = baseModel.bake(baker, spriteGetter, modelState, modelLocation);
        }

        ImmutableMap.Builder<ItemDisplayContext, BakedModel> prespectives = ImmutableMap.builder();
        for (Map.Entry<ItemDisplayContext, BlockModel> entry : perspectives.entrySet()) {
            BlockModel perspective = entry.getValue();
            BakedModel p;
            if (perspective.getRootModel() == ModelBakery.GENERATION_MARKER) {
                p = ITEM_MODEL_GENERATOR.generateBlockModel(spriteGetter, perspective)
                        .bake(baker, perspective, spriteGetter, modelState, modelLocation, false);
            } else {
                p = perspective.bake(baker, spriteGetter, modelState, modelLocation);
            }
            prespectives.put(entry.getKey(), p);
        }

        return new Baked(
                this.hasAmbientOcclusion(), true, usesBlockLight,
                spriteGetter.apply(this.getMaterial("particle")),
                this.getItemOverrides(baker, baseModel),
                base,
                prespectives.build()
        );
    }

    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
        baseModel.resolveParents(resolver);
        perspectives.values().forEach(model -> model.resolveParents(resolver));
    }

    private ItemOverrides getItemOverrides(ModelBaker baker, BlockModel model) {
        return this.getOverrides().isEmpty() ? ItemOverrides.EMPTY : new ItemOverrides(baker, model, this.getOverrides());
    }

    public Collection<BlockModel> getAllModels() {
        Collection<BlockModel> models = new ArrayList<>(perspectives.values());
        models.add(baseModel);
        return models;
    }

    public static class Baked implements BakedModel {
        private final boolean isAmbientOcclusion;
        private final boolean isGui3d;
        private final boolean isSideLit;
        private final TextureAtlasSprite particle;
        private final ItemOverrides overrides;
        private final BakedModel baseModel;
        private final ImmutableMap<ItemDisplayContext, BakedModel> perspectives;

        public Baked(boolean isAmbientOcclusion, boolean isGui3d, boolean isSideLit, TextureAtlasSprite particle, ItemOverrides overrides, BakedModel baseModel, ImmutableMap<ItemDisplayContext, BakedModel> perspectives) {
            this.isAmbientOcclusion = isAmbientOcclusion;
            this.isGui3d = isGui3d;
            this.isSideLit = isSideLit;
            this.particle = particle;
            this.overrides = overrides;
            this.baseModel = baseModel;
            this.perspectives = perspectives;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
            return baseModel.getQuads(state, direction, random);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return isAmbientOcclusion;
        }

        @Override
        public boolean isGui3d() {
            return isGui3d;
        }

        @Override
        public boolean usesBlockLight() {
            return isSideLit;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return particle;
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }

        @Override
        public ItemTransforms getTransforms() {
            return ItemTransforms.NO_TRANSFORMS;
        }

        public BakedModel applyTransform(ItemDisplayContext transformType, PoseStack poseStack, boolean applyLeftHandTransform) {
            if (perspectives.containsKey(transformType)) {
                BakedModel p = perspectives.get(transformType);
                p.getTransforms().getTransform(transformType).apply(applyLeftHandTransform, poseStack);
                return p;
            }
            baseModel.getTransforms().getTransform(transformType).apply(applyLeftHandTransform, poseStack);
            return baseModel;
        }

    }
}
