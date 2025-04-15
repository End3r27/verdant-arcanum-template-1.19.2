package end3r.verdant_arcanum.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;

import end3r.verdant_arcanum.registry.ModItems;

public class RootgraspFlowerBlock extends CropBlock {
    public static final int MAX_AGE = 2;
    public static final IntProperty AGE = IntProperty.of("age", 0, MAX_AGE);

    private static final VoxelShape[] AGE_TO_SHAPE = new VoxelShape[]{
            Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 4.0, 11.0),  // Age 0
            Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 8.0, 13.0),  // Age 1
            Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 12.0, 15.0)  // Age 2 (fully grown)
    };

    public RootgraspFlowerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AGE, 0));
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.ROOTGRASP_FLOWER_SEEDS;
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return AGE_TO_SHAPE[state.get(AGE)];
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        // Check if the plant has good growing conditions
        if (world.getBaseLightLevel(pos, 0) >= 9 && canGrow(world, random, pos, state)) {
            int currentAge = state.get(AGE);
            if (currentAge < MAX_AGE && random.nextInt(5) == 0) {
                world.setBlockState(pos, state.with(AGE, currentAge + 1), Block.NOTIFY_LISTENERS);
            }
        }

        // Special behavior: when mature, check for entities to root
        if (state.get(AGE) == MAX_AGE && random.nextInt(10) == 0) {
            Box area = new Box(pos).expand(4.0);
            for (Entity entity : world.getOtherEntities(null, area)) {
                if (entity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) entity;
                    // Add slowness effect
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 1));

                    // Visual effect: make roots appear at entity's feet
                    if (random.nextInt(3) == 0) {
                        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                livingEntity.getX(), livingEntity.getY(), livingEntity.getZ(),
                                10, 0.5, 0.1, 0.5, 0.02);
                    }
                }
            }
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            int age = state.get(AGE);

            // Stronger effect as the plant grows
            if (age > 0) {
                // Root the entity in place
                livingEntity.setVelocity(Vec3d.ZERO);
                livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60 * age, age));

                // More mature plants also apply mining fatigue
                if (age == MAX_AGE) {
                    livingEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 1));
                }
            }
        }
        super.onEntityCollision(state, world, pos, entity);
    }

    @Override
    public boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
        Block block = floor.getBlock();
        // Can only be planted on Grove Soil
        return block instanceof GroveSoilBlock;
    }
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        int age = getAge(state);

        // Only harvest if the flower is fully grown
        if (age >= MAX_AGE) {
            // Drop bloom on right-click
            if (!world.isClient) {
                // Drop only the bloom item
                ItemStack bloomStack = new ItemStack(ModItems.ROOTGRASP_FLOWER_BLOOM);
                Block.dropStack(world, pos, bloomStack);

                // Reset to initial growth stage
                world.setBlockState(pos, state.with(AGE, 0), 2);

                // Play harvest sound
                world.playSound(null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 1.0F, 1.0F);
            }
            return ActionResult.success(world.isClient);
        }

        return ActionResult.PASS;
    }

}