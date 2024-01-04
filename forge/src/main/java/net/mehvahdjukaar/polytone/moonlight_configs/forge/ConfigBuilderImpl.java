package net.mehvahdjukaar.polytone.moonlight_configs.forge;

import net.mehvahdjukaar.polytone.moonlight_configs.ConfigBuilder;
import net.mehvahdjukaar.polytone.moonlight_configs.ConfigType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ConfigBuilderImpl extends ConfigBuilder {

    public static ConfigBuilder create(ResourceLocation name, ConfigType type) {
        return new ConfigBuilderImpl(name, type);
    }

    private final ForgeConfigSpec.Builder builder;

    private String cat = null;

    public ConfigBuilderImpl(ResourceLocation name, ConfigType type) {
        super(name, type);
        this.builder = new ForgeConfigSpec.Builder();
    }

    @Override
    protected String currentCategory() {
        return cat;
    }


    @Override
    public ConfigSpecWrapper build() {
        return new ConfigSpecWrapper(this.getName(), this.builder.build(), this.type, this.synced, this.changeCallback);
    }

    @Override
    public ConfigBuilderImpl push(String category) {
        assert cat == null;
        builder.push(category);
        cat = category;
        return this;
    }

    @Override
    public ConfigBuilderImpl pop() {
        assert cat != null;
        builder.pop();
        cat = null;
        return this;
    }

    @Override
    public Supplier<Boolean> define(String name, boolean defaultValue) {
        maybeAddTranslationString(name);
        return builder.define(name, defaultValue);
    }

    @Override
    public Supplier<Double> define(String name, double defaultValue, double min, double max) {
        maybeAddTranslationString(name);
        return builder.defineInRange(name, defaultValue, min, max);
    }

    @Override
    public Supplier<Integer> define(String name, int defaultValue, int min, int max) {
        maybeAddTranslationString(name);
        return builder.defineInRange(name, defaultValue, min, max);
    }

    @Override
    public Supplier<Integer> defineColor(String name, int defaultValue) {
        maybeAddTranslationString(name);
        var stringConfig = builder.define(name, Integer.toHexString(defaultValue), ConfigBuilder.COLOR_CHECK);
        return () -> Integer.parseUnsignedInt(stringConfig.get().replace("0x", ""), 16);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    @Override
    public Supplier<String> define(String name, String defaultValue, Predicate<Object> validator) {
        maybeAddTranslationString(name);
        ForgeConfigSpec.ConfigValue<String> stringConfig = builder.define(name, (String) defaultValue, validator);
        return stringConfig;
    }

    @Override
    public <T extends String> Supplier<List<String>> define(String name, List<? extends T> defaultValue, Predicate<Object> predicate) {
        maybeAddTranslationString(name);
        var value = builder.defineList(name, defaultValue, predicate);
        return () -> (List<String>) value.get();
    }

    @Override
    public <T> Supplier<List<? extends T>> defineForgeList(String name, List<? extends T> defaultValue, Predicate<Object> predicate) {
        maybeAddTranslationString(name);
        var value = builder.defineList(name, defaultValue, predicate);
        return (Supplier<List<? extends T>>) value;
    }

    @Override
    public <V extends Enum<V>> Supplier<V> define(String name, V defaultValue) {
        maybeAddTranslationString(name);
        return builder.defineEnum(name, defaultValue);
    }

    @Override
    public ConfigBuilder comment(String comment) {
        builder.comment(comment); //.translationKey(getTranslationName());
        //TODO: choose. either add a translation or a comment literal not both
        return super.comment(comment);
    }
}
