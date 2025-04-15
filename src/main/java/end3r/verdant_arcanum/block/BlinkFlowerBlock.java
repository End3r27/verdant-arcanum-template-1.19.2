package end3r.verdant_arcanum.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
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
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import end3r.verdant_arcanum.registry.ModItems;

public class BlinkFlowerBlock extends CropBlock {
    public static final int MAX_AGE = 2;
    public static final IntProperty AGE = IntProperty.of("age", 0, MAX_AGE);

    private static final VoxelShape[] AGE_TO_SHAPE = new VoxelShape[]{
            Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 6.0, 11.0),  // Age 0
            Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 8.0, 13.0),  // Age 1
            Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 12.0, 14.0)  // Age 2 (fully grown)
    };

    public BlinkFlowerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AGE, 0));
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.BLINK_FLOWER_SEEDS;
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

        // Special behavior: occasionally teleport nearby entities when mature
        if (state.get(AGE) == MAX_AGE && random.nextInt(20) == 0) {
            Box area = new Box(pos).expand(3.0);
            for (Entity entity : world.getOtherEntities(null, area)) {
                if (entity instanceof LivingEntity && random.nextInt(4) == 0) {
                    teleportRandomly(world, (LivingEntity)entity, random, 10.0);
                }
            }
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
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && state.get(AGE) > 0 && entity instanceof LivingEntity && world.random.nextInt(10) == 0) {
            // Chance to teleport entity when touched
            LivingEntity livingEntity = (LivingEntity) entity;
            teleportRandomly((ServerWorld)world, livingEntity, world.random, 5.0);
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
                ItemStack bloomStack = new ItemStack(ModItems.BLINK_FLOWER_BLOOM);
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