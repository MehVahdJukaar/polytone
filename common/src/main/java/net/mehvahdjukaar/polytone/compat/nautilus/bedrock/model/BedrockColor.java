package net.mehvahdjukaar.polytone.compat.nautilus.bedrock.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.compat.nautilus.bedrock.molang.MolangExpr;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// particle_appearance_tinting color, written as an rgb(a) array, an #AARRGGBB string or a gradient
public sealed interface BedrockColor {

    // gradient stops are these too, and packs mix both spellings freely
    Codec<BedrockColor> SOLID = Codec.withAlternative(
            BedrockCodecs.branch(Rgba.CODEC, Rgba.class),
            BedrockCodecs.branch(Hex.CODEC, Hex.class));

    Codec<BedrockColor> CODEC = Codec.withAlternative(SOLID, BedrockCodecs.branch(Gradient.CODEC, Gradient.class));

    record Rgba(MolangExpr r, MolangExpr g, MolangExpr b, MolangExpr a) implements BedrockColor {

        // alpha is optional: plenty of packs write [r, g, b]
        public static final Codec<Rgba> CODEC = MolangExpr.CODEC.listOf().comapFlatMap(
                list -> switch (list.size()) {
                    case 3 -> DataResult.success(new Rgba(list.get(0), list.get(1), list.get(2), MolangExpr.ONE));
                    case 4 -> DataResult.success(new Rgba(list.get(0), list.get(1), list.get(2), list.get(3)));
                    default -> DataResult.error(() -> "Expected 3 or 4 colour components, got " + list.size());
                },
                c -> List.of(c.r, c.g, c.b, c.a));
    }

    record Hex(String value) implements BedrockColor {
        public static final Codec<Hex> CODEC = Codec.STRING.xmap(Hex::new, Hex::value);

        // Bedrock hex is #AARRGGBB: alpha first, and 8 digits overflow a signed int
        public @Nullable Rgba toRgba() {
            String digits = value.replace("#", "").replace("0x", "").trim();
            long packed;
            try {
                packed = Long.parseLong(digits, 16);
            } catch (NumberFormatException e) {
                return null;
            }
            return switch (digits.length()) {
                case 6 -> new Rgba(channel(packed >> 16), channel(packed >> 8), channel(packed), MolangExpr.ONE);
                case 8 -> new Rgba(channel(packed >> 16), channel(packed >> 8), channel(packed), channel(packed >> 24));
                default -> null;
            };
        }

        private static MolangExpr channel(long shifted) {
            return MolangExpr.of((shifted & 0xFF) / 255.0);
        }
    }

    // stops are keyed by position along the interpolant; a bare list spreads them evenly from 0 to 1
    record Gradient(List<Stop> stops, Optional<MolangExpr> interpolant) implements BedrockColor {

        private static final Codec<List<Stop>> KEYED_STOPS = Codec.unboundedMap(Codec.STRING, SOLID)
                .comapFlatMap(Gradient::fromKeyed, Gradient::toKeyed);

        private static final Codec<List<Stop>> EVEN_STOPS = SOLID.listOf()
                .xmap(Gradient::spreadEvenly, Gradient::colorsOf);

        public static final Codec<Gradient> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.withAlternative(KEYED_STOPS, EVEN_STOPS).fieldOf("gradient").forGetter(Gradient::stops),
                MolangExpr.CODEC.optionalFieldOf("interpolant").forGetter(Gradient::interpolant)
        ).apply(i, Gradient::new));

        private static DataResult<List<Stop>> fromKeyed(Map<String, BedrockColor> map) {
            List<Stop> stops = new ArrayList<>();
            for (var entry : map.entrySet()) {
                try {
                    stops.add(new Stop(Double.parseDouble(entry.getKey()), entry.getValue()));
                } catch (NumberFormatException e) {
                    return DataResult.error(() -> "Gradient stop key '" + entry.getKey() + "' is not a number");
                }
            }
            stops.sort(Comparator.comparingDouble(Stop::position));
            return DataResult.success(stops);
        }

        private static Map<String, BedrockColor> toKeyed(List<Stop> stops) {
            return stops.stream().collect(Collectors.toMap(
                    s -> String.valueOf(s.position), Stop::color, (a, b) -> a, LinkedHashMap::new));
        }

        private static List<Stop> spreadEvenly(List<BedrockColor> colors) {
            List<Stop> stops = new ArrayList<>(colors.size());
            for (int i = 0; i < colors.size(); i++) {
                double position = colors.size() == 1 ? 0 : (double) i / (colors.size() - 1);
                stops.add(new Stop(position, colors.get(i)));
            }
            return stops;
        }

        private static List<BedrockColor> colorsOf(List<Stop> stops) {
            return stops.stream().map(Stop::color).toList();
        }

        public record Stop(double position, BedrockColor color) {
        }
    }
}
