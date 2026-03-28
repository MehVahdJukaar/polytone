package net.mehvahdjukaar.polytone.content.color;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.ColorUtils;
import net.mehvahdjukaar.polytone.common.exp.impl.EntityContextExpression;
import net.mehvahdjukaar.polytone.common.expressions.impl.IEntityExp;
import net.mehvahdjukaar.polytone.common.reloader.SingleJsonOrPropertiesReloadListener;
import net.mehvahdjukaar.polytone.common.struc.Vec3f;
import net.mehvahdjukaar.polytone.content.color.fog_env.FogEnvironmentMod;
import net.mehvahdjukaar.polytone.mixins.accessor.DustParticleOptionAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.ColorLerper;
import net.minecraft.client.renderer.entity.state.ExperienceOrbRenderState;
import net.minecraft.client.renderer.fog.environment.PowderedSnowFogEnvironment;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ARGB;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.border.BorderStatus;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static net.minecraft.world.effect.MobEffect.AMBIENT_ALPHA;

public class ColorManager extends SingleJsonOrPropertiesReloadListener {

    private static final int DEFAULT_COLOR = ARGB.colorFromFloat(1.0f, 1, 0, 0);
    private final Object2IntMap<MapColor> vanillaMapColors = new Object2IntOpenHashMap<>();
    private final Map<DyeColor, Integer> vanillaFireworkColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> vanillaDiffuseColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> vanillaTextColors = new EnumMap<>(DyeColor.class);
    private final Map<ChatFormatting, Integer> vanillaChatFormatting = new EnumMap<>(ChatFormatting.class);
    private final Object2IntMap<MobEffect> vanillaEffectColors = new Object2IntOpenHashMap<>();
    private final Map<MobEffect, Function<MobEffectInstance, ParticleOptions>> vanillaEffectParticles = new HashMap<>();
    private final EnumMap<BorderStatus, Integer> vanillaBorderStatus = new EnumMap<>(BorderStatus.class);

    private final Map<DyeColor, Integer> customSheepColors = new EnumMap<>(DyeColor.class);
    private final Map<DyeColor, Integer> originalSheepColors = new EnumMap<>(ColorLerper.Type.SHEEP.colorByDye);

    protected final int[] originalRedstoneWireColors = Arrays.copyOf(RedStoneWireBlock.COLORS, RedStoneWireBlock.COLORS.length);

    protected final Style originalSplash = SplashManager.DEFAULT_STYLE;

    protected final int originalPowderSnowColor = PowderedSnowFogEnvironment.COLOR;
    @Nullable
    protected FogEnvironmentMod powderSnowFogMod = null;
    @Nullable
    protected FogEnvironmentMod lavaFogMod = null;
    //TODO: add this

    @Nullable
    Identifier xpOrbParticle;
    @Nullable
    private IEntityExp xpOrbColor;
    @Nullable
    private IEntityExp xpOrbColorR;
    @Nullable
    private IEntityExp xpOrbColorG;
    @Nullable
    private IEntityExp xpOrbColorB;

    private Integer xpBar = null;
    private Integer xpBarBack = null;
    private Integer enchantTableXp = null;

    private Integer skyFlashColor = null;
    private Integer voidDarknessOffset = null;
    private Integer horizonHeight = null;

    @Nullable
    private Integer fishingLineColor = null;
    @Nullable
    private Vec3f fishingLineOffset = null;

    //don't use pls
    @Nullable
    private Integer swampDark = null;
    @Nullable
    private Integer swampLight = null;
    @Nullable
    private Integer darkForest = null;

    public ColorManager() {
        //determines the priority. last applied will be the one with highest priority. Polytone is last applied one
        super("colo_manager",
                "color.properties", "colors.json",
                Polytone.MOD_ID, "colormatic", "vanadium", "optifine");
    }

    public Integer getSkyFlash() {
        return skyFlashColor;
    }

    public Integer getVoidDarknessOffset() {
        return voidDarknessOffset;
    }

    public Integer getHorizonHeight() {
        return horizonHeight;
    }

    public Integer getXpBar() {
        return xpBar;
    }

    public Integer getXpBarBackground() {
        return xpBarBack;
    }

    //TODO: figure out a way to get that mixin to work
    public @Nullable Integer getSpecialSwampDark() {
        return swampDark;
    }

    public @Nullable Integer getSpecialSwampLight() {
        return swampLight;
    }

    public @Nullable Integer getSpecialDarkForest() {
        return darkForest;
    }

    @Override
    protected void parseWithLevel(Map<Identifier, JsonElement> jsons, RegistryOps<JsonElement> ops, HolderLookup.Provider access) {
        var keySet = new ArrayList<>(jsons.keySet());

        for (var k : Lists.reverse(keySet)) {
            JsonElement root = jsons.get(k);
            try {
                parseColorJson(root, k);
            } catch (Exception e1) {
                Polytone.LOGGER.error("Failed to parse color.json in file {}", k, e1);
            }
        }

        regenSheepColors();
    }

    private void parseColorJson(JsonElement root, Identifier fileId) {
        JsonObject obj = root.getAsJsonObject();

        doWith(obj, "map", (k, v) -> {
            MapColor color = MapColorHelper.byName(k);
            if (color != null) {
                int col = parseColor(v);
                if (!vanillaMapColors.containsKey(color)) {
                    vanillaMapColors.put(color, color.col);
                }
                color.col = col;
            } else Polytone.LOGGER.warn("Unknown MapColor with name {}", k);
        });

        doWith(obj, "environment", (k, e) -> {
            switch (k) {
                case "sky_flash":
                    skyFlashColor = parseColor(e);
                case "void_darkness_offset":
                    voidDarknessOffset = e.getAsInt();
                case "horizon_height":
                    horizonHeight = e.getAsInt();
            }
        });

        doWith(obj, "fog", (ka, va) -> {
            powderSnowFogMod = get(va, "powder_snow", FogEnvironmentMod.CODEC);
            lavaFogMod = get(va, "lava", FogEnvironmentMod.CODEC);
        });

        doWith(obj, "dye", (k, v) -> {
            DyeColor color = DyeColor.byName(k, null);
            if (color == null) {
                Polytone.LOGGER.warn("Unknown DyeColor with name {}", k);
                return;
            }
            for (var entry : entries(v)) {
                String param = entry.getKey();
                int col = parseColor(entry.getValue());
                switch (param) {
                    case "diffuse" -> {
                        if (!vanillaDiffuseColors.containsKey(color)) {
                            vanillaDiffuseColors.put(color, color.getTextureDiffuseColor());
                        }
                        color.textureDiffuseColor = ARGB.opaque(col);
                    }
                    case "firework" -> {
                        if (!vanillaFireworkColors.containsKey(color)) {
                            vanillaFireworkColors.put(color, color.fireworkColor);
                        }
                        color.fireworkColor = col;
                    }
                    case "text" -> {
                        if (!vanillaTextColors.containsKey(color)) {
                            vanillaTextColors.put(color, color.textColor);
                        }
                        color.textColor = col;
                    }
                }
            }
        });

        doWith(obj, "special_grass_color", (k, v) -> {
            int hex = parseColor(v);
            switch (k) {
                case "swamp_dark" -> {
                    swampDark = hex;
                }
                case "swamp_light" -> {
                    swampLight = hex;
                }
                case "dark_forest" -> {
                    darkForest = hex;
                }
            }
        });

        doWith(obj, "particle", (k, v) -> {
            Identifier id = Identifier.parse(k.replace("\\", ""));
            try {
                // turn from hex to decimal if it is a single number
                int hex = parseColor(v);
                Polytone.PARTICLE_MODIFIERS.addCustomParticleColor(id, String.valueOf(hex));
            } catch (Exception e) {
                Polytone.PARTICLE_MODIFIERS.addCustomParticleColor(id, v.getAsString());
            }

        });

        doWith(obj, "fishing_line", (k, v) -> {
            switch (k) {
                case "color" -> {
                    fishingLineColor = parseColor(v);
                }
                case "offset" -> {
                    fishingLineOffset = Vec3f.CODEC.decode(JsonOps.INSTANCE, v)
                            .getOrThrow()
                            .getFirst();
                }
            }

        });

        doWith(obj, "world_border", (k, v) -> {
            BorderStatus status = BorderStatus.valueOf(k.toLowerCase(Locale.ROOT));
            int col = parseColor(v);
            if (!vanillaBorderStatus.containsKey(status)) {
                vanillaBorderStatus.put(status, status.getColor());
            }
            status.color = col;
        });

        //for jank of compat... no comment
        doWithOrAlias(obj, List.of("effect", "potion"), (k, v) -> {
            Identifier id = Identifier.parse(k.replace("\\", ""));
            ParticleOptions particle = get(v, "particle", ParticleTypes.CODEC);

            String color = getString(v, "color");
            Integer col;

            if (color == null && v instanceof JsonPrimitive) {
                col = parseColor(v);
            } else {
                col = parseColor(color);
            }


            if (id.getPath().equals("empty")) {
                // TODO: handle PotionContents.EMPTY_COLOR
            } else if (id.getPath().equals("water")) {
                PotionContents.BASE_POTION_COLOR = col;
            } else {
                MobEffect effect = BuiltInRegistries.MOB_EFFECT.getOptional(id).orElse(null);
                if (effect != null) {
                    if (!vanillaEffectColors.containsKey(effect)) {
                        vanillaEffectColors.put(effect, effect.getColor());
                    }

                    effect.color = col;
                    effect.particleFactory = mobEffectInstance -> {
                        int alpha = mobEffectInstance.isAmbient() ? AMBIENT_ALPHA : 255;
                        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, ARGB.color(alpha, col));
                    };
                    if (particle != null) {
                        if (!vanillaEffectParticles.containsKey(effect)) {
                            vanillaEffectParticles.put(effect, effect.particleFactory);
                        }
                        effect.particleFactory = mobEffectInstance -> particle;
                    }
                } else Polytone.LOGGER.warn("Unknown Mob Effect with name {}", id);
            }
        });

        doWith(obj, "sheep", (k, v) -> {
            DyeColor color = DyeColor.byName(k, null);
            if (color != null) {
                int col = ARGB.opaque(parseColor(v));
                customSheepColors.put(color, col);
            } else Polytone.LOGGER.warn("Unknown Dye Color with name {}", k);
        });

        doWith(obj, "xporb", (k, v) -> {
            switch (k) {
                case "particle_replacement" -> Polytone.PARTICLE_MODIFIERS.setXpOrbReplace(v);
                case "color" -> xpOrbColor = new EntityContextExpression(v.getAsString());
                case "red" -> xpOrbColorR = new EntityContextExpression(v.getAsString());
                case "green" -> xpOrbColorG = new EntityContextExpression(v.getAsString());
                case "blue" -> xpOrbColorB = new EntityContextExpression(v.getAsString());
            }
        });

        doWith(obj, "redstone", (k, v) -> {
            int code = Integer.parseInt(k);
            if (code < RedStoneWireBlock.COLORS.length) {
                int col = parseColor(v);
                var rgb = ColorUtils.unpack(col);
                RedStoneWireBlock.COLORS[code] = ARGB.colorFromFloat(1.0f, rgb[0], rgb[1], rgb[2]);
                if (code == 15) {
                    int maxPower = ARGB.colorFromFloat(1.0f, rgb[0], rgb[1], rgb[2]);
                    net.minecraft.core.particles.DustParticleOptions.REDSTONE_PARTICLE_COLOR = maxPower;
                    ((DustParticleOptionAccessor) DustParticleOptions.REDSTONE).setColor(maxPower);
                }
            } else Polytone.LOGGER.warn("Redstone color index must be between 0 and 15");
        });

        doWith(obj, "text", (k, v) -> {
            //TODO: this wont be applied unless we are into a level...
            if (k.equals("splash")) {
                int splashCol = parseColor(v);
                refreshSplash(Style.EMPTY.withColor(splashCol));
            } else if (k.equals("xpbar")) {
                if (v instanceof JsonPrimitive) {
                    xpBar = parseColor(v);
                } else {
                    for (var e : entries(v)) {
                        switch (e.getKey()) {
                            case "foreground" -> xpBar = parseColor(e.getValue());
                            case "background" -> xpBarBack = parseColor(e.getValue());
                        }
                    }
                }
            } else if (k.startsWith("code:")) {
                String s = k.substring(5);
                int code = Integer.parseInt(s);
                ChatFormatting text = ChatFormatting.getById(code);
                setTextColor(text, parseColor(v));
            } else if (k.equals("code")) {
                for (var entry : entries(v)) {
                    String s = entry.getKey();
                    int code = Integer.parseInt(s);
                    ChatFormatting text = ChatFormatting.getById(code);
                    setTextColor(text, parseColor(entry.getValue()));
                }
            } else {
                ChatFormatting text = ChatFormatting.getByName(k);
                setTextColor(text, parseColor(v));
            }
        });

        doWith(obj, "palette", (k, v) -> {
            if (k.equals("block") && v.isJsonObject()) {
                for (var entry : getEntries(v.getAsJsonObject(), "block")) {
                    String path = entry.getKey().replace("~/colormap/", fileId.getNamespace() + ":");
                    Polytone.BLOCK_MODIFIERS.addSimpleColormap(Identifier.parse(path), entry.getValue().getAsString());
                }
            }
        });
    }

    private void refreshSplash(Style style) {
        SplashManager.DEFAULT_STYLE = style;
        SplashManager manager = Minecraft.getInstance().getSplashManager();
        var splashes = manager.splashes;
        List<Component> newSplashes = new ArrayList<>();
        for (Component c : splashes) {
            newSplashes.add(c.copy().withStyle(style));
        }
        manager.splashes = newSplashes;
    }

    private void setTextColor(ChatFormatting text, int col) {
        if (!vanillaChatFormatting.containsKey(text)) {
            vanillaChatFormatting.put(text, text.getColor());
        }
        text.color = col;
        TextColor tc = TextColor.fromLegacyFormat(text);
        tc.value = col;
    }

    private static void doWith(JsonElement o, String key, BiConsumer<String, JsonElement> entryHandler) {
        if (!(o instanceof JsonObject obj)) return;
        try {
            if (obj.has(key)) {
                JsonObject sub = GsonHelper.getAsJsonObject(obj, key);
                for (var entry : sub.entrySet()) {
                    entryHandler.accept(entry.getKey(), entry.getValue());
                }
            }
        } catch (JsonParseException e) {
            throw new JsonParseException("Failed to parse color JSON for key: " + key, e);
        }
    }

    private static void doWithOrAlias(JsonObject obj, Collection<String> names, BiConsumer<String, JsonElement> entryHandler) {
        for (var s : names) {
            doWith(obj, s, entryHandler);
        }
    }

    private static Set<Map.Entry<String, JsonElement>> getEntries(JsonObject element, String key) {
        var elements = element.get(key);
        if (elements != null && elements.isJsonObject()) {
            return elements.getAsJsonObject().entrySet();
        }
        return Collections.emptySet();
    }

    @Nullable
    private static <T> T get(JsonElement element, String key, Codec<T> codec) {
        if (element instanceof JsonObject jo) {
            JsonElement joo = jo.get(key);
            if (joo != null) {
                return codec.decode(JsonOps.INSTANCE, joo).getOrThrow().getFirst();
            }
        }
        return null;
    }

    private static String getString(JsonElement element, String key) {
        if (element instanceof JsonObject jo) {
            JsonElement joo = jo.get(key);
            if (joo != null && joo.isJsonPrimitive() && joo.getAsJsonPrimitive().isString()) {
                return joo.getAsString();
            }
        }
        return null;
    }

    private static Set<Map.Entry<String, JsonElement>> entries(JsonElement element) {
        if (element.isJsonObject()) {
            return element.getAsJsonObject().entrySet();
        }
        return Collections.emptySet();
    }

    private static int parseColor(JsonElement obj) {
        return ColorUtils.COLOR.decode(JsonOps.INSTANCE, obj)
                .getOrThrow()
                .getFirst(); // this will throw if the element is not a valid color
    }

    private static int parseColor(String str) {
        str = str.replace("#", "").replace("0x", "");
        return Integer.parseInt(str.trim(), 16);
    }

    @Override
    protected void applyWithLevel(HolderLookup.Provider access, boolean isLogIn) {

    }

    @Override
    protected void resetWithLevel(boolean logOff) {
        //TODO: potion empty
        //PotionContents.EMPTY_COLOR = 16253176;
        PotionContents.BASE_POTION_COLOR = 3694022;
        xpBar = null;
        xpOrbParticle = null;
        xpOrbColor = null;
        xpOrbColorR = null;
        xpOrbColorG = null;
        xpOrbColorB = null;
        fishingLineOffset = null;
        fishingLineColor = null;
        skyFlashColor = null;
        voidDarknessOffset = null;
        horizonHeight = null;
        lavaFogMod = null;
        powderSnowFogMod = null;
        // map colors
        for (var e : vanillaMapColors.entrySet()) {
            MapColor color = e.getKey();
            color.col = e.getValue();
        }
        vanillaMapColors.clear();

        // dye colors
        for (var e : vanillaDiffuseColors.entrySet()) {
            DyeColor color = e.getKey();
            color.textureDiffuseColor = e.getValue();
        }
        vanillaDiffuseColors.clear();

        for (var e : vanillaFireworkColors.entrySet()) {
            DyeColor color = e.getKey();
            color.fireworkColor = e.getValue();
        }
        vanillaFireworkColors.clear();

        for (var e : vanillaTextColors.entrySet()) {
            DyeColor color = e.getKey();
            color.textColor = e.getValue();
        }
        vanillaTextColors.clear();

        //chat formatting
        for (var e : vanillaChatFormatting.entrySet()) {
            ChatFormatting text = e.getKey();
            text.color = e.getValue();
            TextColor tc = TextColor.fromLegacyFormat(text);
            tc.value = e.getValue();
        }
        vanillaChatFormatting.clear();

        //effects
        for (var e : vanillaEffectColors.object2IntEntrySet()) {
            MobEffect effect = e.getKey();
            effect.color = e.getIntValue();
        }
        vanillaEffectColors.clear();

        for (var e : vanillaEffectParticles.entrySet()) {
            MobEffect effect = e.getKey();
            effect.particleFactory = e.getValue();
        }
        vanillaEffectParticles.clear();

        //border status
        for (var e : vanillaBorderStatus.entrySet()) {
            BorderStatus status = e.getKey();
            status.color = e.getValue();
        }
        vanillaBorderStatus.clear();

        RedStoneWireBlock.COLORS = Arrays.copyOf(originalRedstoneWireColors, originalRedstoneWireColors.length);
        DustParticleOptions.REDSTONE_PARTICLE_COLOR = DEFAULT_COLOR;//default
        ((DustParticleOptionAccessor) DustParticleOptions.REDSTONE).setColor(DustParticleOptions.REDSTONE_PARTICLE_COLOR);
    }

    public void regenSheepColors() {
        var map = new EnumMap<>(originalSheepColors);
        map.putAll(customSheepColors);
        customSheepColors.clear();

        ColorLerper.Type.SHEEP.colorByDye = map;
    }

    public float @Nullable [] getXpOrbColor(ExperienceOrbRenderState orb, float partialTicks) {
     /*
        Vec3 orbPos = new Vec3(orb.x, orb.y, orb.z);
        Level level = Minecraft.getInstance().level;
        if (xpOrbColor != null) {
            int color = (int) xpOrbColor.evaluate(orb);
            return ColorUtils.unpack(color);
        }
        if (xpOrbColorR == null && xpOrbColorG == null && xpOrbColorB == null) return null;
        float r = 0;
        float g = 0;
        float b = 0;
        if (xpOrbColorR != null) r = (float) xpOrbColorR.evaluate(orb);
        if (xpOrbColorG != null) g = (float) xpOrbColorG.evaluate(orb);
        if (xpOrbColorB != null) b = (float) xpOrbColorB.evaluate(orb);

      */
        return null;
        //TODO:
        //return new float[]{r, g, b};
    }


    @Nullable
    public Vec3f getFishingLineOffset() {
        return fishingLineOffset;
    }

    @Nullable
    public Integer getFishingLineColor() {
        return fishingLineColor;
    }
}
