package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//nonsense regex magic that will inject our ight sampling inside all shaders that we deem need it. could have false positives
public class PolytoneShaderPatcher {

    private static final Pattern LIGHTMAP_READ = Pattern.compile("\\bsample_lightmap\\s*\\(\\s*Sampler2\\s*,\\s*UV2\\s*\\)");
    //every vanilla vsh hands its camera relative position to the fog, pos in terrain and block, Position elsewhere
    private static final Pattern FOG_DISTANCE_ARG = Pattern.compile("\\bfog_spherical_distance\\s*\\(\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\)");
    private static final Pattern DECLARES_NORMAL = Pattern.compile("\\bin\\s+vec3\\s+Normal\\s*;");
    private static final Pattern DECLARES_UV2 = Pattern.compile("\\bin\\s+ivec2\\s+UV2\\s*;");
    private static final Pattern UV2_USE = Pattern.compile("\\bUV2\\b");
    private static final Pattern VERSION_LINE = Pattern.compile("^\\s*#version[^\\n]*\\n", Pattern.MULTILINE);

    private static final Pattern SODIUM_LIGHTMAP_READ = Pattern.compile("texture\\s*\\(\\s*u_LightTex\\s*,\\s*_vert_tex_light_coord\\s*\\)");
    private static final Pattern SODIUM_DECLARES_POSITION = Pattern.compile("\\bvec3\\s+position\\s*=");
    //we steal last 3 bits of sodium material byte to store the direction of the face. Since sodium kills normals
    public static final int SODIUM_MATERIAL_DIRECTION_BITS_SHIFT = 5;
    //vanilla BLOCK has no normal either. block light coords only go to 240 so bits 8-10 are free
    public static final int LIGHT_DIRECTION_BITS_SHIFT = 8;
    private static final String RAW_UV2 = "@POLY_RAW_UV2@";

    // set while shaders load, read when chunks mesh
    private static volatile boolean terrainReadsFaceBits = false;
    private static volatile boolean blockReadsFaceBits = false;

    private static final String INCLUDE_IMPORT_LINE = "#moj_import <polytone_colored_light.glsl>\n";
    public static final Identifier INCLUDE_LOCATION = Identifier.withDefaultNamespace("shaders/include/polytone_colored_light.glsl");
    private static final Identifier TERRAIN = Identifier.withDefaultNamespace("core/terrain");
    private static final Identifier BLOCK = Identifier.withDefaultNamespace("core/block");
    private static final Set<Identifier> SOLID_FACE_SHADERS = Set.of(TERRAIN, BLOCK);

    public static boolean isIncludeAvailable() {
        return Minecraft.getInstance().getResourceManager().getResource(INCLUDE_LOCATION).isPresent();
    }

    //face = Direction ordinal + 1, 0 = dont know. shared by the sodium and vanilla meshes
    public static int faceBits(Direction direction) {
        return direction.ordinal() + 1;
    }

    //both, any block mesh can end up drawn by either
    public static boolean canStoreFaceInLight() {
        return terrainReadsFaceBits && blockReadsFaceBits && !CompatHandler.irisShaderPackActive();
    }

    public static String onLoadVertexShader(Identifier shaderId, String vertexShaderText, boolean canPatch) {
        String patched = canPatch ? patch(shaderId, vertexShaderText) : vertexShaderText;
        boolean readsFaceBits = patched.contains("_polyColoredLightmapForFace(");
        if (shaderId.equals(TERRAIN)) terrainReadsFaceBits = readsFaceBits;
        else if (shaderId.equals(BLOCK)) blockReadsFaceBits = readsFaceBits;
        return patched;
    }

    private static String patch(Identifier shaderId, String vertexShaderText) {
        if (!LIGHTMAP_READ.matcher(vertexShaderText).find()) return vertexShaderText;
        Matcher fogDistance = FOG_DISTANCE_ARG.matcher(vertexShaderText);
        if (!fogDistance.find()) return vertexShaderText;
        Matcher versionLine = VERSION_LINE.matcher(vertexShaderText);
        if (!versionLine.find()) return vertexShaderText;

        String cameraRelativePos = fogDistance.group(1);
        boolean hasNormal = DECLARES_NORMAL.matcher(vertexShaderText).find();
        boolean isSolidFace = !hasNormal && SOLID_FACE_SHADERS.contains(shaderId);
        //sample_lightmap reads texel centers
        String levels = "vec2(UV2) / 16.0";
        String patched;
        Matcher uv2Declaration = DECLARES_UV2.matcher(vertexShaderText);
        if (isSolidFace && uv2Declaration.find()) {
            //face bits live in the light coords so every read past the declaration gets them masked off
            String face = "uint((" + RAW_UV2 + ".x >> " + LIGHT_DIRECTION_BITS_SHIFT + ") & 7)";
            String body = LIGHTMAP_READ.matcher(vertexShaderText.substring(uv2Declaration.end())).replaceAll(m -> Matcher.quoteReplacement(
                    "_polyColoredLightmapForFace(" + m.group() + ", Sampler2, " + levels + ", 0.5, " + cameraRelativePos + ", " + face + ")"));
            body = UV2_USE.matcher(body).replaceAll("ivec2(UV2.x & 0xFF, UV2.y)").replace(RAW_UV2, "UV2");
            patched = vertexShaderText.substring(0, uv2Declaration.end()) + body;
        } else {
            patched = LIGHTMAP_READ.matcher(vertexShaderText).replaceAll(m -> Matcher.quoteReplacement(isSolidFace ?
                    "_polyColoredLightmapBrightestSide(" + m.group() + ", Sampler2, " + levels + ", 0.5, " + cameraRelativePos + ")" :
                    wrapWithColoredLight(m.group(), levels, "0.5", cameraRelativePos, hasNormal ? "Normal" : "vec3(0.0)")));
        }

        int afterVersion = versionLine.end();
        return patched.substring(0, afterVersion) + INCLUDE_IMPORT_LINE + patched.substring(afterVersion);
    }

    private static String wrapWithColoredLight(String vanillaRead, String lightLevels, String texelShift, String pos, String normal) {
        return "_polyColoredLightmap(" + vanillaRead + ", Sampler2, " + lightLevels + ", " + texelShift + ", " + pos + ", " + normal + ")";
    }

    //sodium #import only looks in its own jat and doesnt know #moj_import. paste ours in expanded
    public static String patchSodiumChunkShader(String vertexShaderText) {
        boolean readsLightmap = SODIUM_LIGHTMAP_READ.matcher(vertexShaderText).find();
        if (!readsLightmap || !SODIUM_DECLARES_POSITION.matcher(vertexShaderText).find()) {
            return vertexShaderText;
        }
        Matcher versionLine = VERSION_LINE.matcher(vertexShaderText);
        if (!versionLine.find()) return vertexShaderText;
        String include = String.join("", new GlslImports().process(INCLUDE_IMPORT_LINE));

        //_vert_tex_light_coord is UV2 / 256
        String face = "((_material_params >> " + SODIUM_MATERIAL_DIRECTION_BITS_SHIFT + "u) & 7u)";
        String patched = SODIUM_LIGHTMAP_READ.matcher(vertexShaderText).replaceAll(m -> Matcher.quoteReplacement(
                "_polyColoredLightmapForFace(" + m.group() + ", u_LightTex, _vert_tex_light_coord * 16.0, 0.0, position, " + face + ")"));

        int afterVersion = versionLine.end();
        return patched.substring(0, afterVersion) + include + "\n" + patched.substring(afterVersion);
    }
}
