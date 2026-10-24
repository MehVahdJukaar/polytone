package net.mehvahdjukaar.polytone.mixins;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.content.slotify.GuiDepthTarget;
import net.mehvahdjukaar.polytone.content.slotify.GuiDepthTargetAware;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
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
    public void polytone$renderInNode(GuiDepthTarget nodeTarget, Runnable renderFunction) {
        this.polytone$wantedNodeTarget = nodeTarget;
        GuiRenderState.Node lastCurrentNode = current;

        renderFunction.run();

        this.current = lastCurrentNode;
        this.polytone$wantedNodeTarget = null;
    }


    @Inject(method = "findAppropriateNode", at = @At("HEAD"))
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
        strataIndex = Math.clamp(strataIndex, 0, strata.size() - 1);
        GuiRenderState.Node current = strata.get(strataIndex);
        int depth = 0;

        // Traverse up to target node (if possible)
        while (depth < nodeIndex && current.up != null) {
            current = current.up;
            depth++;
        }
        /* Not possible anymore
        // Traverse down to target node (if possible)
        while (depth > nodeIndex && current.down != null) {
            current = current.down;
            depth--;
        }
*/
        // Check if current satisfies the predicate
        if (predicate.test(current)) {
            this.current = current;
            return current;
        }

        // Predicate failed, insert a new node using the public up() API
        if (addAbove) {
            GuiRenderState.Node savedUp = current.up;
            current.up = null; // force up() to create a fresh node
            ((GuiRenderState) (Object) this).up();
            GuiRenderState.Node newNode = this.current;
            newNode.up = savedUp; // restore chain
            return newNode;
        } else {
            Polytone.LOGGER.error("Can't insert below anymore!");
            return current;
        }
    }


}
