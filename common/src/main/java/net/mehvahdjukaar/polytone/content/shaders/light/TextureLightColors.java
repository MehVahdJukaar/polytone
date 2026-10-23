package net.mehvahdjukaar.polytone.content.shaders.light;

import com.google.common.base.Stopwatch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FastColor;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

public class TextureLightColors {

    public static Map<Block, Integer> detect(ResourceManager manager) {
        Stopwatch watch = Stopwatch.createStarted();

        Map<Block, Integer> colors = new IdentityHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState state = block.defaultBlockState();
            if (state.getLightEmission() <= 0) continue;
            try {
                Integer color = guessLightColor(manager, block);
                if (color != null) colors.put(block, color);
            } catch (Exception e) {
                Polytone.LOGGER.error("Light color task failed for {}", block, e);
            }
        }

        Polytone.LOGGER.info("Generated {} texture light colors in {}", colors.size(), watch);
        return colors;
    }

    @Nullable
    private static Integer guessLightColor(ResourceManager manager, Block block) throws Exception {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
        Optional<Resource> blockState = manager.getResource(blockId.withPath(p -> "blockstates/" + p + ".json"));
        if (blockState.isEmpty()) return null;

        JsonObject bsElement;
        try (var bsStream = blockState.get().open()) {
            bsElement = deserializeJson(bsStream);
        }
        String modelPath = findDefaultStateModel(bsElement, block.defaultBlockState());
        if (modelPath == null) return null;


        for (String t : findAllTexturesInModelRecursive(manager, modelPath)) {
            //references like #all, already got the real one
            if (t.startsWith("#")) continue;
            ResourceLocation texture = ResourceLocation.parse(t);
            Optional<Resource> res = manager.getResource(texture.withPath(p -> "textures/" + p + ".png"));
            if (res.isEmpty()) continue;
            int avg;
            try (InputStream stream = res.get().open(); NativeImage image = NativeImage.read(stream)) {
                avg = averageColor(image);
            }
            if (avg == -1) continue;
            return avg & 0xFFFFFF;
        }
        return null;
    }

    @Nullable
    private static String findDefaultStateModel(JsonObject bsElement, BlockState state) {
        if (bsElement.has("variants")) {
            JsonObject variants = bsElement.getAsJsonObject("variants");
            Set<String> props = new HashSet<>(List.of(BlockModelShaper.statePropertiesToString(state.getValues()).split(",")));
            for (Map.Entry<String, JsonElement> e : variants.entrySet()) {
                boolean allPropsMatch = e.getKey().isEmpty() || props.containsAll(List.of(e.getKey().split(",")));
                if (allPropsMatch) {
                    var models = findAllResourcesInJsonRecursive(e.getValue(), s -> s.equals("model"));
                    if (!models.isEmpty()) return models.iterator().next();
                }
            }
        }
        //multipart, whatever. grabs the first resource location of a model
        var models = findAllResourcesInJsonRecursive(bsElement, s -> s.equals("model"));
        return models.isEmpty() ? null : models.iterator().next();
    }

    //bright pixels count more, otherwise a torch is just the stick
    private static int averageColor(NativeImage image) {
        //could be faster. whatever
        double r = 0, g = 0, b = 0, weight = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                int a = FastColor.ABGR32.alpha(abgr);
                if (a < 128) continue;
                int pr = FastColor.ABGR32.red(abgr);
                int pg = FastColor.ABGR32.green(abgr);
                int pb = FastColor.ABGR32.blue(abgr);
                double w = 0.05 + (0.2126 * pr + 0.7152 * pg + 0.0722 * pb) / 255d;
                w *= w;
                r += pr * w;
                g += pg * w;
                b += pb * w;
                weight += w;
            }
        }
        if (weight == 0) return -1;
        return FastColor.ARGB32.color(255, (int) (r / weight), (int) (g / weight), (int) (b / weight));
    }


    //remember to close this stream
    public static JsonObject deserializeJson(InputStream stream) {
        return GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    @NotNull
    private static List<String> findAllTexturesInModelRecursive(ResourceManager manager, String modelPath) throws Exception {
        JsonObject modelElement;
        ResourceLocation modelId = ResourceLocation.parse(modelPath);
        try (var modelStream = manager.getResource(modelId.withPath(p -> "models/" + p + ".json")).get().open()) {
            modelElement = deserializeJson(modelStream).getAsJsonObject();
        } catch (Exception e) {
            throw new Exception("Failed to parse model at " + modelPath);
        }
        var textures = new ArrayList<>(findAllResourcesInJsonRecursive(modelElement.getAsJsonObject("textures")));
        if (textures.isEmpty()) {
            if (modelElement.has("parent")) {
                var parentPath = modelElement.get("parent").getAsString();
                textures.addAll(findAllTexturesInModelRecursive(manager, parentPath));
            }
        }
        return textures;
    }

    public static Set<String> findAllResourcesInJsonRecursive(JsonElement element) {
        return findAllResourcesInJsonRecursive(element, s -> true);
    }

    //same as moonlight
    private static Set<String> findAllResourcesInJsonRecursive(JsonElement element, Predicate<String> filter) {
        switch (element) {
            case null -> {
                return Set.of();
            }
            case JsonArray array -> {
                Set<String> list = new LinkedHashSet<>();

                array.forEach(e -> list.addAll(findAllResourcesInJsonRecursive(e, filter)));
                return list;
            }
            case JsonObject json -> {
                var entries = json.entrySet();

                Set<String> list = new LinkedHashSet<>();
                for (var c : entries) {
                    if (c.getValue().isJsonPrimitive() && !filter.test(c.getKey())) continue;
                    var l = findAllResourcesInJsonRecursive(c.getValue(), filter);

                    list.addAll(l);
                }
                return list;
            }
            default -> {
                return Set.of(element.getAsString());
            }
        }
    }
}
