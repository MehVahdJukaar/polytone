package net.mehvahdjukaar.polytone.content.shaders.light;

import net.mehvahdjukaar.polytone.api.ResolvedPointLight;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.compat.CompatHandler;
import net.mehvahdjukaar.polytone.content.shaders.GLHelper;
import net.mehvahdjukaar.polytone.content.shaders.IShader;
import net.mehvahdjukaar.polytone.content.shaders.LevelRenderPassTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class PolyShaderPointLights implements PointLightStorage {

    //must match polytone_point_lights.glsl
    public static final int MAX_LIGHTS = 64;
    public static final String COUNT = "PolyPointLightCount";
    private static final String POSITIONS = "PolyPointLightPos";
    private static final String COLORS = "PolyPointLightColor";

    private final Map<Object, LightAt> lights = new HashMap<>();
    private int generation;

    private final float[] positionsUpload = new float[MAX_LIGHTS * 4];
    private final float[] colorsUpload = new float[MAX_LIGHTS * 4];
    private int count;
    private int packedVersion = -1;
    private Vec3 packedForCamera = Vec3.ZERO;
    private final Int2IntOpenHashMap uploadedVersionByProgram = new Int2IntOpenHashMap();

    public PolyShaderPointLights() {
        uploadedVersionByProgram.defaultReturnValue(Integer.MIN_VALUE);
    }

    @Override
    public void clear() {
        lights.clear();
        uploadedVersionByProgram.clear();
        generation++;
    }

    public void bindTo(IShader shader) {
        int program = shader.programId();
        //GUI items and the hand aren't in wotld space, and a portal view into another level isnt ours
        boolean inOurLevel = (LevelRenderPassTracker.isInLevelPass() || Polytone.POST_CHAINS.isProcessing())
                && LevelRenderPassTracker.currentLevel() == Minecraft.getInstance().level;
        if (!inOurLevel || lights.isEmpty() || CompatHandler.irisShaderPackActive()) {
            if (uploadedVersionByProgram.remove(program) != Integer.MIN_VALUE) {
                GLHelper.setProgramUniform(program, COUNT, 0);
            }
            return;
        }
        repackIfStale(LevelRenderPassTracker.currentCamera().getPosition());
        if (uploadedVersionByProgram.get(program) == packedVersion) return;
        uploadedVersionByProgram.put(program, packedVersion);
        //ez
        GLHelper.setProgramUniform4fv(program, POSITIONS, positionsUpload);
        GLHelper.setProgramUniform4fv(program, COLORS, colorsUpload);
        GLHelper.setProgramUniform(program, COUNT, count);
    }

    private void repackIfStale(Vec3 camera) {
        if (packedVersion == generation && packedForCamera.equals(camera)) return;
        packedVersion = generation;
        packedForCamera = camera;

        List<LightAt> all = new ArrayList<>(lights.values());
        if (all.size() > MAX_LIGHTS) {
            all.sort(Comparator.comparingDouble(l -> camera.distanceToSqr(l.x, l.y, l.z)));
        }
        count = Math.min(all.size(), MAX_LIGHTS);
        for (int i = 0; i < count; i++) {
            LightAt l = all.get(i);
            int color = l.light.color();
            float scale = l.light.brightness() / 255f;
            putVec4(positionsUpload, i, (float) (l.x - camera.x), (float) (l.y - camera.y), (float) (l.z - camera.z), l.light.radius());
            putVec4(colorsUpload, i, FastColor.ARGB32.red(color) * scale, FastColor.ARGB32.green(color) * scale, FastColor.ARGB32.blue(color) * scale, 0);
        }
    }

    private static void putVec4(float[] array, int index, float x, float y, float z, float w) {
        int o = index * 4;
        array[o] = x;
        array[o + 1] = y;
        array[o + 2] = z;
        array[o + 3] = w;
    }

    @Override
    public void set(Object key, double x, double y, double z, ResolvedPointLight light) {
        LightAt l = lights.computeIfAbsent(key, k -> new LightAt());
        l.setPos(x,y,z);
        l.light = light;
        generation++;
    }

    @Override
    public void move(Object key, double x, double y, double z) {
        LightAt l = lights.get(key);
        if (l == null) return;
        l.setPos(x,y,z);
        generation++;
    }

    @Override
    public void remove(Object key) {
        if (lights.remove(key) != null) generation++;
    }

    private static class LightAt {
        double x, y, z;
        ResolvedPointLight light;

        private void setPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
