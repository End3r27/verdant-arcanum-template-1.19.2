package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.entity.MagicInfusedBee;
import end3r.verdant_arcanum.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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

public class SolarBloomBloomBlock extends PlacedBloomBlock {
    public SolarBloomBloomBlock(Settings settings) {
        super(settings);
    }

        @Override
        public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
            // Skip effects if the entity is a MagicInfusedBee
            if (entity instanceof MagicInfusedBee) {
                super.onEntityCollision(state, world, pos, entity);
                return;
            }

             if (!world.isClient && entity instanceof LivingEntity) {
             // Set the entity on fire when touching the placed flame bloom
                 entity.setOnFireFor(5); // Sets the entity on fire for 5 seconds
}
            super.onEntityCollision(state, world, pos, entity);
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