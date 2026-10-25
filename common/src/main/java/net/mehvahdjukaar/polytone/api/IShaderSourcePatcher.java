package net.mehvahdjukaar.polytone.api;

//gets every shader source before it compiles. return it untouched when its not yours
public interface IShaderSourcePatcher {

    String patchShaderSource(String source);

    //sodium chunk programs skip GlslPreprocessor and only know their own #import
    default String patchSodiumChunkShaderSource(String source) {
        return source;
    }
}
