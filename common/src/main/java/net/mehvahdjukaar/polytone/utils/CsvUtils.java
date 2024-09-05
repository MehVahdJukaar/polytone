package net.mehvahdjukaar.polytone.utils;

import com.google.gson.JsonParseException;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvUtils {


    public static Map<ResourceLocation, List<String>> parseCsv(ResourceManager resourceManager, String path) {
        Map<ResourceLocation, List<String>> idList = new HashMap<>();
        Map<ResourceLocation, List<Resource>> res = resourceManager.listResourceStacks(Polytone.MOD_ID, resourceLocation ->
                resourceLocation.getPath().endsWith(path+".csv"));
        for (var e : res.entrySet()) {
            for (var r : e.getValue()) {
                try (Reader reader = r.openAsReader()) {
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    List<String> lines = bufferedReader.lines()
                            .map(line -> line.split(",")) // Splitting by comma
                            .flatMap(Arrays::stream)
                            .map(String::trim)
                            .filter(v -> ResourceLocation.tryParse(v) != null && !v.isEmpty())// Removing extra spaces
                            .toList();
                    if (!lines.isEmpty()) idList.put(e.getKey(), lines);
                } catch (IllegalArgumentException | IOException | JsonParseException ex) {
                    Polytone.LOGGER.error("Couldn't parse CSV file file {}:", e.getKey(), ex);
                }
            }
        }
        return idList;
    }
}
