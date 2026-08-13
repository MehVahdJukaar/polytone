package net.mehvahdjukaar.polytone.bedrock.convert;

import net.mehvahdjukaar.polytone.common.StrUtils;
import net.minecraft.core.Direction.Axis;

import java.util.OptionalDouble;

// The one place that knows our expression dialect. Everything is emitted in the scripting syntax (o.age(),
// random.rand(), named customs) rather than the older uppercase-variable one: it is the more capable of the
// two, and it is the only one that can hold the per-particle state Molang leans on so heavily.
public class PolytoneExpressions {

    public static final String AGE = "o.age()";

    public static String velocity(Axis axis) {
        return switch (axis) {
            case X -> "o.xd()";
            case Y -> "o.yd()";
            case Z -> "o.zd()";
        };
    }

    public static String custom(String name) {
        return "o.custom(\"" + name + "\")";
    }

    public static String constant(double value) {
        return StrUtils.compactNumber(value);
    }

    public static OptionalDouble asNumber(String expression) {
        try {
            return OptionalDouble.of(Double.parseDouble(expression.trim()));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    public static boolean isZero(String expression) {
        OptionalDouble number = asNumber(expression);
        return number.isPresent() && number.getAsDouble() == 0;
    }

    public static String scale(String expression, double factor) {
        if (factor == 1) return expression;
        OptionalDouble number = asNumber(expression);
        if (number.isPresent()) return constant(number.getAsDouble() * factor);
        if (factor == 0) return "0";
        return "(" + expression + ")*" + constant(factor);
    }

    public static String add(String left, String right) {
        if (isZero(left)) return right;
        if (isZero(right)) return left;
        OptionalDouble a = asNumber(left);
        OptionalDouble b = asNumber(right);
        if (a.isPresent() && b.isPresent()) return constant(a.getAsDouble() + b.getAsDouble());
        return left + "+" + parenthesize(right);
    }

    public static String multiply(String left, String right) {
        OptionalDouble a = asNumber(left);
        OptionalDouble b = asNumber(right);
        if (a.isPresent() && b.isPresent()) return constant(a.getAsDouble() * b.getAsDouble());
        if (a.isPresent()) return scale(right, a.getAsDouble());
        if (b.isPresent()) return scale(left, b.getAsDouble());
        return parenthesize(left) + "*" + parenthesize(right);
    }

    // Uniform in [-half, half], which is what a box emitter needs on each axis
    public static String randomSymmetric(String half) {
        if (isZero(half)) return "0";
        OptionalDouble constant = asNumber(half);
        if (constant.isPresent()) {
            return "random.rand(" + constant(-constant.getAsDouble()) + ", " + constant(constant.getAsDouble()) + ")";
        }
        return "(random.rand()*2-1)*" + parenthesize(half);
    }

    // A rough stand-in for "a random direction" on axes that would need a shared normalisation. Three
    // independent gaussians do give an isotropic direction, but each axis is sampled separately so the length
    // varies instead of staying on the unit sphere.
    public static String randomGaussian(String scale) {
        if (isZero(scale)) return "0";
        return "random.gaussian()*" + parenthesize(scale);
    }

    // v * dragMultiplier + accelerationPerTick, the per-tick form of Bedrock's dynamic motion
    public static String integrateVelocity(Axis axis, double dragMultiplier, String accelerationPerTick) {
        return add(scale(velocity(axis), dragMultiplier), accelerationPerTick);
    }

    // Spin as a closed form over age rather than an accumulator, so it cannot drift: start + rate * age.
    public static String spin(String startRadians, String radiansPerTick) {
        return add(startRadians, multiply(radiansPerTick, AGE));
    }

    // wraps anything that isn't a bare number: cheaper than tracking precedence, and the folding above
    // means most results are bare numbers anyway
    private static String parenthesize(String expression) {
        return asNumber(expression).isPresent() ? expression : "(" + expression + ")";
    }
}
