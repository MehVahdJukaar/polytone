package net.mehvahdjukaar.polytone.content.shaders.post;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

//hack: same #moj_import rules ShaderInstance uses for core shaders
public class PostProgramImports extends GlslPreprocessor {

    private static final String PROGRAM_DIR = "shaders/program/";

    private final Set<String> importedPaths = new HashSet<>();

    @Override
    public @Nullable String applyImport(boolean useFullPath, String directory) {
        String path = FileUtil.normalizeResourcePath((useFullPath ? PROGRAM_DIR : "shaders/include/") + directory);
        if (!importedPaths.add(path)) return null;
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(ResourceLocation.parse(path))) {
            return IOUtils.toString(reader);
        } catch (Exception e) {
            Polytone.LOGGER.error("Could not open GLSL import {} for a post shader: {}", path, e.getMessage());
            return "#error " + e.getMessage();
        }
    }
}
