package net.mehvahdjukaar.visualprop.moonlight_configs.yacl;

import dev.isxander.yacl.api.*;
import dev.isxander.yacl.gui.controllers.ColorController;
import dev.isxander.yacl.gui.controllers.LabelController;
import dev.isxander.yacl.gui.controllers.TickBoxController;
import dev.isxander.yacl.gui.controllers.cycling.EnumController;
import dev.isxander.yacl.gui.controllers.slider.DoubleSliderController;
import dev.isxander.yacl.gui.controllers.slider.IntegerSliderController;
import dev.isxander.yacl.gui.controllers.string.StringController;
import net.mehvahdjukaar.visualprop.moonlight_configs.fabric.ConfigEntry;
import net.mehvahdjukaar.visualprop.moonlight_configs.fabric.ConfigSubCategory;
import net.mehvahdjukaar.visualprop.moonlight_configs.fabric.FabricConfigSpec;
import net.mehvahdjukaar.visualprop.moonlight_configs.fabric.values.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.function.Function;

public class YACLCompat {

    public static Screen makeScreen(Screen parent, FabricConfigSpec spec) {
        return makeScreen(parent, spec, null);
    }

    public static Screen makeScreen(Screen parent, FabricConfigSpec spec, @Nullable ResourceLocation background) {

        spec.loadFromFile();

        YetAnotherConfigLib.Builder builder = YetAnotherConfigLib.createBuilder();

        builder.title(spec.getName());
        builder.save(spec::saveConfig);


        for (var en : spec.getMainEntry().getEntries()) {
            //skips stray config values
            if (!(en instanceof ConfigSubCategory c)) continue;
            var mainCat = ConfigCategory.createBuilder()
                    .name(Component.translatable(c.getName()));


            for (var entry : c.getEntries()) {
                if (entry instanceof ConfigSubCategory subCat) {
                    var subBuilder = OptionGroup.createBuilder()
                            .name(Component.translatable(subCat.getName()))
                            .collapsed(true);

                    addEntriesRecursive(mainCat, subBuilder, subCat);

                    mainCat.group(subBuilder.build());
                } else {
                    mainCat.option(buildEntry(entry));
                }
            }

            builder.category(mainCat.build());

        }
        return builder.build().generateScreen(parent);
    }

    private static void addEntriesRecursive(ConfigCategory.Builder builder, OptionGroup.Builder subCategoryBuilder, ConfigSubCategory c) {

        for (var entry : c.getEntries()) {
            if (entry instanceof ConfigSubCategory cc) {
                //not nested subcat not supported. merging
                var scb = OptionGroup.createBuilder()
                        .name(Component.translatable(entry.getName()))
                        .tooltip(Component.literal("Unsupported"));
                // optional
                addEntriesRecursive(builder, subCategoryBuilder, cc);
                //subCategoryBuilder.group(scb.build());
            } else subCategoryBuilder.option(buildEntry(entry));
        }
    }

    private static Option<?> buildEntry(ConfigEntry entry) {

        if (entry instanceof ColorConfigValue col) {
            var e = Option.createBuilder(Color.class)
                    .name(col.getTranslation())
                    .binding(new Color(col.getDefaultValue()), () -> new Color(col.get()), v -> col.set(v.getRGB()))
                    .controller(o -> new ColorController(o, true));
            var description = col.getDescription();
            if (description != null) e.tooltip(description);// Shown when the user hover over this option
            return e.build(); // Builds the option entry for cloth config
        } else if (entry instanceof IntConfigValue ic) {
            var e = Option.createBuilder(Integer.class)
                    .name(ic.getTranslation())
                    .binding(ic.getDefaultValue(), ic, ic::set)
                    .controller(o -> new IntegerSliderController(o, ic.getMin(), ic.getMax(), 1));
            var description = ic.getDescription();
            if (description != null) e.tooltip(description);// Shown when the user hover over this option
            return e.build(); // Builds the option entry for cloth config
        } else if (entry instanceof DoubleConfigValue dc) {
            var e = Option.createBuilder(Double.class)
                    .name(dc.getTranslation())
                    .binding(dc.getDefaultValue(), dc, dc::set)
                    .controller(o -> new DoubleSliderController(o, dc.getMin(), dc.getMax(), 0.0001, DEFAULT_FORMATTER));
            var description = dc.getDescription();
            if (description != null) e.tooltip(description);// Shown when the user hover over this option
            return e.build(); // Builds the option entry for cloth config
        } else if (entry instanceof StringConfigValue sc) {
            var e = Option.createBuilder(String.class)
                    .name(sc.getTranslation())
                    .binding(sc.getDefaultValue(), sc, sc::set)
                    .controller(StringController::new);
            var description = sc.getDescription();
            if (description != null) e.tooltip(description);// Shown when the user hover over this option
            return e.build(); // Builds the option entry for cloth config
        } else if (entry instanceof BoolConfigValue bc) {
            var e = Option.createBuilder(Boolean.class)
                    .name(bc.getTranslation())
                    .binding(bc.getDefaultValue(), bc, bc::set)
                    .controller(TickBoxController::new);
            var description = bc.getDescription();
            if (description != null) e.tooltip(description);// Shown when the user hover over this option
            return e.build(); // Builds the option entry for cloth config
        } else if (entry instanceof EnumConfigValue<?> ec) {
            return addEnum(ec);
        } else if (entry instanceof ListStringConfigValue<?> lc) {
            var e = Option.createBuilder(Component.class)
                    .name(lc.getTranslation())
                    .binding(Binding.immutable(Component.literal("String Lists are not supported")))
                    .controller(LabelController::new);
            var description = lc.getDescription();
            if (description != null) e.tooltip(description);// Shown when the user hover over this option
            return e.build(); // Builds the option entry for cloth config
        }
        throw new UnsupportedOperationException("unknown entry: " + entry.getClass().getName());
    }
    public static final Function<Double, Component> DEFAULT_FORMATTER = value -> Component.nullToEmpty(String.format("%,.4f", value).replaceAll("[  ]", " "));

    private static <T extends Enum<T>> Option<T> addEnum(EnumConfigValue<T> ec) {
        var e = Option.createBuilder(ec.getEnumClass())
                .name(ec.getTranslation())
                .binding(ec.getDefaultValue(), ec, ec::set)
                .controller(EnumController::new);
        var description = ec.getDescription();
        if (description != null) e.tooltip(description);// Shown when the user hover over this option
        return e.build(); // Builds the option entry for cloth config
    }

}