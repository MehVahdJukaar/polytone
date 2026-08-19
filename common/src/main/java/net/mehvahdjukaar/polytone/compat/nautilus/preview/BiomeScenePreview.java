package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.render.SceneCamera;
import net.mehvahdjukaar.nautilus.swing.preview.LivePreview;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewLayout;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.preview.scene.LiveViewport;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiScale;
import net.mehvahdjukaar.nautilus.swing.toolkit.UiTheme;
import net.mehvahdjukaar.nautilus.swing.widget.RegistryPickerField;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.BiomeSceneRenderPass.Colors;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.BiomeSceneRenderPass.Placement;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.BiomeSceneRenderPass.Tint;
import net.mehvahdjukaar.polytone.compat.nautilus.preview.BiomeSceneRenderPass.WaterQuad;
import net.mehvahdjukaar.polytone.content.biome.BiomeEffectModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.swing.Box;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// Live preview for biome effect modifiers. Renders a tiny stylised diorama - a grass shore with a small oak
// and tufts of grass dropping into a water pool, under a sky/fog dome - through the game's own block models
// offscreen (see BiomeSceneRenderPass).
public final class BiomeScenePreview extends LivePreview {

    private static final int DEF_SKY = 0x78A7FF;
    private static final int DEF_FOG = 0xC0D8FF;
    private static final int DEF_GRASS = 0x91BD59;
    private static final int DEF_FOLIAGE = 0x77AB2F;
    private static final int DEF_WATER = 0x3F76E4;

    private static final List<Placement> BLOCKS = buildDiorama();
    private static final WaterQuad WATER = new WaterQuad(4f, 4f, 7f, 7f, 1.85f, 1f);

    private final LiveViewport viewport = new LiveViewport();
    private final RegistryPickerField biomePicker = new RegistryPickerField(Registries.BIOME, id -> recompute());

    // Read on the render thread by the render pass; written on the EDT from recompute().
    private volatile Colors colors = new Colors(DEF_SKY, DEF_FOG, DEF_GRASS, DEF_FOLIAGE, DEF_WATER, GrassColorModifier.NONE);
    private volatile @Nullable BiomeEffectModifier mod;

    public BiomeScenePreview(TabPreview.Context ctx) {
        super("Sample at player position", "Sample the biome the player is standing in instead of the picked one.");
        biomePicker.setSelected(Identifier.withDefaultNamespace("plains"));

        viewport.setRenderer(new SceneRenderer());
        viewport.setPanEnabled(true);
        viewport.frame(new Vector3f(3.5f, 2f, 3.5f), 7f, true);
        viewport.setBorder(UiTheme.hairlineBorder());

        Box content = PreviewLayout.column();
        PreviewLayout.add(content, PreviewLayout.labeled("Biome", biomePicker));
        PreviewLayout.add(content, liveToggle);
        content.add(Box.createVerticalStrut(UiScale.med()));
        PreviewLayout.addFilling(content, new SquareRow(0, UiScale.px(200), UiScale.px(460), viewport));

        install(content);
        recompute();
    }

    @Override
    public void dispose() {
        super.dispose();
        viewport.dispose();
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.mod = value instanceof BiomeEffectModifier m ? m : null;
        recompute();
    }

    @Override
    protected void onLiveChanged(boolean live) {
        biomePicker.setEnabled(!live);
    }

    @Override
    protected void recompute() {
        BiomeEffectModifier m = this.mod;
        Biome biome;
        double bx = 0, bz = 0;

        if (isLive()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                biome = null;
                statusText("No world loaded - showing defaults.");
            } else {
                BlockPos pos = mc.player.blockPosition();
                biome = mc.level.getBiome(pos).value();
                bx = pos.getX();
                bz = pos.getZ();
                statusText("Sampling at player position.");
            }
        } else {
            biome = biomePicker.getSelectedValue(Registries.BIOME);
            if (biome == null) statusText("Biome unavailable (world not loaded?) - showing defaults.");
            else statusText("");
        }

        colors = resolveColors(m, biome, bx, bz);
        viewport.refresh();
    }

    // Base = the biome's own colour (or a default when unresolved); each channel the modifier sets wins.
    private Colors resolveColors(@Nullable BiomeEffectModifier m, @Nullable Biome biome, double bx, double bz) {
        int sky = DEF_SKY, fog = DEF_FOG;
        if (biome != null) {
            EnvironmentAttributeMap base = biome.getAttributes();
            // Sky/fog moved to the environment-attribute system; apply the modifier's attribute mods on top.
            EnvironmentAttributeMap eff = m != null && !m.attributeModifications().isEmpty()
                    ? m.attributeModifications().applyAllModifications(biome) : base;
            sky = readColor(eff, base, EnvironmentAttributes.SKY_COLOR, DEF_SKY);
            fog = readColor(eff, base, EnvironmentAttributes.FOG_COLOR, DEF_FOG);
        }
        int water = channel(opt(m, BiomeEffectModifier::waterColor), biome != null ? biome.getWaterColor() : DEF_WATER);
        int foliage = channel(opt(m, BiomeEffectModifier::foliageColorOverride), biome != null ? biome.getFoliageColor() : DEF_FOLIAGE);
        int grass = channel(opt(m, BiomeEffectModifier::grassColorOverride), biome != null ? biome.getGrassColor(bx, bz) : DEF_GRASS);
        GrassColorModifier gcm = m != null && m.grassColorModifier().isPresent() ? m.grassColorModifier().get() : GrassColorModifier.NONE;
        return new Colors(sky, fog, grass, foliage, water, gcm);
    }

    // Static read of a colour attribute: prefer the modifier-applied map, fall back to the biome's own,
    // then the default. applyModifier resolves any modifier chain against the supplied base.
    private static int readColor(EnvironmentAttributeMap eff, EnvironmentAttributeMap base,
                                 EnvironmentAttribute<Integer> attr, int def) {
        if (eff.contains(attr)) return eff.applyModifier(attr, def) & 0xFFFFFF;
        if (base.contains(attr)) return base.applyModifier(attr, def) & 0xFFFFFF;
        return def;
    }

    private static Optional<Integer> opt(@Nullable BiomeEffectModifier m, Function<BiomeEffectModifier, Optional<Integer>> getter) {
        return m == null ? Optional.empty() : getter.apply(m);
    }

    private static int channel(Optional<Integer> override, int base) {
        return (override.isPresent() ? override.get() : base) & 0xFFFFFF;
    }

    // A 7x7 grass shelf on dirt, a far corner carved out one block deep for the pool, plus a small oak
    // and a few grass tufts on the shore. Block y=1 is the shore top, y=0 the pool floor.
    private static List<Placement> buildDiorama() {
        BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState dirt = Blocks.DIRT.defaultBlockState();
        BlockState log = Blocks.OAK_LOG.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        BlockState tuft = Blocks.SHORT_GRASS.defaultBlockState();

        List<Placement> b = new ArrayList<>();
        for (int x = 0; x <= 6; x++) {
            for (int z = 0; z <= 6; z++) {
                boolean pool = x >= 4 && z >= 4;
                b.add(new Placement(new BlockPos(x, 0, z), dirt, Tint.NONE));
                if (!pool) b.add(new Placement(new BlockPos(x, 1, z), grass, Tint.GRASS));
            }
        }

        for (int y = 2; y <= 4; y++) b.add(new Placement(new BlockPos(2, y, 2), log, Tint.NONE));
        int[][] ring = {{1, 2}, {3, 2}, {2, 1}, {2, 3}, {1, 1}, {3, 3}, {1, 3}, {3, 1}};
        for (int[] o : ring) b.add(new Placement(new BlockPos(o[0], 4, o[1]), leaves, Tint.FOLIAGE));
        int[][] cap = {{2, 2}, {1, 2}, {3, 2}, {2, 1}, {2, 3}};
        for (int[] o : cap) b.add(new Placement(new BlockPos(o[0], 5, o[1]), leaves, Tint.FOLIAGE));

        int[][] tufts = {{0, 0}, {5, 1}, {6, 2}, {2, 5}, {0, 4}};
        for (int[] o : tufts) b.add(new Placement(new BlockPos(o[0], 2, o[1]), tuft, Tint.GRASS));

        return List.copyOf(b);
    }

    private final class SceneRenderer implements LiveViewport.Renderer {
        @Override
        public void advance() {
        }

        @Override
        public void render(SceneCamera camera, int width, int height) {
            BiomeSceneRenderPass.render(camera, width, height, colors, BLOCKS, WATER);
        }
    }
}
