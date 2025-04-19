package end3r.verdant_arcanum.block.tier2;

import end3r.verdant_arcanum.block.PlacedBloomBlock;
import end3r.verdant_arcanum.entity.MagicInfusedBee;
import end3r.verdant_arcanum.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class BreezevineBloomBlock extends PlacedBloomBlock {
    public BreezevineBloomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // Skip effects if the entity is a MagicInfusedBee
        if (entity instanceof MagicInfusedBee) {
            super.onEntityCollision(state, world, pos, entity);
            return;
        }

        if (!world.isClient && entity instanceof LivingEntity && world.getRandom().nextInt(5) == 0) {
            // Push the entity upward and away
            Vec3d entityPos = entity.getPos();
            Vec3d flowerPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            Vec3d pushDirection = entityPos.subtract(flowerPos).normalize();

            entity.setVelocity(
                    entity.getVelocity().x + pushDirection.x * 0.5,
                    entity.getVelocity().y + 0.2,
                    entity.getVelocity().z + pushDirection.z * 0.5
            );
            entity.velocityModified = true;

            // Visual and sound effect
            ((ServerWorld)world).spawnParticles(ParticleTypes.CLOUD,
                    entity.getX(), entity.getY(), entity.getZ(),
                    10, 0.2, 0.1, 0.2, 0.05);
            world.playSound(null, pos, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.BLOCKS, 0.5F, 1.8F);
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Occasionally create breeze effects
        if (random.nextInt(15) == 0) {
            createBreezeEffect(world, pos, random);
        }
    }

    private void createBreezeEffect(ServerWorld world, BlockPos pos, Random random) {
        // Create visual effect - particles
        world.spawnParticles(ParticleTypes.CLOUD,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20, 1.0, 0.5, 1.0, 0.1);

        // Play sound
        world.playSound(null, pos, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.BLOCKS, 1.0F, 1.8F);

        // Push entities away
        Box area = new Box(pos).expand(4.0);
        for (Entity entity : world.getOtherEntities(null, area)) {
            // Skip effects for MagicInfusedBee entities
            if (entity instanceof MagicInfusedBee) {
                continue;
            }

            if (entity instanceof LivingEntity) {
                // Calculate push direction - away from the flower
                Vec3d entityPos = entity.getPos();
                Vec3d flowerPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                Vec3d pushDirection = entityPos.subtract(flowerPos).normalize();

                // Push harder if entity is closer to flower
                double distance = entityPos.distanceTo(flowerPos);
                double pushStrength = Math.max(0.5, 2.0 - (distance / 4.0));

                // Apply push velocity
                Vec3d currentVelocity = entity.getVelocity();
                entity.setVelocity(
                        currentVelocity.x + pushDirection.x * pushStrength,
                        currentVelocity.y + 0.3 * pushStrength, // Push upward for more dramatic effect
                        currentVelocity.z + pushDirection.z * pushStrength
                );
                entity.velocityModified = true;
            }
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