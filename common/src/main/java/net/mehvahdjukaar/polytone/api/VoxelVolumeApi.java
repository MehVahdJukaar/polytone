package net.mehvahdjukaar.polytone.api;

import net.mehvahdjukaar.polytone.Polytone;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * One and only polytone api class
 */
public class VoxelVolumeApi {

    /**
     * callback runs on every resource pack reload
     */
    public static void registerCallback(Activation activation, Consumer<RegisterEvent> callback) {
        Polytone.VOXEL_VOLUME.registerCallback(activation, callback);
    }

    //re-runs the block entity callback at pos when its data changed without a block update. any thread
    public static void markDirty(BlockPos pos) {
        Polytone.VOXEL_VOLUME.markDirty(pos);
    }

    /**
     * This is important as any colored light activates the voxel volume system which is somehting expensive for just 1 light
     */
    public enum Activation {
        ALWAYS,
        ONLY_WITH_COLORED_LIGHT_PACKS
    }

    public static class RegisterEvent {

        /**
         * You will likely have no reason to use this unless a popular pack makes some flags popular. These are only really usable in post shaders
         */

        public void addVoxelFlags(Block block, String... flags) {
            //they end up in uniform names
            for (String f : flags) {
                f = f.toLowerCase(Locale.ROOT);
                //crash if invalid char
                ResourceLocation.parse(f);
                Polytone.VOXEL_VOLUME.addVoxelFlag(block, f);
            }
        }

        public <T extends BlockEntity> void addBlockEntityCallback(BlockEntityType<T> type, VoxelDataProvider<T> data) {
            Polytone.VOXEL_VOLUME.addBlockEntityData(type, data);
        }

        /**
         * Point lights here, all but blocks dont have occlusion. Dont add too many
         */

        public void addBlockLight(Block block, ResolvedPointLight light) {
            addBlockLight(block, state -> light);
        }

        // baked once per blockstate, null means no light
        public void addBlockLight(Block block, Function<BlockState, @Nullable ResolvedPointLight> light) {
            Polytone.COLORED_LIGHTS.addBlockLight(block, (state, pos, level, r) -> light.apply(state), AlwaysTrueTest.INSTANCE);
        }

        //copy an existing block, inherits what packs define. Use for block variants
        public void addBlockAlias(Block block, Block behavesAs) {
            addBlockAlias(block, behavesAs::withPropertiesOf);
        }

        public void addBlockAlias(Block block, Function<BlockState, BlockState> behavesAs) {
            Polytone.COLORED_LIGHTS.addBlockAlias(block, behavesAs);
        }

        public void addEntityAlias(EntityType<?> type, EntityType<?> behavesAs) {
            Polytone.COLORED_LIGHTS.addEntityAlias(type, behavesAs);
        }

        public void addItemAlias(Item item, Item behavesAs) {
            Polytone.COLORED_LIGHTS.addItemAlias(item, behavesAs);
        }

        public void addParticleAlias(ParticleType<?> type, ParticleType<?> behavesAs) {
            Polytone.COLORED_LIGHTS.addParticleAlias(type, behavesAs);
        }

        public void addEntityLight(EntityType<?> type, ResolvedPointLight light) {
            addEntityLight(type, e -> light);
        }

        //called every tick for each lit entity in view
        @SuppressWarnings("unchecked")
        public <T extends Entity> void addEntityLight(EntityType<T> type, Function<T, @Nullable ResolvedPointLight> light) {
            Polytone.COLORED_LIGHTS.addEntityLight(type, (entity, level, r) -> light.apply((T) entity));
        }

        public void addItemLight(Item item, ResolvedPointLight light) {
            addItemLight(item, (stack, holder) -> light);
        }

        //holder is the item entity or whoever holds it
        public void addItemLight(Item item, BiFunction<ItemStack, Entity, @Nullable ResolvedPointLight> light) {
            Polytone.COLORED_LIGHTS.addItemLight(item, (stack, holder, level, r) -> light.apply(stack, holder));
        }

        public void addParticleLight(ParticleType<?> type, ResolvedPointLight light) {
            addParticleLight(type, p -> light);
        }

        public void addParticleLight(ParticleType<?> type, Function<Particle, @Nullable ResolvedPointLight> light) {
            Polytone.COLORED_LIGHTS.addParticleLight(type, (particle, level, r) -> light.apply(particle));
        }
    }
}
