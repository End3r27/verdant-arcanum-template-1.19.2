package end3r.verdant_arcanum.event;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class OvergrowthEvent implements CustomWorldEvent {
    private static final Identifier ID = new Identifier("verdant_arcanum", "overgrowth");
    private static final int EVENT_DURATION = 20 * 60 * 5; // 5 minutes
    private static final int BLOCKS_PER_TICK = 25;
    private static final int GROWTH_TICK_INTERVAL = 20; // Apply growth every second
    
    private int ticksRemaining = EVENT_DURATION;
    private int growthCounter = 0;
    
    // Map for block transformations
    private static final Map<Block, Block> MOSS_TRANSFORMATIONS = new HashMap<>();
    
    static {
        MOSS_TRANSFORMATIONS.put(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
        MOSS_TRANSFORMATIONS.put(Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
        MOSS_TRANSFORMATIONS.put(Blocks.COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE_WALL);
        MOSS_TRANSFORMATIONS.put(Blocks.STONE_BRICK_WALL, Blocks.MOSSY_STONE_BRICK_WALL);
        MOSS_TRANSFORMATIONS.put(Blocks.COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE_STAIRS);
        MOSS_TRANSFORMATIONS.put(Blocks.STONE_BRICK_STAIRS, Blocks.MOSSY_STONE_BRICK_STAIRS);
        MOSS_TRANSFORMATIONS.put(Blocks.COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE_SLAB);
        MOSS_TRANSFORMATIONS.put(Blocks.STONE_BRICK_SLAB, Blocks.MOSSY_STONE_BRICK_SLAB);
        // Add more transformations as needed
    }

    @Override
    public void start(ServerWorld world) {
        ticksRemaining = EVENT_DURATION;
        world.getPlayers().forEach(player -> 
            player.sendMessage(net.minecraft.text.Text.literal("Nature's power surges! An Overgrowth event has begun."), true)
        );
    }

    @Override
    public void tick(ServerWorld world) {
        ticksRemaining--;
        
        // Create ambient particles
        if (world.getRandom().nextFloat() < 0.3f) {
            world.getPlayers().forEach(player -> {
                BlockPos playerPos = player.getBlockPos();
                for (int i = 0; i < 5; i++) {
                    double x = playerPos.getX() + world.getRandom().nextGaussian() * 8;
                    double y = playerPos.getY() + world.getRandom().nextGaussian() * 4;
                    double z = playerPos.getZ() + world.getRandom().nextGaussian() * 8;
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
                }
            });
        }
        
        // Accelerate crop growth and moss transformation
        growthCounter++;
        if (growthCounter >= GROWTH_TICK_INTERVAL) {
            growthCounter = 0;
            applyOvergrowthEffects(world);
        }
        
        // Notify players when the event is halfway through and near completion
        if (ticksRemaining == EVENT_DURATION / 2) {
            world.getPlayers().forEach(player -> 
                player.sendMessage(net.minecraft.text.Text.literal("The Overgrowth continues to spread..."), true)
            );
        } else if (ticksRemaining == 200) { // 10 seconds remaining
            world.getPlayers().forEach(player -> 
                player.sendMessage(net.minecraft.text.Text.literal("The Overgrowth is beginning to subside..."), true)
            );
        }
    }

    private void applyOvergrowthEffects(ServerWorld world) {
        // Process loaded chunks around players
        for (var player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();
            ChunkPos chunkPos = new ChunkPos(playerPos);
            
            List<WorldChunk> nearbyChunks = new ArrayList<>();
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    WorldChunk chunk = world.getChunk(chunkPos.x + x, chunkPos.z + z);
                    if (chunk != null) {
                        nearbyChunks.add(chunk);
                    }
                }
            }
            
            // Select random blocks from nearby chunks
            Random random = world.getRandom();
            for (int i = 0; i < BLOCKS_PER_TICK; i++) {
                if (nearbyChunks.isEmpty()) continue;
                
                WorldChunk chunk = nearbyChunks.get(random.nextInt(nearbyChunks.size()));
                int x = random.nextInt(16);
                int z = random.nextInt(16);
                int y = 50 + random.nextInt(100); // Adjust y-range as needed
                
                BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                
                // Try to find a valid block
                if (world.isChunkLoaded(pos)) {
                    BlockState state = world.getBlockState(pos);
                    
                    // Accelerate crop growth
                    if (state.getBlock() instanceof CropBlock) {
                        CropBlock crop = (CropBlock) state.getBlock();
                        if (!crop.isMature(state)) {
                            int age = state.get(CropBlock.AGE);
                            if (random.nextFloat() < 0.7f) { // 70% chance to grow
                                world.setBlockState(pos, state.with(CropBlock.AGE, Math.min(age + 1, crop.getMaxAge())));
                            }
                        }
                    } 
                    // Transform saplings
                    else if (state.getBlock() instanceof SaplingBlock) {
                        SaplingBlock sapling = (SaplingBlock) state.getBlock();
                        if (random.nextFloat() < 0.4f) { // 40% chance to grow
                            sapling.grow(world, world.getRandom(), pos, state);
                        }
                    }
                    // Apply moss transformations
                    else if (MOSS_TRANSFORMATIONS.containsKey(state.getBlock())) {
                        if (random.nextFloat() < 0.15f) { // 15% chance to transform
                            Block mossyVariant = MOSS_TRANSFORMATIONS.get(state.getBlock());
                            world.setBlockState(pos, mossyVariant.getDefaultState());
                            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, 
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                                5, 0.25, 0.25, 0.25, 0.01);
                        }
                    }
                    // Spread moss to dirt and stone
                    else if ((state.getBlock() == Blocks.DIRT || state.getBlock() == Blocks.STONE) 
                            && world.getBlockState(pos.up()).isAir()) {
                        if (random.nextFloat() < 0.05f) { // 5% chance
                            world.setBlockState(pos, Blocks.MOSS_BLOCK.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isComplete() {
        return ticksRemaining <= 0;
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}