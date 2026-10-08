package net.mehvahdjukaar.polytone.content.shaders;

import net.mehvahdjukaar.polytone.common.ClientFrameTicker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public class PolyGlobalUniforms implements IShaderModifier {

    public static final String PROJ_MAT = "PolyProjMat";
    public static final String MODEL_VIEW_MAT = "PolyModelViewMat";
    public static final String SUN_ANGLE = "PolySunAngle";
    public static final String DAY_TIME = "PolyDayTime";
    public static final String DELTA_TIME = "PolyDeltaTime";
    public static final String PLAYER_BLOCK_POS = "PolyPlayerBlockPos";
    public static final String PLAYER_OFFSET = "PolyPlayerOffset";

    private static final List<String> NAMES = List.of(PROJ_MAT, MODEL_VIEW_MAT, SUN_ANGLE, DAY_TIME, DELTA_TIME, PLAYER_BLOCK_POS, PLAYER_OFFSET);

    private final Matrix4f projMat = new Matrix4f();
    private final Matrix4f modelViewMat = new Matrix4f();
    private float sunAngle;
    private float dayTime;
    private float deltaTime;
    private BlockPos playerBlockPos = BlockPos.ZERO;
    private Vec3 playerOffset = Vec3.ZERO;

    public void capture(Matrix4f projection, Matrix4f modelView) {
        projMat.set(projection);
        modelViewMat.set(modelView);

        Minecraft mc = Minecraft.getInstance();
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(false);
        deltaTime = mc.getTimer().getGameTimeDeltaTicks();
        ClientLevel level = mc.level;
        if (level != null) {
            sunAngle = level.getSunAngle(partial) - Mth.HALF_PI;
            dayTime = (float) (ClientFrameTicker.getDayTime() % 24000);
        } else {
            sunAngle = 0;
            dayTime = 0;
        }

        Vec3 playerPos = mc.player == null ? Vec3.ZERO : mc.player.getPosition(partial);
        playerBlockPos = BlockPos.containing(playerPos);
        playerOffset = Vec3.atLowerCornerOf(playerBlockPos).subtract(playerPos);
    }

    @Override
    public List<String> getEnablingUniforms() {
        return NAMES;
    }

    @Override
    public void bindTo(IShader inputs) {
        inputs.getUniform(PROJ_MAT).set(projMat);
        inputs.getUniform(MODEL_VIEW_MAT).set(modelViewMat);
        inputs.getUniform(SUN_ANGLE).set(sunAngle);
        inputs.getUniform(DAY_TIME).set(dayTime);
        inputs.getUniform(DELTA_TIME).set(deltaTime);
        inputs.getUniform(PLAYER_BLOCK_POS).set(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ());
        inputs.getUniform(PLAYER_OFFSET).set((float) playerOffset.x, (float) playerOffset.y, (float) playerOffset.z);
    }
}
