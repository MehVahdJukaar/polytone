package net.mehvahdjukaar.polytone.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record VersionRange(int minExcl, int maxExcl) {
    public static final Codec<VersionRange> CODEC = Codec.STRING.flatXmap(VersionRange::decode,
            range -> DataResult.success(range.toString()));

    private static final Pattern RANGE_PATTERN = Pattern.compile("([\\[(])([0-9., ]*)([\\])])");

    public static DataResult<VersionRange> decode(String versionStr) {
        versionStr = versionStr.trim();
        if ("*".equals(versionStr)) {
            return DataResult.success(new VersionRange(-1, Integer.MAX_VALUE));
        }

        Matcher matcher = RANGE_PATTERN.matcher(versionStr);
        if (!matcher.matches()) {
            String finalVersionStr = versionStr;
            return DataResult.error(() -> "Invalid version range: " + finalVersionStr);
        }

        boolean minInclusive = matcher.group(1).equals("[");
        boolean maxInclusive = matcher.group(3).equals("]");
        String[] parts = matcher.group(2).split(",");

        int min = -1, max = Integer.MAX_VALUE;
        if (parts.length > 0 && !parts[0].isBlank()) {
            min = parseVersion(parts[0].trim());
            if (minInclusive) {
                min--;
            }
        }
        if (parts.length > 1 && !parts[1].isBlank()) {
            max = parseVersion(parts[1].trim());
            if (maxInclusive) {
                max++;
            }
        }

        return DataResult.success(new VersionRange(min, max));
    }

    public boolean matches(String version) {
        int ver = parseVersion(version);
        return ver > minExcl && ver < maxExcl;
    }

    @Override
    public String toString() {
        return "(" + minExcl + ", " + maxExcl + ")";
    }

    public static int parseVersion(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]) * 1_000_000;
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) * 1_000 : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return major + minor + patch;
    }

}