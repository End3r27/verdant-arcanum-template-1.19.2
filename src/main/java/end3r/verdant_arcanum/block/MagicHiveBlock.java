package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.block.entity.MagicHiveBlockEntity;
import end3r.verdant_arcanum.registry.ModBlockEntities;
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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MagicHiveBlock extends BlockWithEntity {

    public MagicHiveBlock(Settings settings) {
        super(settings);
    }

    // This creates and returns a new block entity for this block
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MagicHiveBlockEntity(pos, state);
    }

    // This returns the render type for the block
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // BlockWithEntity defaults to INVISIBLE, which we don't want
        return BlockRenderType.MODEL;
    }

    // Handle right clicks on the block
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            // This will open the UI when a player right-clicks the block
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);

            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }

        return ActionResult.SUCCESS;
    }

    // Handle block breaking
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MagicHiveBlockEntity) {
                ItemScatterer.spawn(world, pos, (MagicHiveBlockEntity)blockEntity);
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    // This method is called to set up the ticker for the block entity
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModBlockEntities.MAGIC_HIVE_ENTITY, MagicHiveBlockEntity::tick);
    }

    // Method called by the Magic Bee to deposit an essence
    public boolean tryDepositEssence(World world, BlockPos pos, BlockState state, Item essenceType) {
        if (world.isClient()) return false;

        // Get the block entity
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof MagicHiveBlockEntity)) {
            // Debug - wrong block entity type
            // System.out.println("Failed to deposit essence: Not a Magic Hive block entity");
            return false;
        }

        MagicHiveBlockEntity hiveEntity = (MagicHiveBlockEntity) be;
        boolean added = hiveEntity.addEssence(new ItemStack(essenceType, 1));

        if (added) {
            // Play a sound effect for feedback
            world.playSound(null, pos, SoundEvents.BLOCK_BEEHIVE_WORK, SoundCategory.BLOCKS, 1.0F, 1.0F);

            // Debug - successful deposit
            // System.out.println("Successfully deposited " + essenceType + " in hive at " + pos);
            return true;
        }

        // Debug - failed to add essence
        // System.out.println("Failed to deposit essence: Hive inventory might be full");
        return false;
    }
}