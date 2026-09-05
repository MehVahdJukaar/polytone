package net.mehvahdjukaar.polytone.content.expmodel;

import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaRecord;
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

//each loader wraps a Selector in its own baked model type. cases are vanilla Unbaked so they can
//themselves be weighted lists: context routing outside, vanilla variety inside
public final class ExpressionModel {

    public static final Identifier ID = Polytone.res("expression");

    private ExpressionModel() {}

    // One unbaked routing case: render model when when evaluates non-zero
    public record Case(BlockExp when, BlockStateModel.Unbaked model) {
        public static final SchemaCodec<Case> CODEC = SchemaRecord.create(Case.class, i -> i.group(
                i.field("when", BlockExp.TYPE.codec(), Case::when),
                i.field("model", BlockStateModel.Unbaked.CODEC, Case::model)
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

    // Baked selection logic shared by every loader's wrapper model
    public static final class Selector {
        private final List<BakedCase> cases;
        private final BlockExp selector; // nullable
        private final BlockStateModel fallback;

        private Selector(List<BakedCase> cases, BlockExp selector, BlockStateModel fallback) {
            this.cases = cases;
            this.selector = selector;
            this.fallback = fallback;
        }

        // index of the first matching case, or -1 to mean the fallback
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
