package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

//same <ns:path> rules ShaderManager uses, for sources vanilla never loads (compute, sodium)
public class GlslImports extends GlslPreprocessor {

    private final Set<Identifier> imported = new HashSet<>();

    @Override
    public @Nullable String applyImport(boolean isRelative, String path) {
        if (isRelative) {
            Polytone.LOGGER.error("Relative GLSL import \"{}\" isn't supported here, use <namespace:path>", path);
            return "#error relative import " + path;
        }
        Identifier location = Identifier.parse(path).withPrefix("shaders/include/");
        if (!imported.add(location)) return null;
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(location)) {
            return IOUtils.toString(reader);
        } catch (Exception e) {
            Polytone.LOGGER.error("Could not open GLSL import {}: {}", location, e.getMessage());
            return "#error " + e.getMessage();
        }
    }
}
