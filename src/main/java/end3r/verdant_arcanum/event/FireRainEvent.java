package end3r.verdant_arcanum.event;

import end3r.verdant_arcanum.registry.ModItems;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import net.minecraft.world.GameRules;

import java.util.*;

public class FireRainEvent implements CustomWorldEvent {
    public static final Identifier ID = new Identifier("verdant_arcanum", "fire_rain");
    private static final int DEFAULT_DURATION = 20 * 60 * 2; // 2 minutes (in ticks)
    private static final int CHECK_INTERVAL = 10; // Check for player damage every 10 ticks (0.5 seconds)
    private static final float DAMAGE_AMOUNT = 1.0f; // 0.5 hearts of damage
    private static final DamageSource DAMAGE_SOURCE = new DamageSource("fire_rain").setFire();

    // Particle configuration
    private static final int PARTICLE_SPAWN_INTERVAL = 2; // Spawn particles every 2 ticks
    private static final int BASE_PARTICLE_COUNT = 5; // Base number of particles to spawn each interval
    private static final int PARTICLE_HEIGHT = 20; // Height from which particles fall
    private static final float PARTICLE_SPREAD_RADIUS = 15.0f; // Radius around player for particle spawning

    // Flame seed configuration
    private static final float FLAME_SEED_SPAWN_CHANCE = 0.001f; // 0.1 chance per tick cycle
    private static final int FLAME_SEED_LIFETIME = 600; // 30 seconds lifetime for flame seeds on ground
    private static final int FLAME_SEED_CHECK_INTERVAL = 5; // Check for player collision every 5 ticks

    // Track active flame seed particles on the ground
    private Map<BlockPos, Integer> flameSeedPositions = new HashMap<>();


    private int ticksRemaining = DEFAULT_DURATION;
    private int intensityLevel = 2; // 1=mild, 2=moderate, 3=severe
    private int tickCounter = 0;
    private Random random = new Random();

    // Store previous weather state to restore after event
    private boolean wasRaining = false;
    private boolean wasThundering = false;

    /**
     * Set the duration of the fire rain event
     * @param ticks Duration in ticks (20 ticks = 1 second)
     */
    public void setDuration(int ticks) {
        this.ticksRemaining = Math.max(200, ticks); // Minimum 10 seconds
    }

    /**
     * Set the intensity of the fire rain
     * @param intensity 1=mild, 2=moderate, 3=severe
     */
    public void setIntensity(int intensity) {
        this.intensityLevel = Math.max(1, Math.min(3, intensity)); // Clamp between 1-3
    }

    @Override
    public void start(ServerWorld world) {
        // Ensure we're only in the Nether
        if (world.getRegistryKey() != World.NETHER) {
            return;
        }

        // Reset tick counter
        tickCounter = 0;

        // Store current weather state
        wasRaining = world.isRaining();
        wasThundering = world.isThundering();

        // Force rain to start - NOTE: This is a server-side instruction to start the rain weather effect
        // The visual rain effect will be determined by client-side resources or resource packs
        // A resource pack can be used to make the rain appear red/orange in the Nether
        world.setWeather(0, ticksRemaining, true, intensityLevel > 1);

        // Determine description based on intensity
        String intensityDesc;
        switch (intensityLevel) {
            case 1:
                intensityDesc = "Mild";
                break;
            case 3:
                intensityDesc = "Deadly";
                break;
            default:
                intensityDesc = "Fierce";
                break;
        }

        // Notify all players in the Nether
        world.getPlayers().forEach(player ->
                player.sendMessage(Text.literal("🔥 A " + intensityDesc + " rain of fire begins to fall from above!")
                        .formatted(Formatting.RED, Formatting.BOLD), true)
        );

        // Play a dramatic sound for all players
        world.getPlayers().forEach(player ->
                player.playSound(SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.AMBIENT, 1.0f, 0.5f)
        );

        // Initial dramatic thunder and fire sounds
        if (intensityLevel > 1) {
            world.getPlayers().forEach(player -> {
                player.playSound(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.0f, 0.8f);
                player.playSound(SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.AMBIENT, 1.0f, 0.7f);
            });
        }

        // Store fire spread rule value and possibly enable fire spreading based on intensity
        if (intensityLevel > 1) {
            // Only enable fire spread for higher intensities
            boolean originalFireSpreadValue = world.getGameRules().getBoolean(GameRules.DO_FIRE_TICK);
            if (!originalFireSpreadValue) {
                // Don't actually modify the game rule as it affects the whole server
                // But we'll handle fire creation separately
            }
        }

        // Initial burst of fire particles
        spawnFireRainParticles(world, true);
    }

    @Override
    public void tick(ServerWorld world) {
        // Ensure we're only in the Nether
        if (world.getRegistryKey() != World.NETHER) {
            return;
        }

        // Decrement remaining time
        ticksRemaining--;
        tickCounter++;

        // Ensure it's still raining - check every 100 ticks to avoid constant resets
        if (tickCounter % 100 == 0 && !world.isRaining()) {
            world.setWeather(0, ticksRemaining, true, intensityLevel > 1);
        }

        // Spawn fire rain particles regularly
        if (tickCounter % PARTICLE_SPAWN_INTERVAL == 0) {
            spawnFireRainParticles(world, false);
        }

        // Chance to spawn flame seed particles
        if (random.nextFloat() < FLAME_SEED_SPAWN_CHANCE * intensityLevel) {
            spawnFlameSeedParticle(world);
        }

        // For maximum intensity, occasionally create fires on exposed blocks
        if (intensityLevel == 3 && random.nextFloat() < 0.05f) {
            createRandomFires(world);
        }

        // Check for player damage
        if (tickCounter % CHECK_INTERVAL == 0) {
            checkPlayerExposure(world);
        }

        // Check for players walking on flame seeds
        if (tickCounter % FLAME_SEED_CHECK_INTERVAL == 0) {
            checkFlameSeedCollisions(world);
        }

        // Update flame seed lifetimes and remove expired ones
        updateFlameSeedLifetimes(world);

        // Play ambient fire sounds
        if (tickCounter % 40 == 0) {
            world.getPlayers().forEach(player ->
                    player.playSound(
                            SoundEvents.BLOCK_FIRE_AMBIENT,
                            SoundCategory.AMBIENT,
                            0.3f,
                            0.7f + random.nextFloat() * 0.3f
                    )
            );
        }

        // Stronger thunder sounds occasionally for higher intensities
        if (intensityLevel > 1 && tickCounter % 100 == 0) {
            world.getPlayers().forEach(player ->
                    player.playSound(
                            SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                            SoundCategory.WEATHER,
                            0.4f,
                            0.6f + random.nextFloat() * 0.2f
                    )
            );
        }

        // Notify players when event is ending
        if (ticksRemaining == 600) { // 30 seconds left
            world.getPlayers().forEach(player ->
                    player.sendMessage(Text.literal("The fire rain is beginning to subside...")
                            .formatted(Formatting.RED), true)
            );
        } else if (ticksRemaining == 100) { // 5 seconds left
            world.getPlayers().forEach(player ->
                    player.sendMessage(Text.literal("The fire rain stops...")
                            .formatted(Formatting.GOLD), true)
            );
        }

        // If complete, call onComplete to restore weather
        if (isComplete()) {
            onComplete(world);
        }
    }

    /**
     * Spawn a special flame seed particle that stays on the ground
     * @param world The server world
     */
// Modify the spawnFlameSeedParticle method
    private void spawnFlameSeedParticle(ServerWorld world) {
        // Skip if no players are in the dimension
        if (world.getPlayers().isEmpty()) {
            return;
        }

        // Select a random player to center around
        PlayerEntity player = world.getPlayers().get(random.nextInt(world.getPlayers().size()));
        BlockPos playerPos = player.getBlockPos();

        // Add debug message to check if method is being called
        System.out.println("[FireRainEvent] Attempting to spawn flame seed");

        // Randomize position around player
        double offsetX = (random.nextDouble() - 0.5) * PARTICLE_SPREAD_RADIUS * 2;
        double offsetZ = (random.nextDouble() - 0.5) * PARTICLE_SPREAD_RADIUS * 2;

        // Find a valid position to place the flame seed
        int x = playerPos.getX() + (int)offsetX;
        int z = playerPos.getZ() + (int)offsetZ;

        // Try multiple Y positions around the player's level
        // This is more reliable in the Nether than using getTopPosition
        boolean foundValidPos = false;
        BlockPos flameSeedPos = null;

        // Try positions from 5 blocks below to 5 blocks above player
        for (int yOffset = -5; yOffset <= 5; yOffset++) {
            BlockPos checkPos = new BlockPos(x, playerPos.getY() + yOffset, z);

            // Check if the block is solid and the block above is air
            if (world.getBlockState(checkPos).isSolidBlock(world, checkPos) &&
                    world.isAir(checkPos.up())) {
                flameSeedPos = checkPos.up();
                foundValidPos = true;
                System.out.println("[FireRainEvent] Found valid position at " + flameSeedPos);
                break;
            }
        }

        // If we found a valid position, spawn the flame seed
        if (foundValidPos && flameSeedPos != null) {
            // Add this position to our tracked flame seeds with full lifetime
            flameSeedPositions.put(flameSeedPos, FLAME_SEED_LIFETIME);

            // Spawn a distinct particle effect for the flame seed
            world.spawnParticles(
                    ParticleTypes.SOUL_FIRE_FLAME, // Use soul fire for distinction
                    flameSeedPos.getX() + 0.5, flameSeedPos.getY() + 0.1, flameSeedPos.getZ() + 0.5,
                    5, // Count - multiple particles for visibility
                    0.1, 0.0, 0.1, // Minimal spread - contained in one spot
                    0.01 // Very slow drift
            );

            // Add some embers around it
            world.spawnParticles(
                    new DustParticleEffect(new Vec3f(1.0F, 0.5F, 0.0F), 1.0F), // Orange dust
                    flameSeedPos.getX() + 0.5, flameSeedPos.getY() + 0.3, flameSeedPos.getZ() + 0.5,
                    3, // Count
                    0.2, 0.1, 0.2, // Spread
                    0.02 // Speed
            );

            // Play a distinctive sound
            world.playSound(
                    null, // No player
                    flameSeedPos.getX(), flameSeedPos.getY(), flameSeedPos.getZ(),
                    SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.BLOCKS,
                    0.5f,
                    1.5f // Higher pitch for distinction
            );

            // Notify player for debugging
            player.sendMessage(
                    Text.literal("A Flame Seed has appeared nearby! Find it before it disappears!")
                            .formatted(Formatting.GOLD),
                    true
            );
        } else {
            System.out.println("[FireRainEvent] Could not find valid position for flame seed");
        }
    }
    /**
     * Update the lifetime of flame seed particles and remove expired ones
     * @param world The server world
     */
    private void updateFlameSeedLifetimes(ServerWorld world) {
        Iterator<Map.Entry<BlockPos, Integer>> iterator = flameSeedPositions.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            BlockPos pos = entry.getKey();
            int lifetime = entry.getValue() - FLAME_SEED_CHECK_INTERVAL;

            if (lifetime <= 0) {
                // Remove expired flame seeds
                iterator.remove();

                // Play extinguish effect
                world.spawnParticles(
                        ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                        5, // Count
                        0.2, 0.1, 0.2, // Spread
                        0.02 // Speed
                );
            } else {
                // Update the lifetime
                entry.setValue(lifetime);

                // Periodically refresh the particle effect to keep it visible
                if (lifetime % 20 == 0) {
                    world.spawnParticles(
                            ParticleTypes.SOUL_FIRE_FLAME,
                            pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                            1, // Count - just one for maintenance
                            0.1, 0.0, 0.1, // Minimal spread
                            0.01 // Very slow drift
                    );
                }
            }
        }
    }

    /**
     * Check if players are walking on flame seeds and give them flame seeds if they do
     * @param world The server world
     */
    private void checkFlameSeedCollisions(ServerWorld world) {
        // Create a temporary list to store positions to remove
        List<BlockPos> positionsToRemove = new ArrayList<>();



        for (Map.Entry<BlockPos, Integer> entry : flameSeedPositions.entrySet()) {
            BlockPos seedPos = entry.getKey();

            // Create a slightly larger box around the flame seed for better detection
            Box seedBox = new Box(
                    seedPos.getX(), seedPos.getY(), seedPos.getZ(),
                    seedPos.getX() + 1, seedPos.getY() + 0.5, seedPos.getZ() + 1
            );

            // Check all players against this seed
            for (PlayerEntity player : world.getPlayers()) {
                // Skip creative/spectator players
                if (player.isCreative() || player.isSpectator()) {
                    continue;
                }

                // Get player bounds - use the player's actual bounding box
                Box playerBox = player.getBoundingBox();

                // Check for collision
                if (playerBox.intersects(seedBox)) {
                    // Player has walked on the flame seed - give them a flame seed item
                    giveFlameSeedToPlayer(player, world, seedPos);
                    StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 100, 0, false, false, false);

                    // Mark this position for removal
                    positionsToRemove.add(seedPos);

                    // Visual and audio effects for collection
                    world.spawnParticles(
                            ParticleTypes.FLAME,
                            seedPos.getX() + 0.5, seedPos.getY() + 0.5, seedPos.getZ() + 0.5,
                            15, // Count - burst of particles
                            0.3, 0.3, 0.3, // Spread
                            0.1 // Speed
                    );

                    world.playSound(
                            null,
                            seedPos.getX() + 0.5, seedPos.getY() + 0.5, seedPos.getZ() + 0.5,
                            SoundEvents.ENTITY_BLAZE_SHOOT,
                            SoundCategory.PLAYERS,
                            0.7f,
                            1.0f + world.random.nextFloat() * 0.3f
                    );

                    // No need to check other players for this seed
                    break;
                }
            }
        }

        // Remove all marked positions after iteration is complete
        for (BlockPos pos : positionsToRemove) {
            flameSeedPositions.remove(pos);
        }

    }

    /**
     * Give a flame seed item to a player
     * @param player The player to give the item to
     * @param world The server world
     * @param pos The position where the flame seed was collected
     */
    private void giveFlameSeedToPlayer(PlayerEntity player, ServerWorld world, BlockPos pos) {
        // Create a new flame seed item stack
        ItemStack flameSeed = new ItemStack(ModItems.FLAME_FLOWER_SEEDS);

        // Try to add the item to the player's inventory
        if (!player.giveItemStack(flameSeed)) {
            // If the inventory is full, drop the item at the player's position
            player.dropItem(flameSeed, false);

            // Notify the player their inventory is full
            player.sendMessage(
                    Text.literal("Your inventory is full! Flame Seed dropped at your feet.")
                            .formatted(Formatting.YELLOW),
                    true
            );
        } else {
            // Notify the player they collected the item
            player.sendMessage(
                    Text.literal("You collected a Flame Seed from the fire rain!")
                            .formatted(Formatting.GOLD, Formatting.BOLD),
                    true
            );
        }

        // Play a sound at the player's location
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ITEM_PICKUP,
                SoundCategory.PLAYERS,
                0.2f,
                ((world.random.nextFloat() - world.random.nextFloat()) * 0.7f + 1.0f) * 2.0f
        );
    }


    /**
     * Spawn fire particles for the fire rain effect around each player
     * @param world The server world
     * @param isInitialBurst Whether this is the initial burst (more particles)
     */
    private void spawnFireRainParticles(ServerWorld world, boolean isInitialBurst) {
        // Skip if no players are in the dimension
        if (world.getPlayers().isEmpty()) {
            return;
        }

        // Calculate particle count based on intensity
        int particleMultiplier = isInitialBurst ? 5 : 1;
        int particleCount = BASE_PARTICLE_COUNT * intensityLevel * particleMultiplier;

        // Iterate through each player and spawn particles around them
        for (PlayerEntity player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();

            for (int i = 0; i < particleCount; i++) {
                // Randomize position around player
                double offsetX = (random.nextDouble() - 0.5) * PARTICLE_SPREAD_RADIUS * 2;
                double offsetZ = (random.nextDouble() - 0.5) * PARTICLE_SPREAD_RADIUS * 2;

                // Position to spawn the particle
                double x = playerPos.getX() + offsetX;
                double y = playerPos.getY() + PARTICLE_HEIGHT;
                double z = playerPos.getZ() + offsetZ;

                // Randomize velocity slightly for natural movement
                double velX = (random.nextDouble() - 0.5) * 0.02;
                double velY = -0.2 - (random.nextDouble() * 0.3 * intensityLevel); // Faster fall with higher intensity
                double velZ = (random.nextDouble() - 0.5) * 0.02;

                // Choose particle type based on intensity and randomization
                ParticleEffect particleType = getFireParticleType();

                // Spawn the particle
                world.spawnParticles(
                        particleType,
                        x, y, z,
                        1, // Count - just one particle per position
                        velX, velY, velZ,
                        0.1 // Speed modifier
                );
                world.spawnParticles(
                        ParticleTypes.LAVA,
                        x, y, z,
                        1, // Count
                        0, 0, 0, // No velocity for lava particles
                        0.1 // Speed modifier
                );

                // For higher intensities, add additional ember and smoke particles
                if (intensityLevel > 1 && random.nextFloat() < 0.3) {
                    world.spawnParticles(
                            ParticleTypes.SMOKE,
                            x, y, z,
                            1, // Count
                            velX * 0.5, velY * 0.5, velZ * 0.5,
                            0.05 // Speed modifier
                    );
                    world.spawnParticles(
                            ParticleTypes.LAVA,
                            x, y, z,
                            2, // Count
                            0, 0, 0, // No velocity for lava particles
                            0.1 // Speed modifier
                    );
                }

                // For highest intensity, occasionally add lava particles
                if (intensityLevel == 3 && random.nextFloat() < 0.15) {
                    world.spawnParticles(
                            ParticleTypes.LAVA,
                            x, y, z,
                            3, // Count
                            0, 0, 0, // No velocity for lava particles
                            0.1 // Speed modifier
                    );
                }
            }

            // Spawn impact particles where fire hits the ground
            if (intensityLevel > 1 && random.nextFloat() < 0.3) {
                spawnImpactParticles(world, playerPos);
            }
        }
    }

    /**
     * Spawn particles to simulate fire drops hitting the ground
     */
    private void spawnImpactParticles(ServerWorld world, BlockPos playerPos) {
        int impactCount = intensityLevel * 2;

        for (int i = 0; i < impactCount; i++) {
            // Random position around player on the ground
            double offsetX = (random.nextDouble() - 0.5) * PARTICLE_SPREAD_RADIUS;
            double offsetZ = (random.nextDouble() - 0.5) * PARTICLE_SPREAD_RADIUS;

            // Find the top block at this position
            BlockPos impactPos = world.getTopPosition(
                    net.minecraft.world.Heightmap.Type.WORLD_SURFACE,
                    new BlockPos(playerPos.getX() + offsetX, 0, playerPos.getZ() + offsetZ)
            );

            // Only spawn impact particles if the block is visible from above
            if (world.isSkyVisible(impactPos)) {
                double x = impactPos.getX() + random.nextDouble();
                double y = impactPos.getY() + 0.1;
                double z = impactPos.getZ() + random.nextDouble();

                // Small ember burst on impact
                world.spawnParticles(
                        ParticleTypes.FLAME,
                        x, y, z,
                        3 + random.nextInt(3), // 3-5 particles
                        0.2, 0.1, 0.2, // Spread
                        0.05 // Speed
                );

                // Add small smoke puff for effect
                world.spawnParticles(
                        ParticleTypes.SMOKE,
                        x, y, z,
                        1 + random.nextInt(2), // 1-2 particles
                        0.1, 0.1, 0.1, // Spread
                        0.02 // Speed
                );

                // High intensity gets extra lava impact particles
                if (intensityLevel == 3 && random.nextFloat() < 0.3) {
                    world.spawnParticles(
                            ParticleTypes.LAVA,
                            x, y, z,
                            1, // Count
                            0, 0, 0, // No velocity
                            0.1 // Speed
                    );
                }
            }
        }
    }

    /**
     * Get a fire particle type based on randomization and intensity
     * @return A particle effect appropriate for fire rain
     */
    private ParticleEffect getFireParticleType() {
        float chance = random.nextFloat();

        // For intensity 3 (severe), use more dramatic particles
        if (intensityLevel == 3) {
            if (chance < 0.4) return ParticleTypes.FLAME;
            if (chance < 0.7) return ParticleTypes.SMALL_FLAME;
            if (chance < 0.85) return ParticleTypes.LAVA;
            return new DustParticleEffect(new Vec3f(1.0F, 0.3F, 0.0F), 1.0F); // Orange dust
        }

        // For intensity 2 (moderate), use regular fire particles
        if (intensityLevel == 2) {
            if (chance < 0.6) return ParticleTypes.FLAME;
            if (chance < 0.9) return ParticleTypes.SMALL_FLAME;
            return new DustParticleEffect(new Vec3f(1.0F, 0.5F, 0.0F), 0.8F); // Light orange dust
        }

        // For intensity 1 (mild), use mostly embers
        if (chance < 0.7) return ParticleTypes.SMALL_FLAME;
        return new DustParticleEffect(new Vec3f(1.0F, 0.6F, 0.1F), 0.6F); // Ember-like dust
    }

    /**
     * Create random fires on the ground for maximum intensity
     */
    private void createRandomFires(ServerWorld world) {
        for (PlayerEntity player : world.getPlayers()) {
            // Skip if too few players online
            if (world.getPlayers().size() < 1) {
                return;
            }

            // Pick a random player and place fires around them
            if (random.nextFloat() < 0.3f) { // 30% chance per player
                int fires = 1 + random.nextInt(3); // 1-3 fires

                for (int i = 0; i < fires; i++) {
                    int range = 10 + (intensityLevel * 5); // 15-25 block range
                    int x = player.getBlockPos().getX() + (random.nextInt(range * 2) - range);
                    int z = player.getBlockPos().getZ() + (random.nextInt(range * 2) - range);

                    // Find the top block
                    BlockPos pos = new BlockPos(x, player.getBlockPos().getY(), z);
                    BlockPos topBlock = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, pos);

                    // Only place fire if the player is exposed to the sky
                    if (world.isSkyVisible(topBlock)) {
                        // Check if we can place fire here (air block with solid block beneath)
                        BlockPos firePos = topBlock.up();
                        if (world.isAir(firePos) && world.getBlockState(topBlock).isSolidBlock(world, topBlock)) {
                            world.setBlockState(firePos, net.minecraft.block.Blocks.FIRE.getDefaultState());

                            // Add fire placement particles and sound
                            world.spawnParticles(
                                    ParticleTypes.FLAME,
                                    firePos.getX() + 0.5, firePos.getY() + 0.5, firePos.getZ() + 0.5,
                                    10, // Count
                                    0.5, 0.5, 0.5, // Spread
                                    0.1 // Speed
                            );

                            world.playSound(
                                    null, // No player
                                    firePos.getX(), firePos.getY(), firePos.getZ(),
                                    SoundEvents.ENTITY_GENERIC_BURN,
                                    SoundCategory.BLOCKS,
                                    0.5f + (random.nextFloat() * 0.5f),
                                    0.7f + (random.nextFloat() * 0.6f)
                            );
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if players are exposed to the fire rain and apply damage
     */
    private void checkPlayerExposure(ServerWorld world) {
        for (PlayerEntity player : world.getPlayers()) {
            // Skip players in creative or spectator mode
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            // Check if the player is exposed to the sky (not under cover)
            if (isExposedToSky(world, player)) {
                // Scale damage with intensity
                float damage = DAMAGE_AMOUNT * intensityLevel;

                // Apply fire damage
                player.damage(DAMAGE_SOURCE, damage);

                // Apply fire effect for longer duration with higher intensity
                int fireDuration = 20 + (intensityLevel * 20); // 1-3 seconds of fire
                player.setFireTicks(player.getFireTicks() + fireDuration);

                // Add fire particles directly on the player for visual effect
                world.spawnParticles(
                        ParticleTypes.FLAME,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        5 + intensityLevel * 3, // 8-14 particles based on intensity
                        0.2, 0.5, 0.2, // Spread
                        0.05 // Speed
                );

                // Notify player (only sometimes to avoid spam)
                if (random.nextFloat() < 0.3f) {
                    player.sendMessage(Text.literal("You're being burned by the fire rain!")
                            .formatted(Formatting.RED), true);
                }
            }
        }
    }

    /**
     * Check if a player is exposed to the sky
     */
    private boolean isExposedToSky(ServerWorld world, PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();

        // The height to check above the player
        int checkHeight = 10;

        // Check if there are any blocks above the player
        for (int y = 1; y <= checkHeight; y++) {
            BlockPos checkPos = playerPos.up(y);
            if (!world.isAir(checkPos)) {
                return false; // Player is covered
            }
        }

        return true; // Player is exposed
    }

    /**
     * Restore the previous weather state when the event completes
     */
    public void onComplete(ServerWorld world) {
        // Final burst of particles as the event ends
        for (PlayerEntity player : world.getPlayers()) {
            // Create a visual "fizzling out" effect with smoke particles
            BlockPos playerPos = player.getBlockPos();
            for (int i = 0; i < 50; i++) {
                double offsetX = (random.nextDouble() - 0.5) * PARTICLE_SPREAD_RADIUS * 2;
                double offsetZ = (random.nextDouble() - 0.5) * PARTICLE_SPREAD_RADIUS * 2;

                world.spawnParticles(
                        ParticleTypes.SMOKE,
                        playerPos.getX() + offsetX,
                        playerPos.getY() + PARTICLE_HEIGHT / 2.0 + random.nextDouble() * PARTICLE_HEIGHT / 2.0,
                        playerPos.getZ() + offsetZ,
                        1, // Count
                        0, -0.02, 0, // Slight downward drift
                        0.05 // Speed
                );
            }
        }

        // Restore previous weather state
        int normalWeatherDuration = 6000; // 5 minutes

        if (wasRaining) {
            if (wasThundering) {
                world.setWeather(0, normalWeatherDuration, true, true);
            } else {
                world.setWeather(0, normalWeatherDuration, true, false);
            }
        } else {
            world.setWeather(normalWeatherDuration, 0, false, false);
        }

        // Notify players that the event has ended
        world.getPlayers().forEach(player ->
                player.sendMessage(Text.literal("The fire rain has ended.")
                        .formatted(Formatting.GOLD), true)
        );
    }

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
        return 20 * 60 * 2;
    }
}