package net.mehvahdjukaar.polytone.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.bedrock.molang.MolangExpr;
import net.minecraft.util.StringRepresentable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// A curve is read back as a Molang variable of the same name, so it acts as a function of input in the expression
// environment. bezier_chain keys its control points by position instead of listing them, hence the two Nodes shapes.
public record BedrockCurve(Type type, Optional<MolangExpr> input, Optional<MolangExpr> horizontalRange,
                           Nodes nodes) {

    public static final Codec<BedrockCurve> CODEC = RecordCodecBuilder.create(i -> i.group(
            Type.CODEC.optionalFieldOf("type", Type.LINEAR).forGetter(BedrockCurve::type),
            MolangExpr.CODEC.optionalFieldOf("input").forGetter(BedrockCurve::input),
            MolangExpr.CODEC.optionalFieldOf("horizontal_range").forGetter(BedrockCurve::horizontalRange),
            Nodes.CODEC.optionalFieldOf("nodes", new Nodes.Listed(List.of())).forGetter(BedrockCurve::nodes)
    ).apply(i, BedrockCurve::new));

    public sealed interface Nodes {

        Codec<Nodes> CODEC = Codec.withAlternative(
                BedrockCodecs.branch(Listed.CODEC, Listed.class),
                BedrockCodecs.branch(Chained.CODEC, Chained.class));

        record Listed(List<MolangExpr> values) implements Nodes {
            public static final Codec<Listed> CODEC = MolangExpr.CODEC.listOf().xmap(Listed::new, Listed::values);
        }

        record Chained(Map<String, ChainNode> values) implements Nodes {
            public static final Codec<Chained> CODEC =
                    Codec.unboundedMap(Codec.STRING, ChainNode.CODEC).xmap(Chained::new, Chained::values);
        }
    }

    public record ChainNode(Optional<MolangExpr> leftValue, Optional<MolangExpr> rightValue,
                            Optional<MolangExpr> leftSlope, Optional<MolangExpr> rightSlope) {
        public static final Codec<ChainNode> CODEC = RecordCodecBuilder.create(i -> i.group(
                MolangExpr.CODEC.optionalFieldOf("left_value").forGetter(ChainNode::leftValue),
                MolangExpr.CODEC.optionalFieldOf("right_value").forGetter(ChainNode::rightValue),
                MolangExpr.CODEC.optionalFieldOf("left_slope").forGetter(ChainNode::leftSlope),
                MolangExpr.CODEC.optionalFieldOf("right_slope").forGetter(ChainNode::rightSlope)
        ).apply(i, ChainNode::new));
    }

    public enum Type implements StringRepresentable {
        LINEAR("linear"),
        BEZIER("bezier"),
        BEZIER_CHAIN("bezier_chain"),
        CATMULL_ROM("catmull_rom");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
