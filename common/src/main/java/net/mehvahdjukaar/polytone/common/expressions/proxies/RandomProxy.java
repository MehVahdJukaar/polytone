package net.mehvahdjukaar.polytone.common.expressions.proxies;

import net.mehvahdjukaar.candlelight.api.BeanAliases;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.noise.NoiseManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

@BeanAliases
public class RandomProxy {

    public static final RandomProxy GLOBAL = RandomProxy.of(RandomSource.createThreadLocalInstance());

    private final RandomSource source;

    public static RandomProxy posSeeded(BlockPos pos) {
        return new RandomProxy(RandomSource.create(pos.asLong()));
    }

    public static RandomProxy seeded(long seed) {
        return new RandomProxy(RandomSource.create(seed));
    }

    public static RandomProxy createRandom() {
        return new RandomProxy(RandomSource.create());
    }

    public static RandomProxy of(RandomSource source) {
        return new RandomProxy(source);
    }

    RandomProxy(RandomSource source) {
        this.source = source;
    }

    public int randInt() {
        return source.nextInt();
    }

    public int randInt(int bound) {
        return source.nextInt(bound);
    }

    public int randInt(int origin, int bound) {
        return source.nextInt(origin, bound);
    }

    public double rand() {
        return source.nextDouble();
    }

    public double rand(double origin, double bound) {
        return source.nextDouble() * (bound - origin) + origin;
    }

    public double rand(double bound) {
        return source.nextDouble() * bound;
    }

    public double gaussian() {
        return source.nextGaussian();
    }

    public double gaussian(double mean, double deviation) {
        return source.nextGaussian() * deviation + mean;
    }

    public double noise(String name, double x, double y) {
        PerlinSimplexNoise n = Polytone.NOISES.getNoise(name);
        if (n == null) return 0;
        return n.getValue(x, y, true);
    }

    public double noise(double x, double y) {
        PerlinSimplexNoise n = NoiseManager.DEFAULT;
        return n.getValue(x, y, true);
    }
}
