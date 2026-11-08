package net.mehvahdjukaar.polytone.compat;

import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.light.data.PointLightData;
import foundry.veil.api.client.render.light.renderer.LightRenderHandle;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.IncludeOverloadStrategy;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.MinecraftContext;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslNode;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.function.GlslFunctionNode;
import io.github.ocelot.glslprocessor.api.node.function.GlslInvokeFunctionNode;
import io.github.ocelot.glslprocessor.api.node.variable.GlslVariableNode;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;
import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import net.mehvahdjukaar.polytone.content.shaders.PolytoneShaderPatcher;
import net.mehvahdjukaar.polytone.content.shaders.light.ColoredLightsManager;
import net.mehvahdjukaar.polytone.content.shaders.light.PointLightStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VeilCompat {

    public static void init() {
        addShaderPreProcessorLast(new ColoredLightmapProcessor());
    }

    //must be run after every other mods processor like sable
    @PlatformImpl
    public static void addShaderPreProcessorLast(ShaderPreProcessor processor) {
        throw new AssertionError();
    }

    public static PointLightStorage createPointLightStorage() {
        return new VeilPointLightStorage();
    }

    //same rewrite as PolytoneShaderPatcher.patch but on parsed tree so whatever uv call ended up with is kept
    private static class ColoredLightmapProcessor implements ShaderPreProcessor {
        @Override
        public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
            if (!(ctx instanceof MinecraftContext) || !ctx.isVertex() || !ctx.isSourceFile()){
                return;
            }
            if (tree.field("Position").isEmpty() || !ColoredLightsManager.canPatchShaders()) {
                return;
            }
            GlslFunctionNode main = tree.mainFunction().orElse(null);
            if (main == null) {
                return;
            }

            List<GlslInvokeFunctionNode> lightmapReads = new ArrayList<>();
            for (GlslNode node : main.stream().toList()) {
                if (node instanceof GlslInvokeFunctionNode call && (isTerrainRead(call) || isEntityRead(call))) {
                    lightmapReads.add(call);
                }
            }
            if (lightmapReads.isEmpty()) return;

            String pos = tree.field("ChunkOffset").isPresent() ? "Position + ChunkOffset" : "Position";
            String normal = tree.field("Normal").isPresent() ? "Normal" : "vec3(0.0)";

            for (GlslInvokeFunctionNode call : lightmapReads) {
                boolean terrain = isTerrainRead(call);
                String uv = call.getParameters().get(1).toSourceString();
                String wrapped = PolytoneShaderPatcher.wrapWithColoredLight(call.toSourceString(),
                        terrain ? "vec2(" + uv + ") / 16.0" : "vec2(" + uv + ")", terrain ? "0.0" : "0.5", pos, normal);
                GlslInvokeFunctionNode replacement = (GlslInvokeFunctionNode) GlslParser.parseExpression(wrapped);
                call.setHeader(replacement.getHeader());
                call.getParameters().clear();
                call.getParameters().addAll(replacement.getParameters());
            }
            GlslTree include = GlslParser.preprocessParse(PolytoneShaderPatcher.expandedInclude(), new HashMap<>());
            ctx.include(tree, "polytone_colored_light", include, IncludeOverloadStrategy.SOURCE);
        }

        private static boolean isTerrainRead(GlslInvokeFunctionNode call) {
            return isCall(call, "minecraft_sample_lightmap", 2);
        }

        private static boolean isEntityRead(GlslInvokeFunctionNode call) {
            return isCall(call, "texelFetch", 3);
        }
        private static boolean isCall(GlslInvokeFunctionNode call, String name, int paramCount) {
            List<GlslNode> params = call.getParameters();
            return call.getHeader() instanceof GlslVariableNode header && header.getName().equals(name) &&
                    params.size() == paramCount && params.getFirst() instanceof GlslVariableNode sampler && sampler.getName().equals("Sampler2");
        }
    }



    private static class VeilPointLightStorage implements PointLightStorage {

        private final Map<Object, LightRenderHandle<PointLightData>> handles = new HashMap<>();

        @Override
        public void set(Object key, double x, double y, double z, ResolvedPointLight light) {
            LightRenderHandle<PointLightData> handle = handles.get(key);
            if (handle != null && handle.isValid()) {
                configure(handle.getLightData(), x, y, z, light);
            } else {
                PointLightData data = new PointLightData();
                configure(data, x, y, z, light);
                handles.put(key, VeilRenderSystem.renderer().getLightRenderer().addLight(data));
            }
        }

        @Override
        public void move(Object key, double x, double y, double z) {
            LightRenderHandle<PointLightData> handle = handles.get(key);
            if (handle != null && handle.isValid()) handle.getLightData().setPosition(x, y, z);
        }

        @Override
        public void remove(Object key) {
            LightRenderHandle<PointLightData> handle = handles.remove(key);
            if (handle != null && handle.isValid()) {
                handle.free();
            }
        }

        @Override
        public void clear() {
            for (var handle : handles.values()) {
                if (handle.isValid()) handle.free();
            }
            handles.clear();
        }

        private static void configure(PointLightData data, double x, double y, double z, ResolvedPointLight light) {
            data.setPosition(x, y, z)
                    .setRadius(light.radius())
                    .setBrightness(light.brightness())
                    .setColor(light.color());
        }
    }
}
