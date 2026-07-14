package net.mehvahdjukaar.polytone.common.expressions;

import net.mehvahdjukaar.polytone.Polytone;
import net.mehvahdjukaar.polytone.common.expressions.proxies.ParticleProxy;
import net.mehvahdjukaar.polytone.common.expressions.proxies.RandomProxy;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-thread reusable variable environment for a ticker's field expressions. Replaces a fresh
 * map build per expression (about 7x per particle tick); behavior matches a fresh map exactly.
 */
public final class ParticleExpEnv {

    private static final ThreadLocal<ParticleExpEnv> POOL = ThreadLocal.withInitial(ParticleExpEnv::new);

    private final Map<String, Object> vars = new HashMap<>();
    private final Set<String> baseKeys;

    private ParticleExpEnv() {
        ExpUtils.addStaticVars(vars);
        vars.put("random", RandomProxy.GLOBAL);
        vars.put("r", RandomProxy.GLOBAL);
        Set<String> keys = new HashSet<>(vars.keySet());
        keys.add("o");
        keys.add("object");
        this.baseKeys = Set.copyOf(keys);
    }

    /** Borrows this thread's environment. Safe with async ticking, each worker owns its own. */
    public static ParticleExpEnv get() {
        return POOL.get();
    }

    public Map<String, Object> prepare(Particle particle, Level level) {
        vars.keySet().retainAll(baseKeys); // drop variables assigned by the previous field
        Polytone.GLOBAL_EXPRESSION.addValues(vars); // dynamic globals refresh once per prepare
        // fresh proxy per field so its cached BlockPos sees position changes from earlier fields
        ParticleProxy obj = new ParticleProxy(particle, level);
        vars.put("o", obj);
        vars.put("object", obj);
        return vars;
    }
}
