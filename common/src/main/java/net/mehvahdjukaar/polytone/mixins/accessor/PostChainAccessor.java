package net.mehvahdjukaar.polytone.mixins.accessor;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

// the external targets the chain actually reads, not the permitted set
@Mixin(PostChain.class)
public interface PostChainAccessor {

    @Accessor("externalTargets")
    Set<Identifier> polytone$getExternalTargets();
}
