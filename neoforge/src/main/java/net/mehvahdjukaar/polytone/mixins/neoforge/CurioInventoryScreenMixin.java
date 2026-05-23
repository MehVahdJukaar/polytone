package net.mehvahdjukaar.polytone.mixins.neoforge;

//TODO: add later when curio ports
/*
import top.theillusivec4.curios.client.gui.CuriosScreen;

@Pseudo
@Mixin(CuriosScreen.class)
public abstract class CurioInventoryScreenMixin {

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 1
    )
    public int polytone$modifyRenderEntityX(int x) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return x + s.x();
            }
        }
        return x;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 2
    )
    public int polytone$modifyRenderEntityX2(int x) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return x + s.x();
            }
        }
        return x;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 2
    )
    public int poltone$modifyRenderEntityY(int y) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return y + s.y();
            }
        }
        return y;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 4
    )
    public int poltone$modifyRenderEntityY2(int y) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return y + s.y();
            }
        }
        return y;
    }

    @ModifyArg(method = "renderBg", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/InventoryScreen;renderEntityInInventoryFollowsMouse(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIIIFFFLnet/minecraft/world/entity/LivingEntity;)V")
            , index = 4
    )
    public int polytone$modifyRenderEntityScale(int y) {
        var m = ((SlotifyScreen) this).polytone$getModifier();
        if (m != null) {
            var s = m.getSpecial("player");
            if (s != null) {
                return y + s.z();
            }
        }
        return y;
    }



}*/
