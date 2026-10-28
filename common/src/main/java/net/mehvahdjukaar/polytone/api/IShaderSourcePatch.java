package net.mehvahdjukaar.polytone.api;

import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.util.Map;

//gets every shader source before it compiles. return it untouched when its not yours
public interface IShaderSourcePatch {

    //check includes in files, at startup the resource manager can still be the vanilla pack alone
    String patchShaderSource(Identifier shaderId, ShaderType type, String source, Map<Identifier, Resource> files);

    //sodium chunk programs skip the vanilla preprocessor and only know their own #import. vertex and fragment both come through here
    default String patchSodiumChunkShaderSource(String source) {
        return source;
    }
}
