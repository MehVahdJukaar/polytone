package net.mehvahdjukaar.polytone.content.noise;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.jspecify.annotations.Nullable;

import java.util.List;

// Stack of 2D simplex noise octaves. Vanilla's SimplexNoise returns float, which gives the high octaves a different
// seed. This class keeps the double math, so packs get the same noise as the old PerlinSimplexNoise.
public class OctaveSimplexNoise {

    private final @Nullable Octave[] noiseLevels;
    private final double highestFreqValueFactor;
    private final double highestFreqInputFactor;

    public OctaveSimplexNoise(RandomSource random, List<Integer> octaveSet) {
        this(random, new IntRBTreeSet(octaveSet));
    }

    private OctaveSimplexNoise(RandomSource random, IntSortedSet octaveSet) {
        if (octaveSet.isEmpty()) {
            throw new IllegalArgumentException("Need some octaves!");
        }

        int lowFreqOctaves = -octaveSet.firstInt();
        int highFreqOctaves = octaveSet.lastInt();
        int octaves = lowFreqOctaves + highFreqOctaves + 1;
        if (octaves < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
        }

        Octave zeroOctave = new Octave(random);
        int zeroOctaveIndex = highFreqOctaves;
        this.noiseLevels = new Octave[octaves];
        if (zeroOctaveIndex >= 0 && zeroOctaveIndex < octaves && octaveSet.contains(0)) {
            this.noiseLevels[zeroOctaveIndex] = zeroOctave;
        }

        for (int i = zeroOctaveIndex + 1; i < octaves; i++) {
            if (i >= 0 && octaveSet.contains(zeroOctaveIndex - i)) {
                this.noiseLevels[i] = new Octave(random);
            } else {
                random.consumeCount(262);
            }
        }

        if (highFreqOctaves > 0) {
            long positiveOctaveSeed = (long) (zeroOctave.getValue(zeroOctave.xo, zeroOctave.yo, zeroOctave.zo) * 9.223372E18F);
            RandomSource highFreqRandom = new WorldgenRandom(new LegacyRandomSource(positiveOctaveSeed));

            for (int i = zeroOctaveIndex - 1; i >= 0; i--) {
                if (i < octaves && octaveSet.contains(zeroOctaveIndex - i)) {
                    this.noiseLevels[i] = new Octave(highFreqRandom);
                } else {
                    highFreqRandom.consumeCount(262);
                }
            }
        }

        this.highestFreqInputFactor = Math.pow(2.0, highFreqOctaves);
        this.highestFreqValueFactor = 1.0 / (Math.pow(2.0, octaves) - 1.0);
    }

    public double getValue(double x, double y, boolean useNoiseStart) {
        double value = 0.0;
        double factor = this.highestFreqInputFactor;
        double valueFactor = this.highestFreqValueFactor;

        for (Octave noiseLevel : this.noiseLevels) {
            if (noiseLevel != null) {
                value += noiseLevel.getValue(x * factor + (useNoiseStart ? noiseLevel.xo : 0.0),
                        y * factor + (useNoiseStart ? noiseLevel.yo : 0.0)) * valueFactor;
            }

            factor /= 2.0;
            valueFactor *= 2.0;
        }

        return value;
    }

    private static final class Octave {
        private static final int[][] GRADIENT = new int[][]{
                {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
                {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
                {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
                {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}
        };
        private static final double SQRT_3 = Math.sqrt(3.0);
        private static final double F2 = 0.5 * (SQRT_3 - 1.0);
        private static final double G2 = (3.0 - SQRT_3) / 6.0;

        private final int[] p = new int[512];
        private final double xo;
        private final double yo;
        private final double zo;

        private Octave(RandomSource random) {
            this.xo = random.nextDouble() * 256.0;
            this.yo = random.nextDouble() * 256.0;
            this.zo = random.nextDouble() * 256.0;
            for (int i = 0; i < 256; i++) {
                this.p[i] = i;
            }
            for (int i = 0; i < 256; i++) {
                int offset = random.nextInt(256 - i);
                int tmp = this.p[i];
                this.p[i] = this.p[offset + i];
                this.p[offset + i] = tmp;
            }
        }

        private int p(int x) {
            return this.p[x & 0xFF];
        }

        private static double cornerNoise(int index, double x, double y, double z, double base) {
            double t = base - x * x - y * y - z * z;
            if (t < 0.0) return 0.0;
            t *= t;
            int[] g = GRADIENT[index];
            return t * t * (g[0] * x + g[1] * y + g[2] * z);
        }

        private double getValue(double xin, double yin) {
            double s = (xin + yin) * F2;
            int i = Mth.floor(xin + s);
            int j = Mth.floor(yin + s);
            double t = (i + j) * G2;
            double x0 = xin - (i - t);
            double y0 = yin - (j - t);
            int i1 = x0 > y0 ? 1 : 0;
            int j1 = 1 - i1;

            double x1 = x0 - i1 + G2;
            double y1 = y0 - j1 + G2;
            double x2 = x0 - 1.0 + 2.0 * G2;
            double y2 = y0 - 1.0 + 2.0 * G2;
            int ii = i & 0xFF;
            int jj = j & 0xFF;
            int gi0 = this.p(ii + this.p(jj)) % 12;
            int gi1 = this.p(ii + i1 + this.p(jj + j1)) % 12;
            int gi2 = this.p(ii + 1 + this.p(jj + 1)) % 12;
            return 70.0 * (cornerNoise(gi0, x0, y0, 0.0, 0.5)
                    + cornerNoise(gi1, x1, y1, 0.0, 0.5)
                    + cornerNoise(gi2, x2, y2, 0.0, 0.5));
        }

        private double getValue(double xin, double yin, double zin) {
            double s = (xin + yin + zin) * 0.3333333333333333;
            int i = Mth.floor(xin + s);
            int j = Mth.floor(yin + s);
            int k = Mth.floor(zin + s);
            double t = (i + j + k) * 0.16666666666666666;
            double x0 = xin - (i - t);
            double y0 = yin - (j - t);
            double z0 = zin - (k - t);
            int i1, j1, k1, i2, j2, k2;
            if (x0 >= y0) {
                if (y0 >= z0) {
                    i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
                } else if (x0 >= z0) {
                    i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1;
                } else {
                    i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1;
                }
            } else if (y0 < z0) {
                i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1;
            } else if (x0 < z0) {
                i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1;
            } else {
                i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0;
            }

            double x1 = x0 - i1 + 0.16666666666666666;
            double y1 = y0 - j1 + 0.16666666666666666;
            double z1 = z0 - k1 + 0.16666666666666666;
            double x2 = x0 - i2 + 0.3333333333333333;
            double y2 = y0 - j2 + 0.3333333333333333;
            double z2 = z0 - k2 + 0.3333333333333333;
            double x3 = x0 - 1.0 + 0.5;
            double y3 = y0 - 1.0 + 0.5;
            double z3 = z0 - 1.0 + 0.5;
            int ii = i & 0xFF;
            int jj = j & 0xFF;
            int kk = k & 0xFF;
            int gi0 = this.p(ii + this.p(jj + this.p(kk))) % 12;
            int gi1 = this.p(ii + i1 + this.p(jj + j1 + this.p(kk + k1))) % 12;
            int gi2 = this.p(ii + i2 + this.p(jj + j2 + this.p(kk + k2))) % 12;
            int gi3 = this.p(ii + 1 + this.p(jj + 1 + this.p(kk + 1))) % 12;
            return 32.0 * (cornerNoise(gi0, x0, y0, z0, 0.6)
                    + cornerNoise(gi1, x1, y1, z1, 0.6)
                    + cornerNoise(gi2, x2, y2, z2, 0.6)
                    + cornerNoise(gi3, x3, y3, z3, 0.6));
        }
    }
}
