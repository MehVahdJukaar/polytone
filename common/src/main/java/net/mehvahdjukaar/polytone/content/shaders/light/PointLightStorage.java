package net.mehvahdjukaar.polytone.content.shaders.light;

// keys are the lit thing itself: an Entity, a Particle, a BlockPos
public interface PointLightStorage {

    void set(Object key, double x, double y, double z, ResolvedPointLight light);

    void move(Object key, double x, double y, double z);

    void remove(Object key);

    void clear();
}
