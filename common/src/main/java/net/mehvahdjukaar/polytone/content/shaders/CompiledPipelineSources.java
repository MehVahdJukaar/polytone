package net.mehvahdjukaar.polytone.content.shaders;

import com.google.common.collect.MapMaker;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.jspecify.annotations.Nullable;

import java.util.Map;

// Render passes only see compiled pipelines; this maps each one back to its source pipeline
public final class CompiledPipelineSources {

    private static final Map<CompiledRenderPipeline, RenderPipeline> SOURCES = new MapMaker().weakKeys().makeMap();

    public static void record(CompiledRenderPipeline compiled, RenderPipeline source) {
        SOURCES.put(compiled, source);
    }

    public static @Nullable RenderPipeline get(CompiledRenderPipeline compiled) {
        return SOURCES.get(compiled);
    }
}
