package net.mehvahdjukaar.polytone.compat.nautilus.preview;

import com.google.gson.JsonElement;
import net.mehvahdjukaar.nautilus.SchemaEditor.Side;
import net.mehvahdjukaar.nautilus.render.SceneCamera;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewStatus;
import net.mehvahdjukaar.nautilus.swing.preview.PreviewSurface;
import net.mehvahdjukaar.nautilus.swing.preview.TabPreview;
import net.mehvahdjukaar.nautilus.swing.preview.scene.LiveViewport;
import net.mehvahdjukaar.nautilus.swing.toolkit.EditorOps;
import net.mehvahdjukaar.nautilus.swing.toolkit.SquareRow;
import net.mehvahdjukaar.nautilus.swing.toolkit.StyledLabels;
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
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

// A biome modifier only overrides an existing biome's effects, so the chosen biome's real colours are
// resolved first and the overrides layered on top. No world loaded = plains-ish defaults.
public final class BiomeScenePreview implements TabPreview {

    private static final int DEF_SKY = 0x78A7FF;
    private static final int DEF_FOG = 0xC0D8FF;
    private static final int DEF_GRASS = 0x91BD59;
    private static final int DEF_FOLIAGE = 0x77AB2F;
    private static final int DEF_WATER = 0x3F76E4;

    private static final List<Placement> BLOCKS = buildDiorama();
    // Pool occupies the far corner (x/z 4..6), one block below the shore; surface just under the shore
    // top, floor on top of the dirt bed at y=1.
    private static final WaterQuad WATER = new WaterQuad(4f, 4f, 7f, 7f, 1.85f, 1f);

    private final PreviewStatus status = new PreviewStatus();
    private final LiveViewport viewport = new LiveViewport();
    private final RegistryPickerField biomePicker = new RegistryPickerField(Registries.BIOME, id -> recompute());
    private final JCheckBox liveToggle = new JCheckBox("Sample at player position");
    private final Timer liveTimer = new Timer(500, e -> { if (liveToggle.isSelected()) recompute(); });
    private PreviewSurface root;

    // Read on the render thread by the render pass; written on the EDT from recompute().
    private volatile Colors colors = new Colors(DEF_SKY, DEF_FOG, DEF_GRASS, DEF_FOLIAGE, DEF_WATER, GrassColorModifier.NONE);
    private volatile @Nullable BiomeEffectModifier mod;

    public BiomeScenePreview(TabPreview.Context ctx) {
        biomePicker.setSelected(ResourceLocation.withDefaultNamespace("plains"));
        liveToggle.setOpaque(false);
        liveToggle.setToolTipText("Sample the biome the player is standing in instead of the picked one.");
        liveToggle.addActionListener(e -> setLive(liveToggle.isSelected()));

        viewport.setRenderer(new SceneRenderer());
        viewport.setPanEnabled(true);
        viewport.frame(new Vector3f(3.5f, 2f, 3.5f), 7f, true);
        viewport.setBorder(UiTheme.hairlineBorder());

        buildLayout();
        recompute();
    }

    private void buildLayout() {
        Box toolbar = Box.createVerticalBox();
        Box topRow = Box.createHorizontalBox();
        topRow.add(status);
        topRow.add(Box.createHorizontalGlue());
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbar.add(topRow);

        Box content = Box.createVerticalBox();
        addField(content, labeled("Biome", biomePicker));
        liveToggle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(liveToggle);
        content.add(Box.createVerticalStrut(UiScale.med()));

        SquareRow sceneRow = new SquareRow(0, UiScale.px(200), UiScale.px(460), viewport);
        sceneRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sceneRow);

        root = new PreviewSurface(toolbar, content);
        root.setMinimumSize(new Dimension(UiScale.px(160), UiScale.px(120)));
    }

    @Override
    public JComponent component() {
        return root;
    }

    @Override
    public void dispose() {
        liveTimer.stop();
        viewport.dispose();
    }

    @Override
    public void onValueChanged(@Nullable JsonElement json, @Nullable Object value) {
        this.mod = value instanceof BiomeEffectModifier m ? m : null;
        recompute();
    }

    private void setLive(boolean live) {
        biomePicker.setEnabled(!live);
        if (live) liveTimer.start();
        else liveTimer.stop();
        recompute();
    }

    private void recompute() {
        BiomeEffectModifier m = this.mod;
        Biome biome;
        double bx = 0, bz = 0;

        if (liveToggle.isSelected()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                biome = null;
                status.error("No world loaded - showing defaults.");
            } else {
                BlockPos pos = mc.player.blockPosition();
                biome = mc.level.getBiome(pos).value();
                bx = pos.getX();
                bz = pos.getZ();
                status.setText("Sampling at player position.");
            }
        } else {
            biome = resolveBiome(biomePicker.getSelected());
            if (biome == null) status.info("Biome unavailable (world not loaded?) - showing defaults.");
            else status.setText("");
        }

        colors = resolveColors(m, biome, bx, bz);
        viewport.refresh();
    }

    // Base = the biome's own colour (or a default when unresolved); each channel the modifier sets wins.
    // Grass takes the biome's grass colour then the modifier's grass post-process, applied per block in
    // the render pass so swamp's two-tone shows.
    private Colors resolveColors(@Nullable BiomeEffectModifier m, @Nullable Biome biome, double bx, double bz) {
        int sky = channel(opt(m, BiomeEffectModifier::skyColor), biome != null ? biome.getSkyColor() : DEF_SKY);
        int fog = channel(opt(m, BiomeEffectModifier::fogColor), biome != null ? biome.getFogColor() : DEF_FOG);
        int water = channel(opt(m, BiomeEffectModifier::waterColor), biome != null ? biome.getWaterColor() : DEF_WATER);
        int foliage = channel(opt(m, BiomeEffectModifier::foliageColorOverride), biome != null ? biome.getFoliageColor() : DEF_FOLIAGE);
        int grass = channel(opt(m, BiomeEffectModifier::grassColorOverride), biome != null ? biome.getGrassColor(bx, bz) : DEF_GRASS);
        GrassColorModifier gcm = m != null && m.grassColorModifier().isPresent() ? m.grassColorModifier().get() : GrassColorModifier.NONE;
        return new Colors(sky, fog, grass, foliage, water, gcm);
    }

    private @Nullable Biome resolveBiome(@Nullable ResourceLocation id) {
        if (id == null) return null;
        try {
            Side side = EditorOps.resolveSide(biomePicker);
            return EditorOps.registries(side).lookup(Registries.BIOME)
                    .flatMap(lookup -> lookup.get(ResourceKey.create(Registries.BIOME, id)))
                    .map(Holder::value)
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Optional<Integer> opt(@Nullable BiomeEffectModifier m, Function<BiomeEffectModifier, Optional<Integer>> getter) {
        return m == null ? Optional.empty() : getter.apply(m);
    }

    private static int channel(Optional<Integer> override, int base) {
        return (override.isPresent() ? override.get() : base) & 0xFFFFFF;
    }

    // A 7x7 grass shelf on dirt, a far corner carved out one block deep for the pool, plus a small oak
    // and a few grass tufts on the shore. Grass tops/tufts take the grass colour, leaves the foliage
    // colour; dirt and logs keep their own texture. Block y=1 is the shore top, y=0 the pool floor.
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

    private static void addField(JComponent box, JComponent field) {
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        box.add(field);
        box.add(Box.createVerticalStrut(UiScale.small()));
    }

    private static JComponent labeled(String text, JComponent field) {
        Box row = Box.createVerticalBox();
        JLabel l = StyledLabels.small(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, field.getPreferredSize().height));
        row.add(l);
        row.add(field);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private final class SceneRenderer implements LiveViewport.Renderer {
        @Override
        public void advance() {
            // Static scene - nothing to tick.
        }

        @Override
        public void render(SceneCamera camera, int width, int height) {
            BiomeSceneRenderPass.render(camera, width, height, colors, BLOCKS, WATER);
        }
    }
}
