package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.content.shaders.post.PostProgramImports;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//nonsense regex magic that will inject our ight sampling inside all shaders that we deem need it. could have false positives
public class LightShaderPatcher {

    private static final Pattern TERRAIN_LIGHTMAP_READ = Pattern.compile("minecraft_sample_lightmap\\s*\\(\\s*Sampler2\\s*,\\s*UV2\\s*\\)");
    private static final Pattern ENTITY_LIGHTMAP_READ = Pattern.compile("texelFetch\\s*\\(\\s*Sampler2\\s*,\\s*UV2\\s*/\\s*16\\s*,\\s*0\\s*\\)");
    private static final Pattern DECLARES_POSITION = Pattern.compile("\\bin\\s+vec3\\s+Position\\s*;");
    private static final Pattern DECLARES_NORMAL = Pattern.compile("\\bin\\s+vec3\\s+Normal\\s*;");
    private static final Pattern DECLARES_CHUNK_OFFSET = Pattern.compile("\\buniform\\s+vec3\\s+ChunkOffset\\s*;");
    private static final Pattern VERSION_LINE = Pattern.compile("^\\s*#version[^\\n]*\\n", Pattern.MULTILINE);

    private static final Pattern SODIUM_LIGHTMAP_READ = Pattern.compile("texture\\s*\\(\\s*u_LightTex\\s*,\\s*_vert_tex_light_coord\\s*\\)");
    private static final Pattern SODIUM_DECLARES_POSITION = Pattern.compile("\\bvec3\\s+position\\s*=");
    //we steal last 3 bits of sodium material byte to store the direction of the face. Since sodium kills normals
    public static final int SODIUM_MATERIAL_DIRECTION_BITS_SHIFT = 5;

    private static final String INCLUDE_IMPORT_LINE = "#moj_import <polytone_block_light.glsl>\n";
    private static final ResourceLocation INCLUDE_LOCATION = ResourceLocation.withDefaultNamespace("shaders/include/polytone_block_light.glsl");

    public static boolean isIncludeAvailable() {
        return Minecraft.getInstance().getResourceManager()
                .getResource(INCLUDE_LOCATION).isPresent();
    }

    public static String patch(String vertexShaderText) {
        boolean readsLightmap = TERRAIN_LIGHTMAP_READ.matcher(vertexShaderText).find() || ENTITY_LIGHTMAP_READ.matcher(vertexShaderText).find();
        if (!readsLightmap || !DECLARES_POSITION.matcher(vertexShaderText).find()){
            return vertexShaderText;
        }
        Matcher versionLine = VERSION_LINE.matcher(vertexShaderText);
        if (!versionLine.find()) return vertexShaderText;

        String cameraRelativePos = DECLARES_CHUNK_OFFSET.matcher(vertexShaderText).find() ? "Position + ChunkOffset" : "Position";
        String normal = DECLARES_NORMAL.matcher(vertexShaderText).find() ? "Normal" : "vec3(0.0)";

        String patched = TERRAIN_LIGHTMAP_READ.matcher(vertexShaderText).replaceAll(m -> Matcher.quoteReplacement(
                wrapWithColoredLight(m.group(), "vec2(UV2) / 16.0", "0.0", cameraRelativePos, normal)));
        patched = ENTITY_LIGHTMAP_READ.matcher(patched).replaceAll(m -> Matcher.quoteReplacement(
                wrapWithColoredLight(m.group(), "vec2(UV2 / 16)", "0.5", cameraRelativePos, normal)));

        int afterVersion = versionLine.end();
        return patched.substring(0, afterVersion) + INCLUDE_IMPORT_LINE + patched.substring(afterVersion);
    }

    //texel shift matchs how the vanilla read samples: terrain between texels, entities at their centers
    private static String wrapWithColoredLight(String vanillaRead, String lightLevels, String texelShift, String pos, String normal) {
        return "polyColoredLightmap(" + vanillaRead + ", Sampler2, " + lightLevels + ", " + texelShift + ", " + pos + ", " + normal + ")";
    }

    //sodium #import only looks in its own jat and doesnt know #moj_import. paste ours in expanded
    public static String patchSodiumChunkShader(String vertexShaderText) {
        boolean readsLightmap = SODIUM_LIGHTMAP_READ.matcher(vertexShaderText).find();
        if (!readsLightmap || !SODIUM_DECLARES_POSITION.matcher(vertexShaderText).find()) {
            return vertexShaderText;
        }
        Matcher versionLine = VERSION_LINE.matcher(vertexShaderText);
        if (!versionLine.find()) return vertexShaderText;
        String include = String.join("", new PostProgramImports().process(INCLUDE_IMPORT_LINE));

        //_vert_tex_light_coord is UV2 / 256
        String face = "((_material_params >> " + SODIUM_MATERIAL_DIRECTION_BITS_SHIFT + "u) & 7u)";
        String patched = SODIUM_LIGHTMAP_READ.matcher(vertexShaderText).replaceAll(m -> Matcher.quoteReplacement(
                "polyColoredLightmapForFace(" + m.group() + ", u_LightTex, _vert_tex_light_coord * 16.0, 0.0, position, " + face + ")"));

        int afterVersion = versionLine.end();
        return patched.substring(0, afterVersion) + include + "\n" + patched.substring(afterVersion);
    }
}
