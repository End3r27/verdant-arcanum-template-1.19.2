package end3r.verdant_arcanum.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemConvertible;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import end3r.verdant_arcanum.registry.ModItems;

public class GustFlowerBlock extends CropBlock {
    public static final int MAX_AGE = 2;
    public static final IntProperty AGE = IntProperty.of("age", 0, MAX_AGE);

    private static final VoxelShape[] AGE_TO_SHAPE = new VoxelShape[]{
            Block.createCuboidShape(5.0, 0.0, 5.0, 11.0, 6.0, 11.0),  // Age 0
            Block.createCuboidShape(4.0, 0.0, 4.0, 12.0, 9.0, 12.0),  // Age 1
            Block.createCuboidShape(2.0, 0.0, 2.0, 14.0, 14.0, 14.0)  // Age 2 (fully grown)
    };

    public GustFlowerBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(AGE, 0));
    }

    @Override
    protected ItemConvertible getSeedsItem() {
        return ModItems.GUST_FLOWER_SEEDS;
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

        // Special behavior: mature plants produce occasional gusts of wind
        if (state.get(AGE) == MAX_AGE && random.nextInt(15) == 0) {
            createGustEffect(world, pos, random);
        }
    }

    private void createGustEffect(ServerWorld world, BlockPos pos, Random random) {
        // Create visual effect - particles
        world.spawnParticles(ParticleTypes.CLOUD,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20, 1.0, 0.5, 1.0, 0.1);

        // Play sound
        world.playSound(null, pos, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.BLOCKS, 1.0F, 1.5F);

        // Push entities away
        Box area = new Box(pos).expand(4.0);
        for (Entity entity : world.getOtherEntities(null, area)) {
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
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isClient && state.get(AGE) > 0 && entity instanceof LivingEntity) {
            int age = state.get(AGE);

            // More mature plants have stronger push effect
            if (age > 0 && world.random.nextInt(5) == 0) {
                // Push the entity upward and away
                Vec3d entityPos = entity.getPos();
                Vec3d flowerPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                Vec3d pushDirection = entityPos.subtract(flowerPos).normalize();

                // Push strength increases with age
                double pushStrength = 0.5 * age;

                entity.setVelocity(
                        entity.getVelocity().x + pushDirection.x * pushStrength,
                        entity.getVelocity().y + 0.2 * age,
                        entity.getVelocity().z + pushDirection.z * pushStrength
                );
                entity.velocityModified = true;

                // Visual and sound effect
                if (age == MAX_AGE) {
                    ((ServerWorld)world).spawnParticles(ParticleTypes.CLOUD,
                            entity.getX(), entity.getY(), entity.getZ(),
                            10, 0.2, 0.1, 0.2, 0.05);
                    world.playSound(null, pos, SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.BLOCKS, 0.5F, 1.5F);
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
}