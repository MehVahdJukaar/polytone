package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.Identifier;

public record PostChainEffect (Identifier postChain)  {

    public static final Codec<PostChainEffect> CODEC = Identifier.CODEC.xmap(
              PostChainEffect::new, PostChainEffect::postChain
    ).fieldOf("post_chain").codec();
}
