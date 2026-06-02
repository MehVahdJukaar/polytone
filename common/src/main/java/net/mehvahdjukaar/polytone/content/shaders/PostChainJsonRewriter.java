package net.mehvahdjukaar.polytone.content.shaders;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

/**
 * Best-effort translation of the post-effect JSON format used by Minecraft 1.21.2+ (and the
 * polytone 1.21.11 branch) into the format the 1.21.1 {@link net.minecraft.client.renderer.PostChain}
 * expects.
 *
 * <p>Triggered from {@code PostChainMixin} only when {@link PostShadersManager#POLYTONE_LOADING}
 * is set, so vanilla post-effects loaded for other reasons are untouched.</p>
 *
 * <h2>Mapping</h2>
 * <ul>
 *   <li>{@code "targets": { "swap": {...} }} → {@code "targets": [ "swap" ]} (or object form if
 *       width/height are present)</li>
 *   <li>Per pass:
 *     <ul>
 *       <li>{@code "fragment_shader": "ns:path"} → {@code "name": "ns:path"} (vertex_shader is
 *           ignored — old PostChain always uses the screen-quad vertex shader)</li>
 *       <li>{@code "inputs"[0].target} → {@code "intarget"}</li>
 *       <li>Remaining {@code "inputs"} entries → {@code "auxtargets"} (using {@code sampler_name}
 *           as {@code name}; {@code use_depth_buffer:true} maps to {@code "id": "<target>:depth"})</li>
 *       <li>{@code "output"} → {@code "outtarget"}</li>
 *       <li>{@code "uniforms": { "UBO": [{name,type,value}, ...], ... }} → flat array of
 *           {@code {name, values}} entries (UBO grouping discarded; {@code type} ignored).</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class PostChainJsonRewriter {

    private PostChainJsonRewriter() {}

    /**
     * Old-format pass {@code name} is a program lookup, so the {@code post/} subdirectory used by
     * the new-format {@code fragment_shader} path is redundant — strip it (preserving any namespace).
     * e.g. {@code "sunbathing:post/godrays"} → {@code "sunbathing:godrays"}; {@code "post/blit"} →
     * {@code "blit"}. Inputs without a {@code post/} segment are returned unchanged.
     */
    static String stripPostPrefix(String fragmentShader) {
        int colon = fragmentShader.indexOf(':');
        String ns = colon >= 0 ? fragmentShader.substring(0, colon + 1) : "";
        String path = colon >= 0 ? fragmentShader.substring(colon + 1) : fragmentShader;
        if (path.startsWith("post/")) path = path.substring("post/".length());
        return ns + path;
    }

    /** True if this JSON appears to use the new format (object-typed {@code targets} or any pass with {@code fragment_shader}). */
    public static boolean isNewFormat(JsonObject root) {
        JsonElement targets = root.get("targets");
        if (targets != null && targets.isJsonObject()) return true;
        JsonElement passes = root.get("passes");
        if (passes != null && passes.isJsonArray()) {
            for (JsonElement p : passes.getAsJsonArray()) {
                if (p.isJsonObject() && p.getAsJsonObject().has("fragment_shader")) return true;
            }
        }
        return false;
    }

    /** Rewrite {@code root} in place into the old (1.21.1) format. */
    public static void rewrite(JsonObject root) {
        rewriteTargets(root);
        rewritePasses(root);
        int a =1;
    }

    private static void rewriteTargets(JsonObject root) {
        JsonElement targets = root.get("targets");
        if (targets == null || !targets.isJsonObject()) return;
        JsonArray out = new JsonArray();
        for (Map.Entry<String, JsonElement> e : targets.getAsJsonObject().entrySet()) {
            String name = e.getKey();
            JsonObject cfg = e.getValue().isJsonObject() ? e.getValue().getAsJsonObject() : new JsonObject();
            if (cfg.has("width") || cfg.has("height")) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", name);
                if (cfg.has("width")) obj.add("width", cfg.get("width"));
                if (cfg.has("height")) obj.add("height", cfg.get("height"));
                out.add(obj);
            } else {
                out.add(name);
            }
        }
        root.add("targets", out);
    }

    private static void rewritePasses(JsonObject root) {
        JsonElement passesEl = root.get("passes");
        if (passesEl == null || !passesEl.isJsonArray()) return;
        JsonArray newPasses = new JsonArray();
        for (JsonElement p : passesEl.getAsJsonArray()) {
            if (!p.isJsonObject()) {
                newPasses.add(p);
                continue;
            }
            newPasses.add(rewritePass(p.getAsJsonObject()));
        }
        root.add("passes", newPasses);
    }

    private static JsonObject rewritePass(JsonObject in) {
        if (!in.has("fragment_shader")) return in; // already old, or unknown — pass through

        JsonObject out = new JsonObject();
        out.addProperty("name", stripPostPrefix(in.get("fragment_shader").getAsString()));

        JsonArray inputs = in.has("inputs") && in.get("inputs").isJsonArray() ? in.getAsJsonArray("inputs") : new JsonArray();
        JsonArray auxtargets = new JsonArray();
        boolean primarySet = false;
        for (JsonElement e : inputs) {
            if (!e.isJsonObject()) continue;
            JsonObject inp = e.getAsJsonObject();
            String target = inp.has("target") ? inp.get("target").getAsString() : null;
            String sampler = inp.has("sampler_name") ? inp.get("sampler_name").getAsString() : null;
            boolean depth = inp.has("use_depth_buffer") && inp.get("use_depth_buffer").getAsBoolean();
            if (target == null) continue;
            if (!primarySet && !depth) {
                out.addProperty("intarget", target);
                primarySet = true;
            } else {
                JsonObject aux = new JsonObject();
                aux.addProperty("name", sampler != null ? sampler : target);
                aux.addProperty("id", depth ? target + ":depth" : target);
                auxtargets.add(aux);
            }
        }
        if (!primarySet) {
            // No non-depth input — fall back to first input as primary, even if depth
            for (JsonElement e : inputs) {
                if (!e.isJsonObject()) continue;
                JsonObject inp = e.getAsJsonObject();
                if (inp.has("target")) {
                    out.addProperty("intarget", inp.get("target").getAsString());
                    primarySet = true;
                    break;
                }
            }
            if (!primarySet) out.addProperty("intarget", "minecraft:main");
        }

        out.addProperty("outtarget", in.has("output") ? in.get("output").getAsString() : "minecraft:main");

        if (!auxtargets.isEmpty()) out.add("auxtargets", auxtargets);

        // Uniforms: { "UBO": [{name,type,value}, ...], ... } -> flat array of {name, values}
        JsonElement uniformsEl = in.get("uniforms");
        if (uniformsEl != null && uniformsEl.isJsonObject()) {
            JsonArray uniforms = new JsonArray();
            for (Map.Entry<String, JsonElement> ubo : uniformsEl.getAsJsonObject().entrySet()) {
                if (!ubo.getValue().isJsonArray()) continue;
                for (JsonElement u : ubo.getValue().getAsJsonArray()) {
                    if (!u.isJsonObject()) continue;
                    JsonObject src = u.getAsJsonObject();
                    if (!src.has("name") || !src.has("value")) continue;
                    JsonObject dst = new JsonObject();
                    dst.add("name", src.get("name"));
                    JsonElement v = src.get("value");
                    if (v.isJsonArray()) {
                        dst.add("values", v);
                    } else {
                        JsonArray vals = new JsonArray();
                        vals.add(v);
                        dst.add("values", vals);
                    }
                    uniforms.add(dst);
                }
            }
            if (!uniforms.isEmpty()) out.add("uniforms", uniforms);
        } else if (uniformsEl != null && uniformsEl.isJsonArray()) {
            // Already old form
            out.add("uniforms", uniformsEl);
        }

        if (in.has("use_linear_filter")) out.add("use_linear_filter", in.get("use_linear_filter"));

        return out;
    }
}
