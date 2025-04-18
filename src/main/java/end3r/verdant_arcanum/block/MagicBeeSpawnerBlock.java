package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.block.entity.MagicBeeSpawnerBlockEntity;
import end3r.verdant_arcanum.registry.ModBlockEntities;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MagicBeeSpawnerBlock extends BlockWithEntity {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public MagicBeeSpawnerBlock(Settings settings) {
        super(settings);
        // Set default state with facing direction
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        // Set facing direction based on player placement
        return this.getDefaultState().with(FACING, ctx.getPlayerFacing().getOpposite());
    }

    // Make this block use a BlockEntity
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MagicBeeSpawnerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.MAGIC_BEE_SPAWNER_ENTITY, MagicBeeSpawnerBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MagicBeeSpawnerBlockEntity) {
                MagicBeeSpawnerBlockEntity spawner = (MagicBeeSpawnerBlockEntity) blockEntity;

                // Get the current bee count in the area
                int currentBeeCount = spawner.countNearbyBees();

                // Provide feedback to the player
                player.sendMessage(Text.literal("Magic Infused Bees in range: " + currentBeeCount + "/10"), true);

                // Add magical particles when the player interacts with the spawner
                if (world instanceof ServerWorld) {
                    ServerWorld serverWorld = (ServerWorld) world;
                    serverWorld.spawnParticles(
                            net.minecraft.particle.ParticleTypes.ENCHANT,
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                            20,  // number of particles
                            0.5, 0.5, 0.5,  // spread
                            0.1  // speed
                    );
                }

                return ActionResult.SUCCESS;
            }
        }

        return ActionResult.CONSUME;
    }
}