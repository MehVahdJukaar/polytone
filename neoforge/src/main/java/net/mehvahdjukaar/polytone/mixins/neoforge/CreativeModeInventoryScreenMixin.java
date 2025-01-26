package net.mehvahdjukaar.polytone.mixins.forge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(CreativeModeInventoryScreen.class)
public class CreativeModeInventoryScreenMixin {

    @ModifyExpressionValue(method = "init", at  = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/CreativeModeTabRegistry;getSortedCreativeModeTabs()Ljava/util/List;"))
    public List<CreativeModeTab> polytone$removeEmptyTabs(List<CreativeModeTab> original){
        return original.stream().filter(CreativeModeTab::shouldDisplay).toList();
    }
}
