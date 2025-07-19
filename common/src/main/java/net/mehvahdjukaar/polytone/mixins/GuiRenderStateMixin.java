package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.utils.GuiDepthTarget;
import net.mehvahdjukaar.polytone.utils.GuiDepthTargetAware;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.ScreenArea;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(GuiRenderState.class)
public abstract class GuiRenderStateMixin implements GuiDepthTargetAware {

    @Shadow
    private GuiRenderState.Node current;
    @Shadow
    @Final
    private List<GuiRenderState.Node> strata;

    @Shadow
    protected abstract boolean hasIntersection(ScreenRectangle screenRectangle, @Nullable List<? extends ScreenArea> list);

    @Unique
    private @Nullable GuiDepthTarget polytone$wantedNodeTarget;

    @Override
    public void renderInNode(GuiDepthTarget nodeTarget, Runnable renderFunction) {
        this.polytone$wantedNodeTarget = nodeTarget;
        GuiRenderState.Node lastCurrentNode = current;

        renderFunction.run();

        this.current = lastCurrentNode;
        this.polytone$wantedNodeTarget = null;
    }


    @Inject(method = "findAppropriateNode", at = @At("HEAD"), cancellable = true)
    public void polytone$findOrSqueezeInNewNode(ScreenArea screenArea, CallbackInfoReturnable<Boolean> cir) {
        if (polytone$wantedNodeTarget == null) return;
        ScreenRectangle screenRectangle = screenArea.bounds();
        if (screenRectangle == null) {
            return;
        }

        this.current = polytone$selectOrAddNewNode(polytone$wantedNodeTarget.strata(),
                polytone$wantedNodeTarget.node(), polytone$wantedNodeTarget.addAbove(), node ->
                        !(this.hasIntersection(screenRectangle, node.elementStates) ||
                                this.hasIntersection(screenRectangle, node.itemStates) ||
                                this.hasIntersection(screenRectangle, node.textStates) ||
                                this.hasIntersection(screenRectangle, node.picturesInPictureStates)
                        ));


    }

    @Unique
    private GuiRenderState.Node polytone$selectOrAddNewNode(int strataIndex, int nodeIndex, boolean addAbove,
                                                            Predicate<GuiRenderState.Node> predicate) {
        // Clamp strata index
        strataIndex = Math.max(0, Math.min(strataIndex, strata.size() - 1));
        GuiRenderState.Node current = strata.get(strataIndex);
        int depth = 0;

        // Traverse up to target node (if possible)
        while (depth < nodeIndex && current.up != null) {
            current = current.up;
            depth++;
        }

        // Traverse down to target node (if possible)
        while (depth > nodeIndex && current.down != null) {
            current = current.down;
            depth--;
        }

        // Check if current satisfies the predicate
        if (predicate.test(current)) {
            this.current = current;
            return current;
        }

        // Predicate failed, insert a new node
        GuiRenderState.Node newNode = new GuiRenderState.Node(current);

        if (addAbove) {
            // Insert above
            newNode.down = current;
            newNode.up = current.up;
            if (current.up != null) {
                current.up.down = newNode;
            }
            current.up = newNode;
        } else {
            // Insert below
            newNode.up = current;
            newNode.down = current.down;
            if (current.down != null) {
                current.down.up = newNode;
            }
            current.down = newNode;
        }

        this.current = newNode;
        return newNode;
    }


}
