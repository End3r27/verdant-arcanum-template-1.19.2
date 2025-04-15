package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class RootgraspBloomBlock extends PlacedBloomBlock {
    public RootgraspBloomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;

            // Root the entity in place
            livingEntity.setVelocity(Vec3d.ZERO);
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 0));
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (random.nextInt(10) == 0) {
            world.getEntitiesByClass(LivingEntity.class,
                            net.minecraft.util.math.Box.of(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), 4, 2, 4),                            entity -> true)
                    .forEach(entity -> {
                        // Add slowness effect
                        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 0));

                        // Visual effect: make roots appear at entity's feet
                        if (random.nextInt(3) == 0) {
                            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                    entity.getX(), entity.getY(), entity.getZ(),
                                    10, 0.5, 0.1, 0.5, 0.02);
                        }
                    });
        }
    }
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos.down());
        // Allow placement on grass blocks or grove soil
        return blockState.isOf(Blocks.GRASS_BLOCK) ||
                blockState.isOf(ModBlocks.GROVE_SOIL) ||
                blockState.isIn(BlockTags.DIRT);
    }
}