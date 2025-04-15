package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;

public class PlacedBloomBlock extends Block {
    // A smaller shape than a full block to represent the bloom
    protected static final VoxelShape SHAPE = Block.createCuboidShape(3.0, 0.0, 3.0, 13.0, 4.0, 13.0);

    public PlacedBloomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
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
