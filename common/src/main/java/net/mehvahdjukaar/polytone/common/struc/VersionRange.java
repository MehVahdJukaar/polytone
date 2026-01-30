package net.mehvahdjukaar.polytone.common.struc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.server.packs.OverlayMetadataSection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record VersionRange(int min, int max, boolean minInclusive, boolean maxInclusive) {

    public static final Codec<VersionRange> CODEC = Codec.STRING.flatXmap(
            VersionRange::decode,
            range -> DataResult.success(range.toString())
    );

    private static final Pattern RANGE_PATTERN = Pattern.compile("([\\[(])([0-9., ]*)([\\])])");

    public static VersionRange any() {
        return new VersionRange(-1, Integer.MAX_VALUE, true, true);
    }

    public static DataResult<VersionRange> decode(String versionStr) {
        versionStr = versionStr.trim();

        // Wildcard "*"
        if ("*".equals(versionStr)) {
            return DataResult.success(any());
        }

        // Single version, treat as [v, v]
        if (versionStr.matches("\\d+(\\.\\d+){0,2}")) {
            int ver = parseVersion(versionStr);
            return DataResult.success(new VersionRange(ver, ver, true, true));
        }

        Matcher matcher = RANGE_PATTERN.matcher(versionStr);
        if (!matcher.matches()) {
            String finalVersionStr = versionStr;
            return DataResult.error(() -> "Invalid version range: " + finalVersionStr);
        }

        boolean minInclusive = matcher.group(1).equals("[");
        boolean maxInclusive = matcher.group(3).equals("]");

        String[] parts = matcher.group(2).split(",");
        int min = parts.length > 0 && !parts[0].isBlank() ? parseVersion(parts[0].trim()) : -1;
        int max = parts.length > 1 && !parts[1].isBlank() ? parseVersion(parts[1].trim()) : Integer.MAX_VALUE;

        return DataResult.success(new VersionRange(min, max, minInclusive, maxInclusive));
    }

    public boolean matches(String version) {
        int ver = parseVersion(version);
        boolean lower = minInclusive ? ver >= min : ver > min;
        boolean upper = maxInclusive ? ver <= max : ver < max;
        return lower && upper;
    }

    @Override
    public String toString() {
        String open = minInclusive ? "[" : "(";
        String close = maxInclusive ? "]" : ")";
        return open + min + ", " + max + close;
    }

    public static int parseVersion(String version) {
        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]) * 1_000_000;
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) * 1_000 : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return major + minor + patch;
    }
}
