package end3r.verdant_arcanum.event;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.item.ItemStack;
import end3r.verdant_arcanum.registry.ModItems;

import java.util.*;

public class OvergrowthEvent implements CustomWorldEvent {
    public static final Identifier ID = new Identifier("verdant_arcanum", "overgrowth");
    private static final int DEFAULT_EVENT_DURATION = 20 * 60 * 5; // 5 minutes
    private static final int BLOCKS_PER_TICK = 25;
    private static final int GROWTH_TICK_INTERVAL = 20; // Apply growth every second
    
    private int ticksRemaining = DEFAULT_EVENT_DURATION;
    private int growthCounter = 0;
    private int intensity = 2; // Default medium intensity (1=mild, 2=moderate, 3=intense)
    
    // Map for block transformations
    private static final Map<Block, Block> MOSS_TRANSFORMATIONS = new HashMap<>();
    
    // Rootgrasp seed particle configuration
    private static final Vec3f ROOTGRASP_SEED_PARTICLE_COLOR = new Vec3f(0.2f, 0.8f, 0.2f); // Green color
    private static final float ROOTGRASP_SEED_PARTICLE_SIZE = 1.2f;
    private static final int ROOTGRASP_SEED_SPAWN_CHANCE = 15; // 1 in X chance per spawn cycle
    private static final double ROOTGRASP_SEED_COLLECTION_RADIUS = 1.0; // Player must be this close to collect
    
    // Track active rootgrasp seed particles - stores BlockPos to ensure they stay on the ground
    private final Map<UUID, BlockPos> activeRootgraspSeedParticles = new HashMap<>();
    
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

    /**
     * Set the duration of the overgrowth event in ticks
     * @param ticks Duration in ticks (20 ticks = 1 second)
     */
    public void setDuration(int ticks) {
        this.ticksRemaining = Math.max(20, ticks); // Minimum 1 second
    }
    
    /**
     * Set the intensity of the overgrowth event
     * @param intensity 1=mild, 2=moderate, 3=intense
     */
    public void setIntensity(int intensity) {
        this.intensity = Math.max(1, Math.min(3, intensity)); // Clamp between 1-3
    }

    @Override
    public void start(ServerWorld world) {
        // Adjust message based on intensity
        String intensityDesc;
        switch (intensity) {
            case 1:
                intensityDesc = "Mild";
                break;
            case 3:
                intensityDesc = "Intense";
                break;
            default:
                intensityDesc = "Strong";
                break;
        }
        
        world.getPlayers().forEach(player -> 
            player.sendMessage(net.minecraft.text.Text.literal("🌱 " + intensityDesc + " nature's power surges! An Overgrowth event has begun."), true)
        );
    }

    @Override
    public void tick(ServerWorld world) {
        ticksRemaining--;
        
        // Adjust particle frequency based on intensity
        float particleChance = 0.1f + (intensity * 0.1f); // 0.2 for mild, 0.3 for moderate, 0.4 for intense
        
        // Create ambient particles
        if (world.getRandom().nextFloat() < particleChance) {
            world.getPlayers().forEach(player -> {
                BlockPos playerPos = player.getBlockPos();
                // Number of particles scales with intensity
                int particleCount = 2 + intensity;
                for (int i = 0; i < particleCount; i++) {
                    double x = playerPos.getX() + world.getRandom().nextGaussian() * 8;
                    double y = playerPos.getY() + world.getRandom().nextGaussian() * 4;
                    double z = playerPos.getZ() + world.getRandom().nextGaussian() * 8;
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
                }
            });
        }
        
        // Spawn rootgrasp seed particles - frequency increases with intensity
        if (ticksRemaining % (60 - (intensity * 15)) == 0) { // 45, 30, or 15 ticks based on intensity
            spawnRootgraspSeedParticles(world);
        }
        
        // Check for players collecting rootgrasp seed particles
        checkRootgraspSeedCollection(world);
        
        // Accelerate crop growth and moss transformation
        growthCounter++;
        // Growth tick interval decreases with intensity (faster growth at higher intensity)
        int adjustedGrowthInterval = Math.max(5, GROWTH_TICK_INTERVAL - ((intensity - 1) * 5));
        if (growthCounter >= adjustedGrowthInterval) {
            growthCounter = 0;
            applyOvergrowthEffects(world);
        }
        
        // Notify players when the event is halfway through and near completion
        if (ticksRemaining == DEFAULT_EVENT_DURATION / 2 && ticksRemaining > 600) {
            world.getPlayers().forEach(player -> 
                player.sendMessage(net.minecraft.text.Text.literal("The Overgrowth continues to spread..."), true)
            );
        } else if (ticksRemaining == 200) { // 10 seconds remaining
            world.getPlayers().forEach(player -> 
                player.sendMessage(net.minecraft.text.Text.literal("The Overgrowth is beginning to subside..."), true)
            );
        }
    }

    /**
     * Spawn special green particles that represent rootgrasp seeds on the ground
     */
    private void spawnRootgraspSeedParticles(ServerWorld world) {
        // Cap the maximum number of active rootgrasp seed particles - scales with intensity
        int maxParticles = 5 + (intensity * 5);
        if (activeRootgraspSeedParticles.size() >= maxParticles) {
            return;
        }

        // Generate particles around players - chance improves with intensity
        int adjustedSpawnChance = Math.max(5, ROOTGRASP_SEED_SPAWN_CHANCE - ((intensity - 1) * 5));

        // Generate particles around players
        for (PlayerEntity player : world.getPlayers()) {
            // Skip if random chance fails
            if (world.getRandom().nextInt(adjustedSpawnChance) != 0) {
                continue;
            }

            BlockPos playerPos = player.getBlockPos();

            // Generate particle at a random position around the player
            int radius = 10 + world.getRandom().nextInt(10); // 10-20 blocks away
            int offsetX = world.getRandom().nextInt(radius * 2) - radius;
            int offsetZ = world.getRandom().nextInt(radius * 2) - radius;

            // Find the top solid block at this x,z position
            BlockPos groundPos = findGroundPos(world, playerPos.add(offsetX, 0, offsetZ));
            
            // Skip if no suitable ground was found
            if (groundPos == null) {
                continue;
            }

            // Create a UUID for this particle to track it
            UUID particleId = UUID.randomUUID();
            activeRootgraspSeedParticles.put(particleId, groundPos);

            // Spawn visible green particle slightly above the ground
            Vec3d particlePos = new Vec3d(
                groundPos.getX() + 0.5,
                groundPos.getY() + 0.1, // Just above the ground
                groundPos.getZ() + 0.5
            );

            world.spawnParticles(
                // Green colored dust particle
                new DustParticleEffect(ROOTGRASP_SEED_PARTICLE_COLOR, ROOTGRASP_SEED_PARTICLE_SIZE),
                particlePos.x, particlePos.y, particlePos.z,
                1, // count - just one per location
                0.0, 0.0, 0.0, // no random motion
                0 // speed factor
            );

            // Add "glittering" effect around the main particle
            for (int i = 0; i < 5; i++) {
                world.spawnParticles(
                    new DustParticleEffect(new Vec3f(0.1f, 0.9f, 0.1f), 0.5f),
                    particlePos.x + (world.getRandom().nextDouble() - 0.5) * 0.3,
                    particlePos.y + world.getRandom().nextDouble() * 0.3,
                    particlePos.z + (world.getRandom().nextDouble() - 0.5) * 0.3,
                    1,
                    (world.getRandom().nextDouble() - 0.5) * 0.01,
                    world.getRandom().nextDouble() * 0.05,
                    (world.getRandom().nextDouble() - 0.5) * 0.01,
                    0.01f
                );
            }
            
            // Add small root-like particles coming out of the ground
            for (int i = 0; i < 3; i++) {
                double rootX = particlePos.x + (world.getRandom().nextDouble() - 0.5) * 0.5;
                double rootZ = particlePos.z + (world.getRandom().nextDouble() - 0.5) * 0.5;
                
                for (int j = 0; j < 3; j++) {
                    world.spawnParticles(
                        new DustParticleEffect(new Vec3f(0.3f, 0.7f, 0.3f), 0.3f),
                        rootX, particlePos.y + (j * 0.1), rootZ,
                        1,
                        0.0, 0.01, 0.0,
                        0.0f
                    );
                }
            }
        }
    }

    /**
     * Find the top solid block at a given x,z position
     */
    private BlockPos findGroundPos(ServerWorld world, BlockPos startPos) {
        // Search from player Y level down to find ground
        int startY = startPos.getY();
        int minY = Math.max(world.getBottomY(), startY - 10);
        int maxY = Math.min(world.getTopY(), startY + 10);
        
        for (int y = startY; y >= minY; y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());
            
            // Skip if the chunk isn't loaded
            if (!world.isChunkLoaded(checkPos)) {
                return null;
            }
            
            BlockState state = world.getBlockState(checkPos);
            
            // Check if this block is solid and the block above is air
            if (state.isFullCube(world, checkPos) && 
                !state.isOf(Blocks.WATER) && 
                !state.isOf(Blocks.LAVA) &&
                world.getBlockState(checkPos.up()).isAir()) {
                
                return checkPos;
            }
        }
        
        // No suitable ground found
        return null;
    }

    /**
     * Check if players are standing on rootgrasp seed particles to collect them
     */
    private void checkRootgraspSeedCollection(ServerWorld world) {
        // Skip if no particles or no players
        if (activeRootgraspSeedParticles.isEmpty() || world.getPlayers().isEmpty()) {
            return;
        }

        // Tick each particle
        Iterator<Map.Entry<UUID, BlockPos>> iterator = activeRootgraspSeedParticles.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BlockPos> entry = iterator.next();
            UUID particleId = entry.getKey();
            BlockPos seedPos = entry.getValue();
            
            // Generate particle effects every tick to make it more visible
            if (ticksRemaining % 5 == 0) {
                Vec3d particlePos = new Vec3d(
                    seedPos.getX() + 0.5,
                    seedPos.getY() + 0.1,
                    seedPos.getZ() + 0.5
                );
                
                // Main particle
                world.spawnParticles(
                    new DustParticleEffect(ROOTGRASP_SEED_PARTICLE_COLOR, ROOTGRASP_SEED_PARTICLE_SIZE),
                    particlePos.x, particlePos.y, particlePos.z,
                    1,
                    0.0, 0.0, 0.0,
                    0
                );
                
                // Small "pulsing" particles
                if (world.getRandom().nextBoolean()) {
                    for (int i = 0; i < 2; i++) {
                        world.spawnParticles(
                            new DustParticleEffect(new Vec3f(0.1f, 0.9f, 0.1f), 0.5f),
                            particlePos.x + (world.getRandom().nextDouble() - 0.5) * 0.3,
                            particlePos.y + world.getRandom().nextDouble() * 0.2,
                            particlePos.z + (world.getRandom().nextDouble() - 0.5) * 0.3,
                            1,
                            (world.getRandom().nextDouble() - 0.5) * 0.01,
                            world.getRandom().nextDouble() * 0.03,
                            (world.getRandom().nextDouble() - 0.5) * 0.01,
                            0.01f
                        );
                    }
                }
            }

            // Randomly remove particles over time (20% chance every 5 seconds)
            if (ticksRemaining % 100 == 0 && world.getRandom().nextFloat() < 0.2f) {
                iterator.remove();
                continue;
            }

            // Check if any player is standing on or very close to the particle
            for (PlayerEntity player : world.getPlayers()) {
                BlockPos playerPos = player.getBlockPos();
                Vec3d playerExactPos = player.getPos();
                
                // Check if player is within 1 block horizontally and at most 2 blocks vertically
                if (Math.abs(playerPos.getX() - seedPos.getX()) <= 1 &&
                    Math.abs(playerPos.getZ() - seedPos.getZ()) <= 1 &&
                    Math.abs(playerExactPos.y - (seedPos.getY() + 1)) <= 1.5) {
                    
                    // Player collected the rootgrasp seed!
                    collectRootgraspSeed(player, seedPos, world);
                    iterator.remove();
                    break;
                }
            }
        }
    }

    /**
     * Give a player a rootgrasp seed when they collect a particle
     */
    private void collectRootgraspSeed(PlayerEntity player, BlockPos seedPos, ServerWorld world) {
        // Create 1-3 rootgrasp seeds based on intensity
        int seedCount = Math.min(1, world.getRandom().nextInt(intensity) + 1);
        ItemStack rootgraspSeed = new ItemStack(ModItems.ROOTGRASP_FLOWER_SEEDS, seedCount);
        
        // Add the item to player inventory or drop it if inventory is full
        if (!player.getInventory().insertStack(rootgraspSeed)) {
            player.dropItem(rootgraspSeed, false);
        }
        
        // Send message to player
        player.sendMessage(net.minecraft.text.Text.literal("You collected " + 
                           (seedCount > 1 ? seedCount + " Rootgrasp Seeds!" : "a Rootgrasp Seed!")), true);
        
        // Play sound and particles for collection
        world.playSound(
            null,
            seedPos,
            SoundEvents.BLOCK_GRASS_BREAK,
            SoundCategory.BLOCKS,
            1.0f,
            1.0f + (world.getRandom().nextFloat() * 0.3f)
        );
        
        // Create particle burst effect
        Vec3d burstPos = new Vec3d(seedPos.getX() + 0.5, seedPos.getY() + 0.1, seedPos.getZ() + 0.5);
        
        // Green particle burst - more particles at higher intensity
        int particleCount = 10 + (intensity * 5);
        for (int i = 0; i < particleCount; i++) {
            world.spawnParticles(
                new DustParticleEffect(new Vec3f(0.2f, 0.8f, 0.2f), world.getRandom().nextFloat() * 0.5f + 0.5f),
                burstPos.x, burstPos.y, burstPos.z,
                1,
                (world.getRandom().nextDouble() - 0.5) * 0.3,
                world.getRandom().nextDouble() * 0.3,
                (world.getRandom().nextDouble() - 0.5) * 0.3,
                0.05f
            );
        }
        
        // Add some vanilla particles too
        world.spawnParticles(
            ParticleTypes.HAPPY_VILLAGER,
            burstPos.x, burstPos.y + 0.5, burstPos.z,
            intensity * 5,
            0.3, 0.3, 0.3,
            0.02
        );
    }

    private void applyOvergrowthEffects(ServerWorld world) {
        // Process loaded chunks around players
        for (var player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();
            ChunkPos chunkPos = new ChunkPos(playerPos);
            
            // Range scales with intensity
            int chunkRange = intensity;
            List<WorldChunk> nearbyChunks = new ArrayList<>();
            for (int x = -chunkRange; x <= chunkRange; x++) {
                for (int z = -chunkRange; z <= chunkRange; z++) {
                    WorldChunk chunk = world.getChunk(chunkPos.x + x, chunkPos.z + z);
                    if (chunk != null) {
                        nearbyChunks.add(chunk);
                    }
                }
            }
            
            // Select random blocks from nearby chunks - scale with intensity
            int blocksToProcess = BLOCKS_PER_TICK * intensity;
            Random random = world.getRandom();
            for (int i = 0; i < blocksToProcess; i++) {
                if (nearbyChunks.isEmpty()) continue;
                
                WorldChunk chunk = nearbyChunks.get(random.nextInt(nearbyChunks.size()));
                int x = random.nextInt(16);
                int z = random.nextInt(16);
                int y = 50 + random.nextInt(100); // Adjust y-range as needed
                
                BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                
                // Try to find a valid block
                if (world.isChunkLoaded(pos)) {
                    BlockState state = world.getBlockState(pos);
                    
                    // Accelerate crop growth - chance increases with intensity
                    if (state.getBlock() instanceof CropBlock) {
                        CropBlock crop = (CropBlock) state.getBlock();
                        if (!crop.isMature(state)) {
                            int age = state.get(CropBlock.AGE);
                            float growChance = 0.5f + (intensity * 0.1f); // 0.6, 0.7, 0.8 based on intensity
                            if (random.nextFloat() < growChance) {
                                // Higher intensity might advance multiple stages
                                int advancement = intensity > 2 ? 
                                    Math.min(crop.getMaxAge() - age, 2) : 
                                    Math.min(crop.getMaxAge() - age, 1);
                                world.setBlockState(pos, state.with(CropBlock.AGE, age + advancement));
                            }
                        }
                    } 
                    // Transform saplings - chance increases with intensity
                    else if (state.getBlock() instanceof SaplingBlock) {
                        SaplingBlock sapling = (SaplingBlock) state.getBlock();
                        float growChance = 0.3f + (intensity * 0.1f); // 0.4, 0.5, 0.6 based on intensity
                        if (random.nextFloat() < growChance) {
                            sapling.grow(world, world.getRandom(), pos, state);
                        }
                    }
                    // Apply moss transformations - chance increases with intensity
                    else if (MOSS_TRANSFORMATIONS.containsKey(state.getBlock())) {
                        float transformChance = 0.1f + (intensity * 0.05f); // 0.15, 0.2, 0.25
                        if (random.nextFloat() < transformChance) {
                            Block mossyVariant = MOSS_TRANSFORMATIONS.get(state.getBlock());
                            world.setBlockState(pos, mossyVariant.getDefaultState());
                            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, 
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 
                                5, 0.25, 0.25, 0.25, 0.01);
                        }
                    }
                    // Spread moss to dirt and stone - chance increases with intensity
                    else if ((state.getBlock() == Blocks.DIRT || state.getBlock() == Blocks.STONE) 
                            && world.getBlockState(pos.up()).isAir()) {
                        float mossChance = 0.03f + (intensity * 0.02f); // 0.05, 0.07, 0.09
                        if (random.nextFloat() < mossChance) {
                            world.setBlockState(pos, Blocks.MOSS_BLOCK.getDefaultState());
                            
                            // At highest intensity, sometimes add moss carpet on adjacent blocks
                            if (intensity == 3 && random.nextFloat() < 0.2f) {
                                for (int dx = -1; dx <= 1; dx++) {
                                    for (int dz = -1; dz <= 1; dz++) {
                                        if (dx == 0 && dz == 0) continue;
                                        
                                        BlockPos adjacentPos = pos.add(dx, 0, dz);
                                        if (world.getBlockState(adjacentPos).isAir() && 
                                            !world.getBlockState(adjacentPos.down()).isAir()) {
                                            world.setBlockState(adjacentPos, Blocks.MOSS_CARPET.getDefaultState());
                                        }
                                    }
                                }
                            }
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
