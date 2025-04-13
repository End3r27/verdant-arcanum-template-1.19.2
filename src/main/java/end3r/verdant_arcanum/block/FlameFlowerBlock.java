// FlameFlowerBlock.java
package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.registry.ModItems;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemConvertible;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.Random;


public class FlameFlowerBlock extends CropBlock {
    // Custom growth stages (0 = seed, 1 = bud, 2 = bloom)
    public static final int MAX_AGE = 2;
    public static final IntProperty AGE = Properties.AGE_2;

    // Tag for grove soil that this plant can grow on
    public static final TagKey<Block> GROVE_SOIL = TagKey.of(Registry.BLOCK_KEY,
            new Identifier("verdant_arcanum", "grove_soil"));

    public FlameFlowerBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(AGE, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public IntProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.FLAME_FLOWER_SEEDS;
    }

    // Fix: Updated method signature to match the superclass (ServerWorld -> World)
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (world.getLightLevel(pos.up()) >= 9) {
            int currentAge = getAge(state);
            if (currentAge < getMaxAge()) {
                float growthChance = getGrowthChance(world, pos);
                if (random.nextInt((int)(25.0F / growthChance) + 1) == 0) {
                    world.setBlockState(pos, state.with(AGE, currentAge + 1), 2);
                }
            }
        }
    }

    // Fix: Updated method signature to match the superclass
    // Changed from (Block block, BlockView world, BlockPos pos) to (World world, BlockPos pos)
    protected float getGrowthChance(World world, BlockPos pos) {
        float baseChance = 0.5f; // Base growth chance
        BlockPos soilPos = pos.down();
        BlockState soil = world.getBlockState(soilPos);

        // Check if planted on grove soil
        if (soil.isIn(GROVE_SOIL)) {
            baseChance *= 2.5f; // 250% growth speed on grove soil
        } else {
            baseChance *= 0.5f; // 50% growth speed on other soils
        }

        return baseChance;
    }

    // Override to only allow planting on grove soil
    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState soil = world.getBlockState(pos.down());
        return soil.isIn(GROVE_SOIL);
    }

    // Special effect when entities touch a fully grown flame flower
    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && getAge(state) == MAX_AGE && entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            // Apply fire resistance when touching a blooming flame flower
            livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 200, 0));
        }
        super.onEntityCollision(state, world, pos, entity);
    }
}