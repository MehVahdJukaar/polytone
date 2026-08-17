package net.mehvahdjukaar.polytone.content.expmodel;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.content.common.expressions.impl.BlockExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// 1.21.1 has no custom blockstate model types... we subclass MultiVariant and swap it in from the deserializer.
public class ExpressionModel extends MultiVariant {

    public static final String ID = "polytone:expression";
    private static final String[] TYPE_KEYS = {"type", "fabric:type", "neoforge:type"};

    private final List<Case> cases;
    @Nullable
    private final BlockExp selector;
    private final MultiVariant fallback;

    private ExpressionModel(List<Case> cases, @Nullable BlockExp selector, MultiVariant fallback) {
        super(flatten(cases, fallback));
        this.cases = List.copyOf(cases);
        this.selector = selector;
        this.fallback = fallback;
    }

    private static List<Variant> flatten(List<Case> cases, MultiVariant fallback) {
        List<Variant> all = new ArrayList<>(fallback.getVariants());
        for (Case c : cases) all.addAll(c.model.getVariants());
        return all;
    }

    public static boolean isExpressionModel(JsonElement json) {
        if (!json.isJsonObject()) return false;
        JsonObject obj = json.getAsJsonObject();
        for (String key : TYPE_KEYS) {
            if (obj.has(key) && ID.equals(GsonHelper.getAsString(obj, key))) return true;
        }
        return false;
    }

    public static ExpressionModel parse(JsonObject json, JsonDeserializationContext context) {
        List<Case> cases = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "cases")) {
            JsonObject caseJson = GsonHelper.convertToJsonObject(element, "case");
            cases.add(new Case(parseExpression(GsonHelper.getNonNull(caseJson, "when")),
                    context.deserialize(GsonHelper.getNonNull(caseJson, "model"), MultiVariant.class)));
        }
        if (cases.isEmpty()) throw new JsonParseException("Expression model needs at least one case");

        BlockExp selector = json.has("selector") ? parseExpression(json.get("selector")) : null;
        MultiVariant fallback = context.deserialize(GsonHelper.getNonNull(json, "fallback"), MultiVariant.class);
        return new ExpressionModel(cases, selector, fallback);
    }

    private static BlockExp parseExpression(JsonElement json) {
        return BlockExp.TYPE.codec().parse(JsonOps.INSTANCE, json).getOrThrow(JsonParseException::new);
    }

    @Override
    @Nullable
    public BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState state) {
        BakedModel bakedFallback = fallback.bake(baker, spriteGetter, state);
        if (bakedFallback == null) return null;

        List<BakedCase> baked = new ArrayList<>(cases.size());
        for (Case c : cases) {
            BakedModel model = c.model.bake(baker, spriteGetter, state);
            if (model != null) baked.add(new BakedCase(c.when, model));
        }
        return PlatStuff.makeExpressionModel(new Selector(List.copyOf(baked), selector, bakedFallback));
    }

    // MultiVariant compares by variant list, which would merge two expression models sharing their models
    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    private record Case(BlockExp when, MultiVariant model) {}

    private record BakedCase(BlockExp when, BakedModel model) {}

    public static final class Selector {
        private final List<BakedCase> cases;
        @Nullable
        private final BlockExp selector;
        private final BakedModel fallback;

        private Selector(List<BakedCase> cases, @Nullable BlockExp selector, BakedModel fallback) {
            this.cases = cases;
            this.selector = selector;
            this.fallback = fallback;
        }

        public BakedModel select(BlockPos pos, BlockState state) {
            LevelReader level = Minecraft.getInstance().level;
            if (level == null) return fallback;
            Vec3 p = Vec3.atLowerCornerOf(pos);
            double v = selector == null ? 0 : selector.evaluate(level, p, state, 0);
            for (BakedCase c : cases) {
                if (c.when.evaluate(level, p, state, v) != 0) return c.model;
            }
            return fallback;
        }

        public BakedModel fallback() {
            return fallback;
        }
    }
}
