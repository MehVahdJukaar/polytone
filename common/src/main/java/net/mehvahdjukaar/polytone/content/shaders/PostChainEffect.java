package net.mehvahdjukaar.polytone.content.shaders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.impl.ISimpleExp;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public final class PostChainEffect {

    public static final Codec<PostChainEffect> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Identifier.CODEC.fieldOf("post_chain").forGetter(p->p.postChain),
                    ISimpleExp.CODEC.optionalFieldOf("activation_condition", ISimpleExp.ONE).forGetter(p->p.turnOnCondition)
            ).apply(i, PostChainEffect::new));
    private final Identifier postChain;
    private final ISimpleExp turnOnCondition;

    private boolean cachedOn = false;
    private boolean cachedBroken = false;

    public PostChainEffect(Identifier postChain, ISimpleExp turnOnCondition) {
        this.postChain = postChain;
        this.turnOnCondition = turnOnCondition;
    }

    public void refreshEnabled() {
        cachedOn = turnOnCondition.evaluate() > 0;
    }

    @Nullable
    public PostChain getPostChain(ShaderManager manager) {
        if (cachedBroken) return null;
        if (!cachedOn) return null;

        try {
            return manager.getPostChain(postChain, LevelTargetBundle.MAIN_TARGETS);
        } catch (Throwable ex) {
            Polytone.LOGGER.error("Failed to load post chain", ex);
            cachedBroken = true;
            return null;
        }
    }
}
