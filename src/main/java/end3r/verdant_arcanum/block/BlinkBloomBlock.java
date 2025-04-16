package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.entity.MagicInfusedBee;
import end3r.verdant_arcanum.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class BlinkBloomBlock extends PlacedBloomBlock {
    public BlinkBloomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        // Skip effects if the entity is a MagicInfusedBee
        if (entity instanceof MagicInfusedBee) {
            super.onEntityCollision(state, world, pos, entity);
            return;
        }

        if (!world.isClient && entity instanceof LivingEntity && world.getRandom().nextInt(10) == 0) {
            // Chance to teleport entity when touched
            teleportRandomly((ServerWorld)world, (LivingEntity)entity, world.getRandom(), 5.0);
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Occasionally teleport nearby entities
        if (random.nextInt(20) == 0) {
            world.getEntitiesByClass(LivingEntity.class,
                            net.minecraft.util.math.Box.of(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), 4, 2, 4),
                            entity -> !(entity instanceof MagicInfusedBee)) // Filter out MagicInfusedBee entities
                    .forEach(entity -> {
                        if (random.nextInt(4) == 0) {
                            teleportRandomly(world, entity, random, 10.0);
                        }
                    });
        }
    }

    private void teleportRandomly(ServerWorld world, LivingEntity entity, Random random, double range) {
        // Calculate random position within range
        double x = entity.getX() + (random.nextDouble() - 0.5) * range * 2;
        double y = entity.getY();
        double z = entity.getZ() + (random.nextDouble() - 0.5) * range * 2;

        // Find valid position on ground
        BlockPos targetPos = new BlockPos(x, y, z);
        while (world.isAir(targetPos) && targetPos.getY() > 0) {
            targetPos = targetPos.down();
        }

        if (world.getBlockState(targetPos).getMaterial().blocksMovement()) {
            // Teleport to valid position above ground
            entity.teleport(x, targetPos.getY() + 1.0, z);
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.BLOCKS, 1.0F, 1.0F);
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