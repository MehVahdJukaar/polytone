package net.mehvahdjukaar.polytone.platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//Credits to fabric api
/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ClientTagsLoader {
    /**
     * Load a given tag from the available mods into a set of {@code Identifier}s.
     * Parsing based on {@link net.minecraft.tags.TagLoader#load(net.minecraft.server.packs.resources.ResourceManager)}
     */
    public static LoadedTag loadTag(TagKey<?> tagKey) {
        var tags = new HashSet<TagEntry>();
        HashSet<Path> tagFiles = getTagFiles(tagKey.registry(), tagKey.location());

        for (Path tagPath : tagFiles) {
            try (BufferedReader tagReader = Files.newBufferedReader(tagPath)) {
                JsonElement jsonElement = JsonParser.parseReader(tagReader);
                TagFile maybeTagFile = TagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, jsonElement))
                        .result().orElse(null);

                if (maybeTagFile != null) {
                    if (maybeTagFile.replace()) {
                        tags.clear();
                    }

                    tags.addAll(maybeTagFile.entries());
                }
            } catch (IOException e) {
                Polytone.LOGGER.error("Error loading tag: {}", tagKey, e);
            }
        }

        HashSet<ResourceLocation> completeIds = new HashSet<>();
        HashSet<ResourceLocation> immediateChildIds = new HashSet<>();
        HashSet<TagKey<?>> immediateChildTags = new HashSet<>();

        for (TagEntry tagEntry : tags) {
            tagEntry.build(new TagEntry.Lookup<>() {
                @Nullable
                @Override
                public ResourceLocation element(ResourceLocation id) {
                    immediateChildIds.add(id);
                    return id;
                }

                @Nullable
                @Override
                public Collection<ResourceLocation> tag(ResourceLocation id) {
                    TagKey<?> tag = TagKey.create(tagKey.registry(), id);
                    immediateChildTags.add(tag);
                    return ClientTagsImpl.getOrCreatePartiallySyncedTag(tag).completeIds;
                }
            }, completeIds::add);
        }

        // Ensure that the tag does not refer to itself
        immediateChildTags.remove(tagKey);

        return new LoadedTag(Collections.unmodifiableSet(completeIds), Collections.unmodifiableSet(immediateChildTags),
                Collections.unmodifiableSet(immediateChildIds));
    }

    public record LoadedTag(Set<ResourceLocation> completeIds, Set<TagKey<?>> immediateChildTags,
                            Set<ResourceLocation> immediateChildIds) {
    }

    /**
     * @param registryKey the RegistryKey of the TagKey
     * @param identifier  the Identifier of the tag
     * @return the paths to all tag json files within the available mods
     */
    private static HashSet<Path> getTagFiles(ResourceKey<? extends Registry<?>> registryKey, ResourceLocation identifier) {
        return getTagFiles(Registries.tagsDirPath(registryKey), identifier);
    }

    /**
     * @return the paths to all tag json files within the available mods
     */
    private static HashSet<Path> getTagFiles(String tagType, ResourceLocation identifier) {
        String tagFile = "data/%s/%s/%s.json".formatted(identifier.getNamespace(), tagType, identifier.getPath());
        return getResourcePaths(tagFile);
    }


    /**
     * @return all paths from the available mods that match the given internal path
     */
    private static HashSet<Path> getResourcePaths(String path) {
        HashSet<Path> out = new HashSet<>();

        for (var mod : ModList.get().getSortedMods()) {
            out.add(mod.getModInfo().getOwningFile().getFile().getFilePath().resolve(path));
        }

        return out;
    }
}
