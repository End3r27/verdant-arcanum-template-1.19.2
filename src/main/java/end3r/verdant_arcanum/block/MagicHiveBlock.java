package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.block.entity.MagicHiveBlockEntity;
import end3r.verdant_arcanum.registry.ModBlockEntities;
import end3r.verdant_arcanum.registry.ModItems;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MagicHiveBlock extends BlockWithEntity {
    // Property to track the honey level (0-5)
    public static final IntProperty HONEY_LEVEL = IntProperty.of("honey_level", 0, 5);

    // Shape of the hive block
    private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);

    public MagicHiveBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(HONEY_LEVEL, 0));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HONEY_LEVEL);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    // Handle right-click interactions
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            // Open the GUI for the hive
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
                world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_ENTER, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
        }
        return ActionResult.SUCCESS;
    }

    // Try to deposit an essence item into the hive
    public boolean tryDepositEssence(World world, BlockPos pos, BlockState state, Item essence) {
        if (world.isClient) return false;

        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof MagicHiveBlockEntity) {
            MagicHiveBlockEntity hive = (MagicHiveBlockEntity) blockEntity;

            // Try to add the essence to the inventory
            boolean success = hive.addEssence(new ItemStack(essence, 1));
            if (success) {
                // Update the honey level based on total essences
                int totalEssences = hive.getEssenceCount();
                int newLevel = Math.min(5, totalEssences / 3); // Every 3 essences increases the level

                if (newLevel != state.get(HONEY_LEVEL)) {
                    world.setBlockState(pos, state.with(HONEY_LEVEL, newLevel));
                }

                // Play a sound effect
                world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.BLOCKS, 1.0f, 1.0f);
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MagicHiveBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.MAGIC_HIVE_ENTITY, MagicHiveBlockEntity::tick);
    }
}