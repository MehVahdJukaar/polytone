package net.mehvahdjukaar.polytone;

import net.mehvahdjukaar.polytone.biome.BiomeEffectsManager;
import net.mehvahdjukaar.polytone.biome.BiomeIdMapperManager;
import net.mehvahdjukaar.polytone.block.BlockPropertiesManager;
import net.mehvahdjukaar.polytone.block.BlockSetManager;
import net.mehvahdjukaar.polytone.color.ColorManager;
import net.mehvahdjukaar.polytone.colormap.ColormapsManager;
import net.mehvahdjukaar.polytone.dimension.DimensionEffectsManager;
import net.mehvahdjukaar.polytone.fluid.FluidPropertiesManager;
import net.mehvahdjukaar.polytone.item.ItemModifier;
import net.mehvahdjukaar.polytone.item.ItemModifiersManager;
import net.mehvahdjukaar.polytone.lightmap.LightmapsManager;
import net.mehvahdjukaar.polytone.particle.CustomParticlesManager;
import net.mehvahdjukaar.polytone.particle.ParticleModifiersManager;
import net.mehvahdjukaar.polytone.slotify.GuiModifierManager;
import net.mehvahdjukaar.polytone.slotify.GuiOverlayManager;
import net.mehvahdjukaar.polytone.sound.SoundTypesManager;
import net.mehvahdjukaar.polytone.tabs.CreativeTabsModifiersManager;
import net.mehvahdjukaar.polytone.texture.VariantTextureManager;
import net.mehvahdjukaar.polytone.utils.BiomeKeysCache;
import net.mehvahdjukaar.polytone.utils.CompoundReloader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class Polytone {
    public static final String MOD_ID = "polytone";

    public static final Logger LOGGER = LogManager.getLogger("Polytone");

    public static final BlockPropertiesManager BLOCK_MODIFIERS = new BlockPropertiesManager();
    public static final FluidPropertiesManager FLUID_MODIFIERS = new FluidPropertiesManager();
    public static final ItemModifiersManager ITEM_MODIFIERS = new ItemModifiersManager();
    public static final BiomeEffectsManager BIOME_MODIFIERS = new BiomeEffectsManager();
    public static final ColormapsManager COLORMAPS = new ColormapsManager();
    public static final LightmapsManager LIGHTMAPS = new LightmapsManager();
    public static final BiomeIdMapperManager BIOME_ID_MAPPERS = new BiomeIdMapperManager();
    public static final DimensionEffectsManager DIMENSION_MODIFIERS = new DimensionEffectsManager();
    public static final CustomParticlesManager CUSTOM_PARTICLES = new CustomParticlesManager();
    public static final ParticleModifiersManager PARTICLE_MODIFIERS = new ParticleModifiersManager();
    public static final SoundTypesManager SOUND_TYPES = new SoundTypesManager();
    public static final VariantTextureManager VARIANT_TEXTURES = new VariantTextureManager();
    public static final ColorManager COLORS = new ColorManager();
    public static final GuiModifierManager SLOTIFY = new GuiModifierManager();
    public static final GuiOverlayManager OVERLAY_MODIFIERS = new GuiOverlayManager();
    public static final BlockSetManager BLOCK_SET = new BlockSetManager();
    public static final CreativeTabsModifiersManager CREATIVE_TABS_MODIFIERS = new CreativeTabsModifiersManager();

    public static boolean sodiumOn = false;
    public static boolean isDevEnv = false;
    public static boolean isForge = false;

    public static void init(boolean isSodiumOn, boolean devEnv, boolean forge) {
        PlatStuff.addClientReloadListener(() -> new CompoundReloader(
                        SOUND_TYPES, CUSTOM_PARTICLES, BIOME_ID_MAPPERS, COLORMAPS, COLORS,
                        BLOCK_SET, BLOCK_MODIFIERS, FLUID_MODIFIERS, ITEM_MODIFIERS,
                        BIOME_MODIFIERS, VARIANT_TEXTURES, LIGHTMAPS, DIMENSION_MODIFIERS,
                        PARTICLE_MODIFIERS, SLOTIFY, OVERLAY_MODIFIERS,
                        CREATIVE_TABS_MODIFIERS),
                res("polytone_stuff"));
        sodiumOn = isSodiumOn;
        isDevEnv = devEnv;
        isForge = forge;
        //TODO: rename effects, modifiers and properties to a common standard naming scheme
        //TODO: colormap for particles
        //SKY and fog
        //block particles
        //item properties and color. cache pllayer coord
        //exp color
        //durability bar color
        //biome lightmap
        // per blockstate offset like block models

        if (isDevEnv) {// force all mixins to load in dev
            MixinEnvironment.getCurrentEnvironment().audit();
        }
    }

    public static ResourceLocation res(String name) {
        return new ResourceLocation(MOD_ID, name);
    }

    public static void onTagsReceived(RegistryAccess registryAccess) {
        BIOME_MODIFIERS.doApply(registryAccess, true);
        DIMENSION_MODIFIERS.doApply(registryAccess, true);
        BiomeKeysCache.clear();
    }

    public static void onLevelUnload() {
        BiomeKeysCache.clear();
    }


}
