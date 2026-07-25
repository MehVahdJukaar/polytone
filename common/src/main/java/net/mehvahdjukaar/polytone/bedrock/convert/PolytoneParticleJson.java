package net.mehvahdjukaar.polytone.bedrock.convert;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.polytone.content.particle.custom.CustomParticleType;
import net.mehvahdjukaar.polytone.content.particle.custom.ParticleRenderMode;

/**
 * Builds a {@code polytone/custom_particles/*.json}. Keeping the field names in one class means the
 * converter never spells them out, and {@link #validate()} runs the result back through the real
 * loading codec - the whole reason this importer lives in the mod instead of in a separate tool.
 */
public class PolytoneParticleJson {

    private final JsonObject root = new JsonObject();
    private final JsonObject initializer = new JsonObject();
    private final JsonObject ticker = new JsonObject();
    private final JsonArray particleEmitters = new JsonArray();
    private final JsonArray soundEmitters = new JsonArray();

    public PolytoneParticleJson renderMode(ParticleRenderMode mode) {
        root.addProperty("render_type", mode.getSerializedName());
        return this;
    }

    public PolytoneParticleJson rotationMode(String mode) {
        root.addProperty("rotation_mode", mode);
        return this;
    }

    public PolytoneParticleJson hasPhysics(boolean value) {
        root.addProperty("has_physics", value);
        return this;
    }

    public PolytoneParticleJson killOnContact(boolean value) {
        root.addProperty("kill_on_contact", value);
        return this;
    }

    public PolytoneParticleJson killWhenStill(boolean value) {
        root.addProperty("kill_when_still", value);
        return this;
    }

    public PolytoneParticleJson killWhenNotInView(boolean value) {
        root.addProperty("kill_when_not_in_view", value);
        return this;
    }

    public PolytoneParticleJson forceSpawn(boolean value) {
        root.addProperty("force_spawn", value);
        return this;
    }

    public PolytoneParticleJson randomSprite(boolean value) {
        root.addProperty("random_sprite", value);
        return this;
    }

    public PolytoneParticleJson liquidAffinity(String value) {
        root.addProperty("liquid_affinity", value);
        return this;
    }

    public PolytoneParticleJson limit(int value) {
        root.addProperty("limit", value);
        return this;
    }

    /** A field of the {@code initializer}: evaluated once, when the particle is born. */
    public PolytoneParticleJson init(String key, String expression) {
        initializer.addProperty(key, expression);
        return this;
    }

    /** A field of the {@code ticker}: re-evaluated every tick. */
    public PolytoneParticleJson tick(String key, String expression) {
        ticker.addProperty(key, expression);
        return this;
    }

    public PolytoneParticleJson emitter(JsonObject emitter) {
        particleEmitters.add(emitter);
        return this;
    }

    public PolytoneParticleJson soundEmitter(JsonObject emitter) {
        soundEmitters.add(emitter);
        return this;
    }

    public boolean hasEmitters() {
        return !particleEmitters.isEmpty();
    }

    public JsonObject build() {
        JsonObject out = root.deepCopy();
        if (!initializer.isEmpty()) out.add("initializer", initializer.deepCopy());
        if (!ticker.isEmpty()) out.add("ticker", ticker.deepCopy());
        if (!particleEmitters.isEmpty()) out.add("particle_emitters", particleEmitters.deepCopy());
        if (!soundEmitters.isEmpty()) out.add("sound_emitters", soundEmitters.deepCopy());
        return out;
    }

    /** Decodes what we just built with the codec that will load it in game. */
    public DataResult<CustomParticleType> validate() {
        return CustomParticleType.CODEC.parse(JsonOps.INSTANCE, build());
    }
}
