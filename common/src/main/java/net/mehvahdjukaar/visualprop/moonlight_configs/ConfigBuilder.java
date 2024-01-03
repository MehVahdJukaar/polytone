package net.mehvahdjukaar.visualprop.moonlight_configs;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A loader independent config builder
 * Support common config syncing
 */
public abstract class ConfigBuilder {

    protected final Map<String, String> comments = new HashMap<>();
    private String currentComment;
    private String currentKey;
    protected boolean synced;
    protected Runnable changeCallback;

    @ExpectPlatform
    public static ConfigBuilder create(ResourceLocation name, ConfigType type) {
        throw new AssertionError();
    }

    private final ResourceLocation name;
    protected final ConfigType type;

    public ConfigBuilder(ResourceLocation name, ConfigType type) {
        this.name = name;
        this.type = type;
    }

    public ConfigSpec buildAndRegister(){
        var spec = this.build();
        spec.register();
        return spec;
    };

    public abstract ConfigSpec build();

    public ResourceLocation getName() {
        return name;
    }


    public abstract ConfigBuilder push(String category);

    public abstract ConfigBuilder pop();

    public abstract Supplier<Boolean> define(String name, boolean defaultValue);

    public abstract Supplier<Double> define(String name, double defaultValue, double min, double max);

    public abstract Supplier<Integer> define(String name, int defaultValue, int min, int max);

    public abstract Supplier<Integer> defineColor(String name, int defaultValue);

    public abstract Supplier<String> define(String name, String defaultValue, Predicate<Object> validator);

    public Supplier<String> define(String name, String defaultValue) {
        return define(name, defaultValue, STRING_CHECK);
    }

    public <T extends String> Supplier<List<String>> define(String name, List<? extends T> defaultValue) {
        return define(name, defaultValue, s -> true);
    }

    protected abstract String currentCategory();

    public abstract <T extends String> Supplier<List<String>> define(String name, List<? extends T> defaultValue, Predicate<Object> predicate);

    public abstract <V extends Enum<V>> Supplier<V> define(String name, V defaultValue);

    @Deprecated(forRemoval = true)
    public abstract  <T> Supplier<List<? extends T>> defineForgeList(String path, List<? extends T> defaultValue, Predicate<Object> elementValidator);

    public Component description(String name) {
        return Component.translatable(translationKey(name));
    }

    public Component tooltip(String name) {
        return Component.translatable(tooltipKey(name));
    }

    public String tooltipKey(String name) {
        return name;
    }

    public String translationKey(String name) {
        return name;
    }


    /**
     * Try not to use this. Just here to make porting easier
     * Will add entries manually to the english language file
     */
    public ConfigBuilder comment(String comment) {
        this.currentComment = comment;
        if (this.currentComment != null && this.currentKey != null) {
            comments.put(currentKey, currentComment);
            this.currentComment = null;
            this.currentKey = null;
        }
        return this;
    }

    public ConfigBuilder setSynced(){
        if(this.type == ConfigType.CLIENT){
            throw new UnsupportedOperationException("Config syncing cannot be used for client config as its not needed");
        }
        this.synced = true;
        return this;
    }

    public ConfigBuilder onChange(Runnable callback){
        this.changeCallback = callback;
        return this;
    }

    protected void maybeAddTranslationString(String name) {
        this.currentKey = this.tooltipKey(name);
        if (this.currentComment != null && this.currentKey != null) {
            comments.put(currentKey, currentComment);
            this.currentComment = null;
            this.currentKey = null;
        }
    }

    public static final Predicate<Object> STRING_CHECK = o -> o instanceof String;

    public static final Predicate<Object> LIST_STRING_CHECK = (s) -> {
        if (s instanceof List<?>) {
            return ((Collection<?>) s).stream().allMatch(o -> o instanceof String);
        }
        return false;
    };

    public static final Predicate<Object> COLOR_CHECK = s -> {
        try {
            Integer.parseUnsignedInt(((String) s).replace("0x", ""), 16);
            return true;
        } catch (Exception e) {
            return false;
        }
    };
}
