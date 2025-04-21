
package end3r.verdant_arcanum.event;

import net.minecraft.block.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;

public class OvergrowthEvent implements CustomWorldEvent {
    public static final Identifier ID = new Identifier("verdant_arcanum", "overgrowth");
    private static final int DEFAULT_EVENT_DURATION = 400; // 10 seconds

    private int ticksRemaining = DEFAULT_EVENT_DURATION;
    private int growthCounter = 0;
    private int intensity = 2; // Default medium intensity (1=mild, 2=moderate, 3=intense)

    // Debugging counters
    private int blockTransformations = 0;
    private int seedParticlesSpawned = 0;
    private int seedsCollected = 0;

    // Map for block transformations
    private static final Map<Block, Block> MOSS_TRANSFORMATIONS = new HashMap<>();

    // Rootgrasp seed particle configuration
    private static final Vec3f ROOTGRASP_SEED_PARTICLE_COLOR = new Vec3f(0.2f, 0.8f, 0.2f); // Green color
    private static final float ROOTGRASP_SEED_PARTICLE_SIZE = 1.5f; // Increased size for better visibility
    private static final int ROOTGRASP_SEED_SPAWN_CHANCE = 50; // Increased chance (1 in X)
    private static final double ROOTGRASP_SEED_COLLECTION_RADIUS = 1.5; // Increased radius for easier collection

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
        // Reset debug counters
        blockTransformations = 0;
        seedParticlesSpawned = 0;
        seedsCollected = 0;

        // Reset active seed particles
        activeRootgraspSeedParticles.clear();

        // Ensure intensity is valid
        intensity = Math.max(1, Math.min(3, intensity));

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


        // Send message to players
        world.getPlayers().forEach(player ->
                player.sendMessage(Text.literal("🌱 " + intensityDesc + " nature's power surges! An Overgrowth event has begun.")
                        .formatted(Formatting.GREEN, Formatting.BOLD), true)
        );


        // Force immediate effects for visibility when starting
        applyImmediateEffects(world);
    }

    /**
     * Apply some immediate effects when the event starts so players can see something happening
     */
    private void applyImmediateEffects(ServerWorld world) {
        // Apply effects around each player
        for (PlayerEntity player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();

            // Spawn lots of particles around the player
            for (int i = 0; i < 50; i++) {
                double x = playerPos.getX() + world.getRandom().nextGaussian() * 5;
                double y = playerPos.getY() + world.getRandom().nextGaussian() * 3 + 1;
                double z = playerPos.getZ() + world.getRandom().nextGaussian() * 5;
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z,
                        1, 0.1, 0.1, 0.1, 0.02);
            }

            // Force spawn seed particles near player
            for (int attempt = 0; attempt < 3; attempt++) {
                int offsetX = world.getRandom().nextInt(6) - 3;
                int offsetZ = world.getRandom().nextInt(6) - 3;
                BlockPos targetPos = playerPos.add(offsetX, 0, offsetZ);

                BlockPos groundPos = findGroundPos(world, targetPos);
                if (groundPos != null) {
                    // Create a seed particle
                    UUID particleId = UUID.randomUUID();
                    activeRootgraspSeedParticles.put(particleId, groundPos);
                    seedParticlesSpawned++;

                    // Spawn extra visible particles at this location
                    Vec3d particlePos = new Vec3d(
                            groundPos.getX() + 0.5,
                            groundPos.getY() + 0.1,
                            groundPos.getZ() + 0.5
                    );

                    for (int i = 0; i < 10; i++) {
                        world.spawnParticles(
                                new DustParticleEffect(ROOTGRASP_SEED_PARTICLE_COLOR, ROOTGRASP_SEED_PARTICLE_SIZE),
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.1, 0.1, 0.1, 0
                        );

                        world.spawnParticles(
                                ParticleTypes.HAPPY_VILLAGER,
                                particlePos.x, particlePos.y + 0.2, particlePos.z,
                                1, 0.2, 0.2, 0.2, 0.01
                        );
                    }

                    // Play a sound
                    world.playSound(
                            null, groundPos, SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.BLOCKS,
                            1.0f, 0.8f + (world.getRandom().nextFloat() * 0.2f)
                    );

                    // Success
                    break;
                }
            }

            // Apply some guaranteed transformations near the player
            applyGuaranteedEffectsNearPlayer(world, player, world.getRandom());
        }

    }

    @Override
    public void tick(ServerWorld world) {
        // Add debug counter to track ticks
        int tickCounter = 0;
        tickCounter++;

        // Decrement remaining time
        ticksRemaining--;

        if (tickCounter % 20 == 0) {

        }
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
        // Run more frequently for better player experience
        if (ticksRemaining % (20 - (intensity * 5)) == 0) { // 15, 10, or 5 ticks based on intensity
            spawnRootgraspSeedParticles(world);
        }

        // Check for players collecting rootgrasp seed particles EVERY tick
        // This is critical to ensure responsive collection
        checkRootgraspSeedCollection(world);

        growthCounter++;

        // IMPORTANT: Growth cycle with simplified, fixed interval
        // Use a static counter to ensure it keeps advancing
        growthCounter++;

        // Log growth counter occasionally
        if (growthCounter % 20 == 0) {

        }

        // Apply growth effects every 20 ticks (1 second)
        if (growthCounter % 20 == 0) {
            applyOvergrowthEffects(world);
        }

        // Notify players when the event is halfway through and near completion
        if (ticksRemaining == DEFAULT_EVENT_DURATION / 2 && ticksRemaining > 600) {
            world.getPlayers().forEach(player ->
                    player.sendMessage(Text.literal("The Overgrowth continues to spread...").formatted(Formatting.GREEN), true)
            );
        } else if (ticksRemaining == 200) { // 10 seconds remaining
            world.getPlayers().forEach(player ->
                    player.sendMessage(Text.literal("The Overgrowth is beginning to subside...").formatted(Formatting.YELLOW), true)
            );
        }

        // Debug messages for operators every minute
        if (ticksRemaining % 1200 == 0 || ticksRemaining <= 10) {
        }
    }

    /**
     * Spawn special green particles that represent rootgrasp seeds on the ground
     */
    private void spawnRootgraspSeedParticles(ServerWorld world) {
        // Cap the maximum number of active rootgrasp seed particles - scales with intensity
        int maxParticles = 8 + (intensity * 7); // Increased max particles
        if (activeRootgraspSeedParticles.size() >= maxParticles) {
            return;
        }

        // Track attempts and success for troubleshooting
        int attempts = 0;
        int successes = 0;

        // Generate particles around players - chance improves with intensity
        int adjustedSpawnChance = Math.max(3, ROOTGRASP_SEED_SPAWN_CHANCE - ((intensity - 1) * 3));

        // Generate particles around players
        for (PlayerEntity player : world.getPlayers()) {
            // Force at least one attempt per player
            for (int forcedAttempt = 0; forcedAttempt < 5; forcedAttempt++) { // Increased attempts
                attempts++;
                BlockPos playerPos = player.getBlockPos();

                // Generate particle at a random position around the player
                int radius = 6 + world.getRandom().nextInt(10); // Adjusted range: 6-16 blocks
                int offsetX = world.getRandom().nextInt(radius * 2) - radius;
                int offsetZ = world.getRandom().nextInt(radius * 2) - radius;

                // Log attempt details
                if (forcedAttempt == 0) {
                }

                // Find the top solid block at this x,z position
                BlockPos targetPos = playerPos.add(offsetX, 0, offsetZ);
                BlockPos groundPos = findGroundPos(world, targetPos);

                // Skip if no suitable ground was found
                if (groundPos == null) {
                    if (forcedAttempt == 0) {
                    }
                    continue;
                }

                // Create a UUID for this particle to track it
                UUID particleId = UUID.randomUUID();
                activeRootgraspSeedParticles.put(particleId, groundPos);
                seedParticlesSpawned++;
                successes++;


                // Spawn visible green particle with extra particles for visibility
                Vec3d particlePos = new Vec3d(
                        groundPos.getX() + 0.5,
                        groundPos.getY() + 0.15, // Slightly higher for better visibility
                        groundPos.getZ() + 0.5
                );

                try {
                    // Spawn more particles to make them more visible
                    for (int i = 0; i < 8; i++) { // Increased particle count
                        world.spawnParticles(
                                new DustParticleEffect(ROOTGRASP_SEED_PARTICLE_COLOR, ROOTGRASP_SEED_PARTICLE_SIZE),
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.1, 0.1, 0.1, 0
                        );
                    }

                    // Also spawn vanilla particles for better visibility
                    world.spawnParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            particlePos.x, particlePos.y + 0.2, particlePos.z,
                            8, 0.2, 0.2, 0.2, 0.01
                    );
                    
                    // Play a sound to make it more noticeable
                    world.playSound(
                            null,
                            groundPos,
                            SoundEvents.BLOCK_GRASS_PLACE,
                            SoundCategory.BLOCKS,
                            0.8f,
                            1.0f + (world.getRandom().nextFloat() * 0.3f)
                    );
                } catch (Exception e) {
                }

                // Successfully spawned, break out of the forced attempts loop
                break;
            }
        }

    }

    /**
     * Find the top solid block at a given x,z position with improved logging
     */
    private BlockPos findGroundPos(ServerWorld world, BlockPos startPos) {
        // Check if the chunk is loaded first
        if (!world.isChunkLoaded(startPos)) {
            return null;
        }

        // Start at player level and search up and down
        int startY = startPos.getY();
        int minY = Math.max(world.getBottomY(), startY - 15);
        int maxY = Math.min(world.getTopY(), startY + 15);

        // Debug logging for troubleshooting
        boolean verbose = world.getRandom().nextInt(20) == 0; // 5% chance to log details
        if (verbose) {
        }

        // Search downward first (more likely to find ground)
        for (int y = startY; y >= minY; y--) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());

            if (!world.isChunkLoaded(checkPos)) {
                continue; // Skip unloaded chunks
            }

            try {
                BlockState state = world.getBlockState(checkPos);
                BlockState aboveState = world.getBlockState(checkPos.up());

                // Log some samples
                if (verbose && (y == startY || y % 5 == 0)) {
                }

                // Check for a suitable ground block (solid blocks with air above)
                if (isSuitableGround(state) &&
                        (aboveState.isAir() || aboveState.getBlock() instanceof PlantBlock)) {
                    if (verbose) {
                    }
                    return checkPos;
                }
            } catch (Exception e) {
                if (verbose) {
                }
            }
        }

        // If not found, search upward
        for (int y = startY + 1; y <= maxY; y++) {
            BlockPos checkPos = new BlockPos(startPos.getX(), y, startPos.getZ());

            if (!world.isChunkLoaded(checkPos)) {
                continue;
            }

            try {
                BlockState state = world.getBlockState(checkPos);
                BlockState aboveState = world.getBlockState(checkPos.up());

                // Check for a suitable ground block (solid blocks with air above)
                if (isSuitableGround(state) &&
                        (aboveState.isAir() || aboveState.getBlock() instanceof PlantBlock)) {
                    if (verbose) {
                    }
                    return checkPos;
                }
            } catch (Exception e) {
                // Skip problematic blocks
            }
        }

        if (verbose) {
        }
        return null;
    }

    /**
     * Check if a block state is suitable to be considered "ground"
     */
    private boolean isSuitableGround(BlockState state) {
        Block block = state.getBlock();

        // Include more valid ground types - with Material.SOLID check removed since it doesn't exist
        return state.getMaterial().blocksMovement() ||
                block == Blocks.GRASS_BLOCK ||
                block == Blocks.DIRT ||
                block == Blocks.COARSE_DIRT ||
                block == Blocks.PODZOL ||
                block == Blocks.MYCELIUM ||
                block == Blocks.FARMLAND ||
                block == Blocks.MOSS_BLOCK ||
                block == Blocks.STONE ||
                block == Blocks.DEEPSLATE ||
                block == Blocks.CALCITE ||
                MOSS_TRANSFORMATIONS.containsKey(block);
    }


    /**
     * Check if players are standing on rootgrasp seed particles to collect them
     */
    private void checkRootgraspSeedCollection(ServerWorld world) {
        // Skip if no particles or no players
        if (activeRootgraspSeedParticles.isEmpty() || world.getPlayers().isEmpty()) {
            return;
        }

        // Debug occasionally
        boolean debugThisTick = world.getRandom().nextInt(100) == 0;
        if (debugThisTick) {
        }

        // Tick each particle
        Iterator<Map.Entry<UUID, BlockPos>> iterator = activeRootgraspSeedParticles.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BlockPos> entry = iterator.next();
            UUID particleId = entry.getKey();
            BlockPos seedPos = entry.getValue();

            // Skip if the chunk isn't loaded
            if (!world.isChunkLoaded(seedPos)) {
                if (world.getRandom().nextFloat() < 0.05f) { // 5% chance to remove unloaded particles
                    iterator.remove();
                }
                continue;
            }
            
            // Additional safety check for chunk access
            try {
                world.getChunk(seedPos);
            } catch (Exception e) {
                // Remove particles in problematic chunks
                iterator.remove();
                continue;
            }

            // Generate particle effects EVERY tick to make it more visible
            Vec3d particlePos = new Vec3d(
                    seedPos.getX() + 0.5,
                    seedPos.getY() + 0.15, // Slightly higher for better visibility
                    seedPos.getZ() + 0.5
            );

            // Main particle - MORE particles for increased visibility
            world.spawnParticles(
                    new DustParticleEffect(ROOTGRASP_SEED_PARTICLE_COLOR, ROOTGRASP_SEED_PARTICLE_SIZE),
                    particlePos.x, particlePos.y, particlePos.z,
                    2, // Spawn 2 at once
                    0.1, 0.1, 0.1,
                    0
            );

            // Add vanilla particles for better visibility EVERY tick
            world.spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    particlePos.x, particlePos.y + 0.2, particlePos.z,
                    1,
                    0.1, 0.1, 0.1,
                    0.0
            );

            // Small "pulsing" particles - keep these for visual effect
            if (world.getRandom().nextInt(2) == 0) {
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

            // Randomly remove particles over time (5% chance every 5 seconds)
            if (ticksRemaining % 100 == 0 && world.getRandom().nextFloat() < 0.05f) {
                iterator.remove();
                continue;
            }

            // Check if any player is standing on or very close to the particle
            for (PlayerEntity player : world.getPlayers()) {
                Vec3d playerExactPos = player.getPos();

                // Check if player is within collection radius horizontally and at most 2 blocks vertically
                double dx = playerExactPos.x - (seedPos.getX() + 0.5);
                double dz = playerExactPos.z - (seedPos.getZ() + 0.5);
                double horizontalDistSq = dx * dx + dz * dz;

                if (horizontalDistSq <= ROOTGRASP_SEED_COLLECTION_RADIUS * ROOTGRASP_SEED_COLLECTION_RADIUS &&
                        Math.abs(playerExactPos.y - (seedPos.getY() + 1.5)) <= 2.0) {

                    // Player collected the rootgrasp seed!
                    collectRootgraspSeed(player, seedPos, world);
                    iterator.remove();
                    break;
                }
                
                // Debug player distances occasionally
                if (debugThisTick && horizontalDistSq < 25) { // Only if within 5 blocks

                }
            }
        }
    }

    /**
     * Give a player a rootgrasp seed when they collect a particle
     */
    private void collectRootgraspSeed(PlayerEntity player, BlockPos seedPos, ServerWorld world) {
        try {
            // Create 1-3 rootgrasp seeds based on intensity
            int seedCount = 1 + world.getRandom().nextInt(intensity);

            // Get the item from the ModItems registry
            if (ModItems.ROOTGRASP_FLOWER_SEEDS != null) {
                ItemStack rootgraspSeed = new ItemStack(ModItems.ROOTGRASP_FLOWER_SEEDS, seedCount);

                // Add the item to player inventory or drop it if inventory is full
                if (!player.getInventory().insertStack(rootgraspSeed)) {
                    player.dropItem(rootgraspSeed, false);
                }

                // Send message to player
                player.sendMessage(Text.literal("You collected " +
                                (seedCount > 1 ? seedCount + " Rootgrasp Seeds!" : "a Rootgrasp Seed!"))
                        .formatted(Formatting.GREEN, Formatting.BOLD), true);

                // Track collection for debugging
                seedsCollected += seedCount;
            } else {
                // Log failure if the item isn't available
                player.sendMessage(Text.literal("You try to collect a rootgrasp seed, but it crumbles away...")
                        .formatted(Formatting.RED), true);

            }
        } catch (Exception e) {
            // Fallback message if there's an error
            player.sendMessage(Text.literal("You try to collect a rootgrasp seed, but something went wrong...")
                    .formatted(Formatting.RED), true);
        }

        // Play sound and particles for collection regardless of success
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
        // Track transformation statistics for debugging
        int transformAttempts = 0;
        int transformSuccess = 0;


        // Process blocks near each player
        for (PlayerEntity player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();

            // Process a specific number of random blocks near the player
            int blocksToProcess = 15 * intensity; // 15, 30, or 45 blocks per player


            // Process random blocks near the player
            for (int i = 0; i < blocksToProcess; i++) {
                // Choose a random position near the player
                int radius = 8; // Fixed, reasonable radius
                int x = playerPos.getX() + world.getRandom().nextInt(radius * 2) - radius;
                int y = playerPos.getY() + world.getRandom().nextInt(8) - 4; // Up to 4 blocks up/down
                int z = playerPos.getZ() + world.getRandom().nextInt(radius * 2) - radius;
                BlockPos pos = new BlockPos(x, y, z);

                // Skip if chunk isn't loaded
                if (!world.isChunkLoaded(pos)) {
                    continue;
                }

                transformAttempts++;

                try {
                    // Get the block and attempt transformation
                    BlockState state = world.getBlockState(pos);
                    Block block = state.getBlock();

                    // Log some sample blocks (10% chance)
                    if (world.getRandom().nextInt(10) == 0) {
                    }

                    boolean transformed = false;

                    // 1. Try moss transformations - guaranteed success
                    if (MOSS_TRANSFORMATIONS.containsKey(block)) {
                        Block mossyVariant = MOSS_TRANSFORMATIONS.get(block);
                        world.setBlockState(pos, mossyVariant.getDefaultState(), Block.NOTIFY_ALL);
                        transformed = true;


                        // Visual feedback
                        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                8, 0.3, 0.3, 0.3, 0.01);
                    }
                    // 2. Try dirt/stone/grass to moss - high success rate
                    else if ((block == Blocks.DIRT || block == Blocks.STONE ||
                            block == Blocks.GRASS_BLOCK) &&
                            world.getBlockState(pos.up()).isAir()) {
                        // 50% chance
                        if (world.getRandom().nextFloat() < 0.5f) {
                            world.setBlockState(pos, Blocks.MOSS_BLOCK.getDefaultState(), Block.NOTIFY_ALL);
                            transformed = true;



                            // Visual feedback
                            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    8, 0.3, 0.3, 0.3, 0.01);
                        }
                    }
                    // 3. Try crop growth
                    else if (block instanceof CropBlock) {
                        CropBlock crop = (CropBlock) block;
                        if (!crop.isMature(state)) {
                            try {
                                int age = state.get(CropBlock.AGE);
                                int maxAge = crop.getMaxAge();

                                // Force growth of 1-2 stages
                                int newAge = Math.min(maxAge, age + 1 + world.getRandom().nextInt(intensity));

                                // Update the block
                                world.setBlockState(pos, state.with(CropBlock.AGE, newAge), Block.NOTIFY_ALL);
                                transformed = true;



                                // Visual feedback
                                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                        5, 0.3, 0.3, 0.3, 0.01);
                            } catch (Exception e) {

                            }
                        }
                    }

                    // Track successful transformations
                    if (transformed) {
                        transformSuccess++;
                        blockTransformations++;
                    }
                } catch (Exception e) {

                }
            }

            // Always do guaranteed effects for each player
            applyGuaranteedEffectsNearPlayer(world, player, world.getRandom());
        }

    }

    /**
     * Apply guaranteed effects very close to a player
     * This ensures players can see the event working even in difficult terrain
     */
    private void applyGuaranteedEffectsNearPlayer(ServerWorld world, PlayerEntity player, Random random) {
        BlockPos playerPos = player.getBlockPos();

        // Try several positions very close to the player
        for (int i = 0; i < 5; i++) {
            int range = 3 + (intensity * 2); // 5, 7, or 9 block range
            int x = playerPos.getX() + random.nextInt(range * 2) - range;
            int z = playerPos.getZ() + random.nextInt(range * 2) - range;

            // Try various heights
            for (int yOffset : new int[]{0, -1, 1, -2, 2, -3, 3}) {
                int y = playerPos.getY() + yOffset;
                BlockPos pos = new BlockPos(x, y, z);

                if (!world.isChunkLoaded(pos)) continue;

                // Additional safety check for chunk access
                try {
                    world.getChunk(pos);
                } catch (Exception e) {
                    // Skip this position if there's any issue accessing the chunk
                    continue;
                }

                BlockState state = world.getBlockState(pos);
                Block block = state.getBlock();

                // Guaranteed moss transformation
                if (MOSS_TRANSFORMATIONS.containsKey(block)) {
                    Block mossyVariant = MOSS_TRANSFORMATIONS.get(block);
                    world.setBlockState(pos, mossyVariant.getDefaultState());
                    blockTransformations++;

                    // Visual and sound feedback
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            8, 0.3, 0.3, 0.3, 0.01);

                    if (random.nextBoolean()) {
                        world.playSound(
                                null, pos, SoundEvents.BLOCK_MOSS_PLACE, SoundCategory.BLOCKS,
                                0.5f, 0.8f + (random.nextFloat() * 0.4f)
                        );
                    }

                    // Success, no need to check more Y levels
                    break;
                }
                // Grass/dirt/stone to moss blocks
                else if ((block == Blocks.DIRT || block == Blocks.STONE || block == Blocks.GRASS_BLOCK) &&
                        world.getBlockState(pos.up()).isAir()) {
                    world.setBlockState(pos, Blocks.MOSS_BLOCK.getDefaultState());
                    blockTransformations++;

                    // Visual and sound feedback
                    world.spawnParticles(ParticleTypes.HAPPY_VILLAGER,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            8, 0.3, 0.3, 0.3, 0.01);

                    if (random.nextBoolean()) {
                        world.playSound(
                                null, pos, SoundEvents.BLOCK_MOSS_PLACE, SoundCategory.BLOCKS,
                                0.5f, 0.8f + (random.nextFloat() * 0.4f)
                        );
                    }

                    // Success, no need to check more Y levels
                    break;
                }
            }
        }
    }

    /**
     * Send a debug message to server operators
     */


    @Override
    public boolean isComplete() {
        return ticksRemaining <= 0;
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public int getDuration() {
        return 400;
    }
}
