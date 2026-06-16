package net.mehvahdjukaar.polytone.content.expmodel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.BlockExp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Loader-agnostic core for the {@code polytone:expression} blockstate model: the unbaked data, its
 * codec, and the per-block-position selection logic. Each loader wraps a {@link Selector} in its own
 * baked model type (NeoForge {@code DynamicBlockStateModel}, Fabric {@code FabricBlockStateModel}).
 * <p>
 * The first {@link Case} whose {@code when} expression is truthy (non-zero) wins; if none match the
 * {@code fallback} is used. Each {@code model} is a vanilla {@link BlockStateModel.Unbaked}, so it may
 * itself be a single variant or a weighted random list - context routing on the outside, vanilla
 * per-position random variety on the inside.
 * <p>
 * An optional {@code selector} expression is evaluated once per position and bound to {@code v}, so
 * cases that all key off the same quantity can be written compactly.
 * <pre>{@code
 * {
 *   "variants": {
 *     "": {
 *       "type": "polytone:expression",          // NeoForge; on Fabric use "fabric:type"
 *       "selector": "TEMPERATURE",              // optional; its value is bound to `v` in each case
 *       "cases": [
 *         { "when": "v > 0.8", "model": { "model": "minecraft:block/red_sand" } },
 *         { "when": "v > 0.3", "model": [ {"model":"a"}, {"model":"b"} ] }
 *       ],
 *       "fallback": { "model": "minecraft:block/sand" }
 *     }
 *   }
 * }
 * }</pre>
 * Without a {@code selector}, each {@code when} is a standalone expression (and {@code v} is 0).
 */
public final class ExpressionModel {

    public static final Identifier ID = Polytone.res("expression");

    private ExpressionModel() {}

    /** One unbaked routing case: render {@code model} when {@code when} evaluates non-zero. */
    public record Case(BlockExp when, BlockStateModel.Unbaked model) {
        public static final Codec<Case> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockExp.TYPE.codec().fieldOf("when").forGetter(Case::when),
                BlockStateModel.Unbaked.CODEC.fieldOf("model").forGetter(Case::model)
        ).apply(i, Case::new));
    }

    private record BakedCase(BlockExp when, BlockStateModel model) {}

    public static Selector bake(List<Case> cases, Optional<BlockExp> selector,
                                BlockStateModel.Unbaked fallback, ModelBaker baker) {
        List<BakedCase> baked = new ArrayList<>(cases.size());
        for (Case c : cases) baked.add(new BakedCase(c.when(), c.model().bake(baker)));
        return new Selector(List.copyOf(baked), selector.orElse(null), fallback.bake(baker));
    }

    public static void resolveDependencies(List<Case> cases, BlockStateModel.Unbaked fallback, ResolvableModel.Resolver resolver) {
        for (Case c : cases) c.model().resolveDependencies(resolver);
        fallback.resolveDependencies(resolver);
    }

    /** Baked selection logic shared by every loader's wrapper model. */
    public static final class Selector {
        private final List<BakedCase> cases;
        private final BlockExp selector; // nullable
        private final BlockStateModel fallback;

        private Selector(List<BakedCase> cases, BlockExp selector, BlockStateModel fallback) {
            this.cases = cases;
            this.selector = selector;
            this.fallback = fallback;
        }

        /** @return index of the first matching case, or -1 to mean the fallback. */
        public int selectIndex(BlockPos pos, BlockState state) {
            // The render-time level is a BlockAndTintGetter, but the expression engine wants a
            // LevelReader for biome/light lookups - use the client level, same as colormaps do.
            LevelReader level = Minecraft.getInstance().level;
            if (level == null) return -1;
            Vec3 p = Vec3.atLowerCornerOf(pos);
            double v = selector == null ? 0 : selector.evaluate(level, p, state);
            for (int i = 0; i < cases.size(); i++) {
                if (cases.get(i).when().evaluate(level, p, state, v) != 0) return i;
            }
            return -1;
        }

        public BlockStateModel select(BlockPos pos, BlockState state) {
            int i = selectIndex(pos, state);
            return i < 0 ? fallback : cases.get(i).model();
        }

        public BlockStateModel fallback() {
            return fallback;
        }

        public TextureAtlasSprite particleIcon() {
            return fallback.particleIcon();
        }
    }
}
