package net.mehvahdjukaar.polytone.utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.operator.Operator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ExpressionUtils {

    private static final RandomSource RANDOM_SOURCE = RandomSource.createNewThreadLocalInstance();

    private static final ThreadLocal<Long> LAST_SEED = new ThreadLocal<>();

    private static final Set<Function> NOISE_FUNCS = new HashSet<>();

    private static final Function RAND = new Function("rand", 0) {
        @Override
        public double apply(double... args) {
            RANDOM_SOURCE.setSeed(LAST_SEED.get());
            return RANDOM_SOURCE.nextDouble();
        }
    };

    private static final Function GAUSSIAN = new Function("gaussian", 0) {
        @Override
        public double apply(double... args) {
            RANDOM_SOURCE.setSeed(LAST_SEED.get());
            return RANDOM_SOURCE.nextGaussian();
        }
    };

    private static final Function COS = new Function("cos", 1) {
        @Override
        public double apply(double... args) {
            return Mth.cos((float) args[0]);
        }
    };

    private static final Function SIN = new Function("sin", 1) {
        @Override
        public double apply(double... args) {
            return Mth.sin((float) args[0]);
        }
    };

    private static final Function RED = new Function("red", 1) {
        @Override
        public double apply(double... args) {
            return ARGB.red((int) args[0]) / 255f;
        }
    };

    private static final Function GREEN = new Function("green", 1) {
        @Override
        public double apply(double... args) {
            return ARGB.green((int) args[0]) / 255f;
        }
    };

    private static final Function BLUE = new Function("blue", 1) {
        @Override
        public double apply(double... args) {
            return ARGB.blue((int) args[0]) / 255f;
        }
    };

    private static final Function ALPHA = new Function("alpha", 1) {
        @Override
        public double apply(double... args) {
            return ARGB.alpha((int) args[0]) / 255f;
        }
    };

    private static final Function COLOR = new Function("color", 4) {
        @Override
        public double apply(double... args) {
            return ARGB.color((int) (args[0] * 255f), (int) (args[1] * 255f), (int) (args[2] * 255f), (int) (args[3] * 255f));
        }
    };

    private static final Function ATAN2 = new Function("atan2", 2) {
        @Override
        public double apply(double... args) {
            return Mth.atan2((float) args[0], args[1]);
        }
    };


    private static final Function STEP = new Function("step", 2) {
        @Override
        public double apply(double... args) {
            return args[0] >= args[1] ? 1 : 0;
        }
    };

    private static final Function MAX = new Function("max", 2) {
        @Override
        public double apply(double... args) {
            return Math.max(args[0], args[1]);
        }
    };

    private static final Function MIN = new Function("min", 2) {
        @Override
        public double apply(double... args) {
            return Math.min(args[0], args[1]);
        }
    };

    private static final Function LERP = new Function("lerp", 3) {
        @Override
        public double apply(double... args) {
            return Mth.lerp(args[0], args[1], args[2]);
        }
    };

    public static final Function SMOOTHSTEP = new Function("smoothstep", 3) {
        @Override
        public double apply(double... args) {
            double t = Math.max(0, Math.min(1, (args[0] - args[1]) / (args[2] - args[1])));
            return t * t * (3 - 2 * t);
        }
    };

    private static final Operator EQUALS = new Operator("==", 2, true, Operator.PRECEDENCE_ADDITION - 1) {
        @Override
        public double apply(double[] values) {
            return values[0] == values[1] ? 1d : 0d;
        }
    };

    private static final Operator LESS_EQUAL = new Operator("<=", 2, true, Operator.PRECEDENCE_ADDITION - 2) {

        @Override
        public double apply(double[] values) {
            if (values[0] <= values[1]) {
                return 1d;
            } else {
                return 0d;
            }
        }
    };

    private static final Operator GREATER_EQUAL = new Operator(">=", 2, true, Operator.PRECEDENCE_ADDITION - 3) {

        @Override
        public double apply(double[] values) {
            if (values[0] >= values[1]) {
                return 1d;
            } else {
                return 0d;
            }
        }
    };

    private static final Operator LESS = new Operator("<", 2, true, Operator.PRECEDENCE_ADDITION - 4) {
        @Override
        public double apply(double[] values) {
            return values[0] < values[1] ? 1d : 0d;
        }
    };

    private static final Operator GREATER = new Operator(">", 2, true, Operator.PRECEDENCE_ADDITION - 5) {
        @Override
        public double apply(double[] values) {
            return values[0] > values[1] ? 1d : 0d;
        }
    };

    private static final Operator FACTORIAL = new Operator("!", 1, true, Operator.PRECEDENCE_POWER + 1) {

        @Override
        public double apply(double... args) {
            final int arg = (int) args[0];
            if (arg != args[0]) {
                throw new IllegalArgumentException("Operand for factorial has to be an integer");
            }
            if (arg < 0) {
                throw new IllegalArgumentException("The operand of the factorial can not be less than zero");
            }
            double result = 1;
            for (int i = 1; i <= arg; i++) {
                result *= i;
            }
            return result;
        }
    };


    public static Function[] defFunc(Function... others) {
        List<Function> list = new ArrayList<>();
        list.addAll(Arrays.asList(others));
        list.addAll(NOISE_FUNCS);
        list.addAll(List.of(COS, SIN, ATAN2, RAND, GAUSSIAN, STEP, SMOOTHSTEP, MAX, MIN, LERP, RED, GREEN, BLUE, ALPHA, COLOR));
        return list.toArray(new Function[0]);
    }

    public static void regenNoiseFunctions(Set<Map.Entry<ResourceLocation, PerlinSimplexNoise>> noises) {
        NOISE_FUNCS.clear();
        for (var e : noises) {
            ResourceLocation res = e.getKey();
            PerlinSimplexNoise noise = e.getValue();
            String key = "noise_" + res.getNamespace() + "_" + res.getPath();
            NOISE_FUNCS.add(new Function(key, 2) {
                @Override
                public double apply(double... args) {
                    return noise.getValue(args[0], args[1], false);
                }
            });
            if (res.getNamespace().equals("minecraft")) {
                key = key.replace("minecraft_", "");
                NOISE_FUNCS.add(new Function(key, 2) {
                    @Override
                    public double apply(double... args) {
                        return noise.getValue(args[0], args[1], false);
                    }
                });
            }
        }
        PerlinSimplexNoise baseNoise = new PerlinSimplexNoise(RandomSource.create(0), List.of(1));
        NOISE_FUNCS.add(new Function("noise", 2) {
            @Override
            public double apply(double... args) {
                return baseNoise.getValue(args[0], args[1], false);
            }
        });

    }


    public static Operator[] defOp(Operator... others) {
        return Stream.concat(
                Stream.of(EQUALS, GREATER, LESS, GREATER_EQUAL, LESS_EQUAL, FACTORIAL),
                Stream.of(others)
        ).toArray(Operator[]::new);
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("(?:#|0x)[0-9a-fA-F]+");

    public static String removeHex(String s) {
        // Create a Matcher object
        Matcher matcher = HEX_PATTERN.matcher(s);

        // StringBuffer to build the modified expression
        StringBuilder sb = new StringBuilder();

        // Iterate through the matches and replace each with its decimal equivalent
        while (matcher.find()) {
            String hexString = matcher.group().replace("#", "").replace("0x", ""); // Remove the prefix
            long decimalValue = Long.parseLong(hexString, 16);
            matcher.appendReplacement(sb, Long.toString(decimalValue));
        }

        // Append the remaining part of the original expression
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static void seedRandom(long seed) {
        LAST_SEED.set(seed);
    }

    public static void randomizeRandom() {
        seedRandom(SECONDARY.nextLong());
    }

    private static final RandomSource SECONDARY = RandomSource.createNewThreadLocalInstance();


}
