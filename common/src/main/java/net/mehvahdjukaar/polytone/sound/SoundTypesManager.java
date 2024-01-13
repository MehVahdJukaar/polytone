package net.mehvahdjukaar.polytone.sound;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.PlatStuff;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.utils.JsonPartialReloader;
import net.mehvahdjukaar.polytone.utils.PartialReloader;
import net.mehvahdjukaar.polytone.utils.ReferenceOrDirectCodec;
import net.mehvahdjukaar.polytone.utils.StrOpt;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener.scanDirectory;

public class SoundTypesManager extends PartialReloader<SoundTypesManager.Resources> {

    private final Map<ResourceLocation, SoundEvent> customSoundEvents = new HashMap<>();

    // custom defined sound types
    private final BiMap<ResourceLocation, SoundType> soundTypesIds = HashBiMap.create();

    public SoundTypesManager(){
        super("sound_types");
    }

    @Nullable
    public SoundType getCustom(ResourceLocation id) {
        return soundTypesIds.get(id);
    }

    @Nullable
    public ResourceLocation getCustomKey(SoundType object) {
        return soundTypesIds.inverse().get(object);
    }

    @Override
    protected Resources prepare(ResourceManager resourceManager) {
        Map<ResourceLocation, JsonElement> jsons = new HashMap<>();
        scanDirectory(resourceManager, path(), GSON, jsons);

        var types = gatherSoundEvents(resourceManager, Polytone.MOD_ID);

        return new Resources(jsons, types);
    }

    @Override
    public void process(Resources resources) {

        var soundJsons = resources.soundTypes;
        var soundEvents = resources.soundEvents;

        //custom sound events

        List<ResourceLocation> ids = new ArrayList<>();
        for (var e : soundEvents.entrySet()) {
            for (var s : e.getValue()) {
                ResourceLocation id = e.getKey().withPath(s);
                if (!customSoundEvents.containsKey(id) && !BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
                    var event = PlatStuff.registerSoundEvent(id);
                    ids.add(id);
                    customSoundEvents.put(id, event);
                }
            }
        }
        if (!ids.isEmpty()) {
            Polytone.LOGGER.info("Registered {} custom Sound Events from Resource Packs: {}", ids.size(), ids + ". Remember to add them to sounds.json!");
            //this is bad
            Minecraft.getInstance().getSoundManager().reload();
            //this entire thing is a bad idea
        }

        // sound types

        for (var j : soundJsons.entrySet()) {
            var json = j.getValue();
            var id = j.getKey();
            SoundType soundType = SoundTypesManager.DIRECT_CODEC.decode(JsonOps.INSTANCE, json)
                    .getOrThrow(false, errorMsg -> Polytone.LOGGER.warn("Could not decode Sound Type with json id {} - error: {}",
                            id, errorMsg)).getFirst();

            soundTypesIds.put(id, soundType);
        }
    }

    @Override
    protected void reset() {
        soundTypesIds.clear();
        customSoundEvents.clear();
    }

    public static Map<ResourceLocation, List<String>> gatherSoundEvents(ResourceManager resourceManager, String path) {
        Map<ResourceLocation, List<String>> idList = new HashMap<>();
        Map<ResourceLocation, List<Resource>> res = resourceManager.listResourceStacks(path, resourceLocation ->
                resourceLocation.getPath().endsWith("sound_events.csv"));
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
                    Polytone.LOGGER.error("Couldn't parse Custom Sound Events file {}:", e.getKey(), ex);
                }
            }
        }
        return idList;
    }


    private static final Map<String, SoundType> SOUND_NAMES = Util.make(() -> {
        Map<String, SoundType> map = new HashMap<>();
        map.put("empty", SoundType.EMPTY);
        map.put("wood", SoundType.WOOD);
        map.put("gravel", SoundType.GRAVEL);
        map.put("grass", SoundType.GRASS);
        map.put("lily_pad", SoundType.LILY_PAD);
        map.put("stone", SoundType.STONE);
        map.put("metal", SoundType.METAL);
        map.put("glass", SoundType.GLASS);
        map.put("wool", SoundType.WOOL);
        map.put("sand", SoundType.SAND);
        map.put("snow", SoundType.SNOW);
        map.put("powder_snow", SoundType.POWDER_SNOW);
        map.put("ladder", SoundType.LADDER);
        map.put("anvil", SoundType.ANVIL);
        map.put("slime_block", SoundType.SLIME_BLOCK);
        map.put("honey_block", SoundType.HONEY_BLOCK);
        map.put("wet_grass", SoundType.WET_GRASS);
        map.put("coral_block", SoundType.CORAL_BLOCK);
        map.put("bamboo", SoundType.BAMBOO);
        map.put("bamboo_sapling", SoundType.BAMBOO_SAPLING);
        map.put("scaffolding", SoundType.SCAFFOLDING);
        map.put("sweet_berry_bush", SoundType.SWEET_BERRY_BUSH);
        map.put("crop", SoundType.CROP);
        map.put("hard_crop", SoundType.HARD_CROP);
        map.put("vine", SoundType.VINE);
        map.put("nether_wart", SoundType.NETHER_WART);
        map.put("lantern", SoundType.LANTERN);
        map.put("stem", SoundType.STEM);
        map.put("nylium", SoundType.NYLIUM);
        map.put("fungus", SoundType.FUNGUS);
        map.put("roots", SoundType.ROOTS);
        map.put("shroomlight", SoundType.SHROOMLIGHT);
        map.put("weeping_vines", SoundType.WEEPING_VINES);
        map.put("twisting_vines", SoundType.TWISTING_VINES);
        map.put("soul_sand", SoundType.SOUL_SAND);
        map.put("soul_soil", SoundType.SOUL_SOIL);
        map.put("basalt", SoundType.BASALT);
        map.put("wart_block", SoundType.WART_BLOCK);
        map.put("netherrack", SoundType.NETHERRACK);
        map.put("nether_bricks", SoundType.NETHER_BRICKS);
        map.put("nether_sprouts", SoundType.NETHER_SPROUTS);
        map.put("nether_ore", SoundType.NETHER_ORE);
        map.put("bone_block", SoundType.BONE_BLOCK);
        map.put("netherite_block", SoundType.NETHERITE_BLOCK);
        map.put("ancient_debris", SoundType.ANCIENT_DEBRIS);
        map.put("lodestone", SoundType.LODESTONE);
        map.put("chain", SoundType.CHAIN);
        map.put("nether_gold_ore", SoundType.NETHER_GOLD_ORE);
        map.put("gilded_blackstone", SoundType.GILDED_BLACKSTONE);
        map.put("candle", SoundType.CANDLE);
        map.put("amethyst", SoundType.AMETHYST);
        map.put("amethyst_cluster", SoundType.AMETHYST_CLUSTER);
        map.put("small_amethyst_bud", SoundType.SMALL_AMETHYST_BUD);
        map.put("medium_amethyst_bud", SoundType.MEDIUM_AMETHYST_BUD);
        map.put("large_amethyst_bud", SoundType.LARGE_AMETHYST_BUD);
        map.put("tuff", SoundType.TUFF);
        map.put("calcite", SoundType.CALCITE);
        map.put("dripstone_block", SoundType.DRIPSTONE_BLOCK);
        map.put("pointed_dripstone", SoundType.POINTED_DRIPSTONE);
        map.put("copper", SoundType.COPPER);
        map.put("cave_vines", SoundType.CAVE_VINES);
        map.put("spore_blossom", SoundType.SPORE_BLOSSOM);
        map.put("azalea", SoundType.AZALEA);
        map.put("flowering_azalea", SoundType.FLOWERING_AZALEA);
        map.put("moss_carpet", SoundType.MOSS_CARPET);
        map.put("pink_petals", SoundType.PINK_PETALS);
        map.put("moss", SoundType.MOSS);
        map.put("big_dripleaf", SoundType.BIG_DRIPLEAF);
        map.put("small_dripleaf", SoundType.SMALL_DRIPLEAF);
        map.put("rooted_dirt", SoundType.ROOTED_DIRT);
        map.put("hanging_roots", SoundType.HANGING_ROOTS);
        map.put("azalea_leaves", SoundType.AZALEA_LEAVES);
        map.put("sculk_sensor", SoundType.SCULK_SENSOR);
        map.put("sculk_catalyst", SoundType.SCULK_CATALYST);
        map.put("sculk", SoundType.SCULK);
        map.put("sculk_vein", SoundType.SCULK_VEIN);
        map.put("sculk_shrieker", SoundType.SCULK_SHRIEKER);
        map.put("glow_lichen", SoundType.GLOW_LICHEN);
        map.put("deepslate", SoundType.DEEPSLATE);
        map.put("deepslate_bricks", SoundType.DEEPSLATE_BRICKS);
        map.put("deepslate_tiles", SoundType.DEEPSLATE_TILES);
        map.put("polished_deepslate", SoundType.POLISHED_DEEPSLATE);
        map.put("froglight", SoundType.FROGLIGHT);
        map.put("frogspawn", SoundType.FROGSPAWN);
        map.put("muddy_mangrove_roots", SoundType.MUDDY_MANGROVE_ROOTS);
        map.put("mud", SoundType.MUD);
        map.put("mud_bricks", SoundType.MUD_BRICKS);
        map.put("packed_mud", SoundType.PACKED_MUD);
        map.put("hanging_sign", SoundType.HANGING_SIGN);
        map.put("nether_wood_hanging_sign", SoundType.NETHER_WOOD_HANGING_SIGN);
        map.put("bamboo_wood_hanging_sign", SoundType.BAMBOO_WOOD_HANGING_SIGN);
        map.put("bamboo_wood", SoundType.BAMBOO_WOOD);
        map.put("nether_wood", SoundType.NETHER_WOOD);
        map.put("cherry_wood", SoundType.CHERRY_WOOD);
        map.put("cherry_sapling", SoundType.CHERRY_SAPLING);
        map.put("cherry_leaves", SoundType.CHERRY_LEAVES);
        map.put("cherry_wood_hanging_sign", SoundType.CHERRY_WOOD_HANGING_SIGN);
        map.put("chiseled_bookshelf", SoundType.CHISELED_BOOKSHELF);
        map.put("suspicious_sand", SoundType.SUSPICIOUS_SAND);
        map.put("suspicious_gravel", SoundType.SUSPICIOUS_GRAVEL);
        map.put("decorated_pot", SoundType.DECORATED_POT);
        map.put("decorated_pot_cracked", SoundType.DECORATED_POT_CRACKED);
        return map;
    });

    private static final Codec<SoundType> SOUND_TYPE_BLOCK_COPY = BuiltInRegistries.BLOCK.byNameCodec()
            .xmap(block -> block.getSoundType(block.defaultBlockState()), soundType1 -> Blocks.AIR);

    //reference or copy. hacky stuff only decodes
    public static final Codec<SoundType> REFERENCE_OR_COPY_CODEC = Codec.STRING.flatXmap(s -> {
                if (s.startsWith("copy(")) {
                    String target = s.replace("copy(", "").replace(")", "");
                    ResourceLocation r = ResourceLocation.tryParse(target);
                    if (r == null) {
                        return DataResult.error(() -> "Invalid string for Sound Type Copy function: " + s + ". Expected 'copy([some_mod]:[some_block])'");
                    }
                    var block = BuiltInRegistries.BLOCK.getOptional(r);
                    if (block.isEmpty()) return DataResult.error(() -> "No block with id '" + r + "' found", SoundType.EMPTY);
                    Block b = block.get();
                    return DataResult.success(b.getSoundType(b.defaultBlockState()));
                }
                var vanilla = SOUND_NAMES.get(s);
                if (vanilla != null) return DataResult.success(vanilla);
                ResourceLocation r = ResourceLocation.tryParse(s);
                if (r != null) {
                    var custom = Polytone.SOUND_TYPES. getCustom(new ResourceLocation(s));
                    if (custom != null) return DataResult.success(custom);
                }
                return DataResult.error(() -> "Could not find any custom Sound Type with id " + r +
                        " Did you place it in 'assets/[your pack]/polytone/sound_types/' ?");
            },
            t -> DataResult.error(() -> "Encoding SoundTypes not supported"));

    public static final Codec<SoundType> DIRECT_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    StrOpt.of(Codec.FLOAT, "volume", 1f).forGetter(SoundType::getVolume),
                    StrOpt.of(Codec.FLOAT, "pitch", 1f).forGetter(SoundType::getPitch),
                    BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("break_sound").forGetter(SoundType::getBreakSound),
                    BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("step_sound").forGetter(SoundType::getStepSound),
                    BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("place_sound").forGetter(SoundType::getPlaceSound),
                    BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("hit_sound").forGetter(SoundType::getHitSound),
                    BuiltInRegistries.SOUND_EVENT.byNameCodec().fieldOf("fall_sound").forGetter(SoundType::getFallSound)
            ).apply(instance, SoundType::new));

    public static final Codec<SoundType> CODEC = new ReferenceOrDirectCodec<>(REFERENCE_OR_COPY_CODEC, DIRECT_CODEC);


    public record Resources(Map<ResourceLocation,JsonElement> soundTypes,
                                   Map<ResourceLocation, List<String>> soundEvents){};

}
