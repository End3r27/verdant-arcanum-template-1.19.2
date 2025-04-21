package end3r.verdant_arcanum.event;

import end3r.verdant_arcanum.registry.ModItems;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import java.util.*;

public class EndVeilEvent implements CustomWorldEvent {
    private static final Identifier ID = new Identifier("verdant_arcanum", "end_veil");

    // Event duration settings
    private static final int MIN_DURATION = 20 * 60 * 5; // 5 minutes minimum
    private static final int MAX_DURATION = 20 * 60 * 10; // 15 minutes maximum
    private int duration;
    private int ticks = 0;

    // Particle effect timers
    private int particleTimer = 0;
    private static int PARTICLE_INTERVAL = 20; // Spawn particles every second

    // Temporal shift settings
    private int timeShiftTimer = 0;
    private static final int TIME_SHIFT_MIN_INTERVAL = 20 * 60; // At least 1 minute between shifts
    private static int TIME_SHIFT_MAX_INTERVAL = 20 * 60 * 3; // At most 3 minutes between shifts
    private int nextTimeShift;

    // Chorus teleport effect tracking
    private final Map<PlayerEntity, Integer> playerTeleportCooldowns = new HashMap<>();
    private static final int TELEPORT_COOLDOWN = 20 * 5; // 5 seconds cooldown

    // Block levitation tracking
    private final List<BlockLevitation> levitatingBlocks = new ArrayList<>();
    private int levitationTimer = 0;
    private static final int LEVITATION_INTERVAL = 20 * 30; // Try to levitate blocks every 30 seconds

    // Terrain "flickering" settings
    private int terrainFlickerTimer = 0;
    private static int TERRAIN_FLICKER_INTERVAL = 20 * 15; // Every 15 seconds
    private boolean isFlickering = false;
    private int flickerDuration = 0;

    private static final int BLINK_VORTEX_INTERVAL = 20 * 20; // Create a new vortex every minute
    private static int blinkVortexTimer = 0;
    private static final Map<BlockPos, Integer> activeBlinkVortexes = new HashMap<>();
    private static final int VORTEX_DURATION = 30 * 20; // 30 seconds duration
    private static final double VORTEX_RADIUS = 2.5; // Size of the particle effect


    public EndVeilEvent() {
        Random random = Random.create();
        this.duration = MIN_DURATION + random.nextInt(MAX_DURATION - MIN_DURATION);
        this.nextTimeShift = TIME_SHIFT_MIN_INTERVAL + random.nextInt(TIME_SHIFT_MAX_INTERVAL - TIME_SHIFT_MIN_INTERVAL);
    }

    /**
     * Sets the intensity level of the End Veil event
     * @param intensity Level 1-3 (mild, moderate, intense)
     */
    public void setIntensity(int intensity) {
        // Adjust event parameters based on intensity
        switch (intensity) {
            case 1: // Mild
                // Reduce effect frequencies
                particleTimer = PARTICLE_INTERVAL * 2;
                terrainFlickerTimer = TERRAIN_FLICKER_INTERVAL * 2;
                timeShiftTimer = TIME_SHIFT_MIN_INTERVAL;
                break;
            case 2: // Moderate
                // Default settings, no changes needed
                break;
            case 3: // Intense
                // Increase effect frequencies
                PARTICLE_INTERVAL = 10; // More frequent particles
                TERRAIN_FLICKER_INTERVAL = (int)(TERRAIN_FLICKER_INTERVAL * 0.75); // More frequent flickering
                TIME_SHIFT_MAX_INTERVAL = (int)(TIME_SHIFT_MAX_INTERVAL * 0.75); // More frequent time shifts
                break;
        }
    }

    /**
     * Sets the duration of the End Veil event
     * @param ticks Duration in game ticks
     */
    public void setDuration(int ticks) {
        this.duration = ticks;
    }

    /**
     * Attempts to create a "blink vortex" in the world - a concentrated area of
     * End energies that can bestow blink flower seeds to players who enter it
     */
    private void processBlinkVortexes(ServerWorld world) {
        Random random = world.getRandom();

        // Attempt to create new vortexes on timer
        if (++blinkVortexTimer >= BLINK_VORTEX_INTERVAL) {
            blinkVortexTimer = 0;

            // Try to place vortex near a random player
            if (!world.getPlayers().isEmpty() && random.nextInt(3) == 0) { // 1/3 chance each interval
                PlayerEntity player = world.getPlayers().get(random.nextInt(world.getPlayers().size()));

                // Find a position near the player
                int distance = 8 + random.nextInt(8); // 8-15 blocks away
                double angle = random.nextDouble() * Math.PI * 2; // Random direction

                int xOffset = (int)(Math.sin(angle) * distance);
                int zOffset = (int)(Math.cos(angle) * distance);

                BlockPos basePos = player.getBlockPos().add(xOffset, 0, zOffset);

                // Find valid Y position (air with solid block below)
                BlockPos vortexPos = null;
                for (int y = -3; y <= 5; y++) {
                    BlockPos checkPos = basePos.add(0, y, 0);
                    if (world.isAir(checkPos) && world.isAir(checkPos.up())
                            && !world.isAir(checkPos.down())) {
                        vortexPos = checkPos;
                        break;
                    }
                }

                if (vortexPos != null) {
                    // Create the vortex
                    activeBlinkVortexes.put(vortexPos, VORTEX_DURATION);

                    // Notify nearby players
                    for (PlayerEntity nearbyPlayer : world.getPlayers()) {
                        if (nearbyPlayer.squaredDistanceTo(vortexPos.getX() + 0.5, vortexPos.getY() + 0.5, vortexPos.getZ() + 0.5) < 100) {
                            nearbyPlayer.sendMessage(Text.literal("A rift of End energy materializes nearby...").formatted(Formatting.DARK_PURPLE), true);
                        }
                    }

                    // Initial visual effect
                    world.spawnParticles(
                            ParticleTypes.REVERSE_PORTAL,
                            vortexPos.getX() + 0.5, vortexPos.getY() + 1.0, vortexPos.getZ() + 0.5,
                            30, 0.5, 0.5, 0.5, 0.05
                    );

                    // Sound effect
                    world.playSound(null, vortexPos,
                            SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                            SoundCategory.AMBIENT, 1.0f, 0.6f);
                }
            }
        }

        // Process existing vortexes
        Iterator<Map.Entry<BlockPos, Integer>> iterator = activeBlinkVortexes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            BlockPos pos = entry.getKey();
            int timeLeft = entry.getValue() - 1;

            if (timeLeft <= 0) {
                // Time expired, remove vortex with a particle burst
                world.spawnParticles(
                        ParticleTypes.DRAGON_BREATH,
                        pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        20, 0.5, 0.5, 0.5, 0.05
                );

                world.playSound(null, pos,
                        SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                        SoundCategory.AMBIENT, 0.6f, 0.8f);

                iterator.remove();
            } else {
                // Vortex still active, update time and visuals
                entry.setValue(timeLeft);

                // Constant particle effect
                if (world.getTime() % 5 == 0) {
                    // Swirling particles around the center
                    double heightOffset = 1.0 + Math.sin(world.getTime() * 0.05) * 0.2;

                    for (int i = 0; i < 3; i++) {
                        double angle = (world.getTime() * 0.1) + (i * (Math.PI * 2 / 3));
                        double radius = 0.3 + (Math.sin(world.getTime() * 0.02) * 0.2);

                        double px = pos.getX() + 0.5 + Math.sin(angle) * radius;
                        double pz = pos.getZ() + 0.5 + Math.cos(angle) * radius;

                        world.spawnParticles(
                                ParticleTypes.PORTAL,
                                px, pos.getY() + heightOffset, pz,
                                1, 0.05, 0.05, 0.05, 0.01
                        );
                    }

                    // Center particles
                    if (random.nextInt(3) == 0) {
                        world.spawnParticles(
                                ParticleTypes.DRAGON_BREATH,
                                pos.getX() + 0.5, pos.getY() + heightOffset, pos.getZ() + 0.5,
                                1, 0.2, 0.2, 0.2, 0.01
                        );
                    }
                }

                // Check for players entering the vortex area
                for (PlayerEntity player : world.getPlayers()) {
                    double distanceSquared = player.squaredDistanceTo(
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);

                    if (distanceSquared <= VORTEX_RADIUS * VORTEX_RADIUS) {
                        // Player entered the vortex
                        givePlayerBlinkSeeds(player, world);

                        // Consume the vortex with a dramatic effect
                        world.spawnParticles(
                                ParticleTypes.EXPLOSION,
                                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                                1, 0, 0, 0, 0
                        );

                        world.spawnParticles(
                                ParticleTypes.DRAGON_BREATH,
                                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                                30, 0.5, 0.5, 0.5, 0.1
                        );

                        world.playSound(null, pos,
                                SoundEvents.ENTITY_ENDER_DRAGON_GROWL,
                                SoundCategory.AMBIENT, 0.3f, 1.2f);

                        // Remove the vortex
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Gives blink flower seeds to a player who interacted with a blink vortex
     */
    private void givePlayerBlinkSeeds(PlayerEntity player, ServerWorld world) {
        // Create seeds item
        ItemStack seeds = new ItemStack(ModItems.BLINK_FLOWER_SEEDS);
        Random random = world.getRandom();
        int amount = 1 + random.nextInt(3); // Give 1-3 seeds
        seeds.setCount(amount);

        // Try to give item to player inventory
        boolean given = player.giveItemStack(seeds);

        // If inventory is full, drop item
        if (!given) {
            ItemEntity itemEntity = new ItemEntity(
                    world,
                    player.getX(), player.getY(), player.getZ(),
                    seeds
            );
            world.spawnEntity(itemEntity);
        }

        // Notification
        player.sendMessage(
                Text.literal("The End energies coalesce into " + amount + " Blink Flower Seeds!").formatted(Formatting.DARK_PURPLE),
                false
        );

        // Apply brief status effect
        player.addStatusEffect(new StatusEffectInstance(
                StatusEffects.BLINDNESS, 20, 0, false, false
        ));

        // Play sound to player
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0f, 0.6f);
    }





    @Override
    public void start(ServerWorld world) {
        // Reset all counters
        ticks = 0;
        particleTimer = 0;
        timeShiftTimer = 0;
        levitationTimer = 0;
        terrainFlickerTimer = 0;

        // Clear any existing effects
        playerTeleportCooldowns.clear();
        levitatingBlocks.clear();

        // Initial notification
        for (PlayerEntity player : world.getPlayers()) {
            player.sendMessage(Text.literal("The veil between dimensions thins... strange energies leak from The End."), false);
        }

        // Initial sound effect
        for (ServerPlayerEntity player : world.getPlayers()) {
            world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.AMBIENT, 0.7f, 0.6f);
        }
    }

    @Override
    public void tick(ServerWorld world) {
        ticks++;

        // Process particle effects
        if (++particleTimer >= PARTICLE_INTERVAL) {
            particleTimer = 0;
            spawnEndParticles(world);
        }

        // Process blink vortexes - add this line
        processBlinkVortexes(world);

        // Process chorus teleport effects
        processChorusEffects(world);



        // Process particle effects
        if (++particleTimer >= PARTICLE_INTERVAL) {
            particleTimer = 0;
            spawnEndParticles(world);
        }

        // Process chorus teleport effects
        processChorusEffects(world);

        // Process gravity distortion
        processGravityDistortion(world);

        // Process projectile distortion
        processProjectileDistortion(world);

        // Process block levitation
        if (++levitationTimer >= LEVITATION_INTERVAL) {
            levitationTimer = 0;
            tryLevitateRandomBlocks(world);
        }

        // Update existing levitating blocks
        updateLevitatingBlocks(world);

        // Process terrain flickering
        if (++terrainFlickerTimer >= TERRAIN_FLICKER_INTERVAL) {
            terrainFlickerTimer = 0;
            triggerTerrainFlicker(world);
        }

        // Process temporal shifts
        if (++timeShiftTimer >= nextTimeShift) {
            timeShiftTimer = 0;
            performTemporalShift(world);

            // Set next time shift
            Random random = world.getRandom();
            nextTimeShift = TIME_SHIFT_MIN_INTERVAL + random.nextInt(TIME_SHIFT_MAX_INTERVAL - TIME_SHIFT_MIN_INTERVAL);
        }

        // Update player teleport cooldowns
        updateTeleportCooldowns();
    }

    private void spawnEndParticles(ServerWorld world) {
        Random random = world.getRandom();

        // Process chorus teleport effects
        processChorusEffects(world);

        // Process gravity distortion
        processGravityDistortion(world);

        // Process projectile distortion
        processProjectileDistortion(world);

        // Process block levitation
        if (++levitationTimer >= LEVITATION_INTERVAL) {
            levitationTimer = 0;
            tryLevitateRandomBlocks(world);
        }

        // Update existing levitating blocks
        updateLevitatingBlocks(world);

        // Process terrain flickering
        if (++terrainFlickerTimer >= TERRAIN_FLICKER_INTERVAL) {
            terrainFlickerTimer = 0;
            triggerTerrainFlicker(world);
        }

        // Process temporal shifts
        if (++timeShiftTimer >= nextTimeShift) {
            timeShiftTimer = 0;
            performTemporalShift(world);

        }

        // Update player teleport cooldowns
        updateTeleportCooldowns();



        for (PlayerEntity player : world.getPlayers()) {
            // Spawn particles around players
            BlockPos playerPos = player.getBlockPos();
            int particleCount = 5 + random.nextInt(10);

            for (int i = 0; i < particleCount; i++) {
                double xOffset = random.nextDouble() * 10 - 5;
                double yOffset = random.nextDouble() * 5;
                double zOffset = random.nextDouble() * 10 - 5;

                Vec3d particlePos = new Vec3d(
                        playerPos.getX() + xOffset,
                        playerPos.getY() + yOffset,
                        playerPos.getZ() + zOffset
                );

                // Small chance for portal particles (more dramatic)
                if (random.nextInt(10) == 0) {
                    world.spawnParticles(
                            ParticleTypes.PORTAL,
                            particlePos.x, particlePos.y, particlePos.z,
                            1, 0.1, 0.1, 0.1, 0.01
                    );
                }
                // Regular end rod particles (main effect)
                else {
                    world.spawnParticles(
                            ParticleTypes.END_ROD,
                            particlePos.x, particlePos.y, particlePos.z,
                            1, 0.1, 0.1, 0.1, 0.01
                    );
                }
            }
        }
    }

    private void processChorusEffects(ServerWorld world) {
        for (PlayerEntity player : world.getPlayers()) {
            // Skip players on cooldown
            if (playerTeleportCooldowns.containsKey(player) && playerTeleportCooldowns.get(player) > 0) {
                continue;
            }

            Random random = world.getRandom();

            // 1% chance per tick when eating
            if (player.isUsingItem() && player.getActiveItem().isFood() && random.nextInt(100) == 0) {
                teleportPlayer(player, world, 0.5f);
            }

            // 10% chance when taking damage (this event happens less frequently)
            if (player.hurtTime > 0 && random.nextInt(10) == 0) {
                teleportPlayer(player, world, 1.5f);
            }

            // 5% chance when jumping from heights (falling more than 3 blocks)
            if (player.fallDistance > 3.0f && !player.isOnGround() && random.nextInt(20) == 0) {
                teleportPlayer(player, world, 1.0f);
                player.fallDistance = 0; // Reset fall distance after teleport
            }
        }
    }

    private void teleportPlayer(PlayerEntity player, ServerWorld world, float distanceMultiplier) {
        Random random = world.getRandom();

        // Calculate teleport distance (3-10 blocks, affected by multiplier)
        double distance = (3 + random.nextInt(8)) * distanceMultiplier;

        // Random direction
        double angle = random.nextDouble() * Math.PI * 2.0;

        // Calculate new position
        double newX = player.getX() + Math.sin(angle) * distance;
        double newZ = player.getZ() + Math.cos(angle) * distance;

        // Find safe Y position
        BlockPos targetPos = new BlockPos((int)newX, (int)player.getY(), (int)newZ);

        // Start from current Y and search for safe spot
        int maxY = 5; // Search up to 5 blocks up
        int minY = 10; // Search up to 10 blocks down

        boolean found = false;
        int newY = player.getBlockY();

        // First try going up
        for (int y = 0; y <= maxY && !found; y++) {
            BlockPos checkPos = targetPos.up(y);
            BlockPos checkPosAbove = checkPos.up();

            if (world.isAir(checkPos) && world.isAir(checkPosAbove) && !world.isAir(checkPos.down())) {
                newY = checkPos.getY();
                found = true;
            }
        }

        // If nothing found above, try going down
        if (!found) {
            for (int y = 1; y <= minY && !found; y++) {
                BlockPos checkPos = targetPos.down(y);
                BlockPos checkPosAbove = checkPos.up();

                if (world.isAir(checkPos) && world.isAir(checkPosAbove) && !world.isAir(checkPos.down())) {
                    newY = checkPos.getY();
                    found = true;
                }
            }
        }

        // If we found a valid position, teleport
        if (found) {
            // Store original position for particle effects
            double oldX = player.getX();
            double oldY = player.getY();
            double oldZ = player.getZ();

            // Teleport player
            player.teleport(newX, newY, newZ);

            // Play teleport sound
            world.playSound(null, oldX, oldY, oldZ, SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS, 0.7f, 1.0f);

            // Spawn particles at both locations
            for (int i = 0; i < 32; i++) {
                world.spawnParticles(
                        ParticleTypes.PORTAL,
                        oldX, oldY + random.nextDouble() * 2.0, oldZ,
                        1, random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5, 0.15
                );

                world.spawnParticles(
                        ParticleTypes.PORTAL,
                        newX, newY + random.nextDouble() * 2.0, newZ,
                        1, random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5, 0.15
                );
            }

            // Set cooldown
            playerTeleportCooldowns.put(player, TELEPORT_COOLDOWN);
        }
    }

    private void processGravityDistortion(ServerWorld world) {
        Random random = world.getRandom();

        for (PlayerEntity player : world.getPlayers()) {
            // Add slow falling effect to simulate reduced gravity
            // 20% chance per second to apply the effect if player doesn't already have it
            if (!player.hasStatusEffect(StatusEffects.SLOW_FALLING) && random.nextInt(100) < 4) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 20 * 5, 0, false, false));
            }

            // Random "glitches" in movement (subtle teleports)
            if (random.nextInt(1000) < 5 && player.isOnGround() && player.getVelocity().length() > 0.05) {
                // 0.5% chance when moving on ground
                double glitchDistance = random.nextDouble() * 0.5;
                Vec3d motion = player.getVelocity().normalize().multiply(glitchDistance);

                player.requestTeleport(
                        player.getX() + motion.x,
                        player.getY() + (random.nextDouble() * 0.2 - 0.1), // Small random vertical shift
                        player.getZ() + motion.z
                );

                // Particles to indicate movement glitch
                world.spawnParticles(
                        ParticleTypes.REVERSE_PORTAL,
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        5, 0.2, 0.2, 0.2, 0.05
                );
            }

            // Occasional jump boost
            if (player.getVelocity().y > 0.1 && !player.isOnGround() && random.nextInt(100) < 20) {
                // 20% chance when jumping
                Vec3d velocity = player.getVelocity();
                player.setVelocity(velocity.x, velocity.y * 1.25, velocity.z);
            }
        }
    }

    private void processProjectileDistortion(ServerWorld world) {
        Random random = world.getRandom();

        // Affect projectiles with weird trajectories
        for (ProjectileEntity entity : world.getEntitiesByType(EntityType.ARROW,
                new Box(-30000000, 0, -30000000, 30000000, world.getHeight(), 30000000),
                Objects::nonNull)) {

            // 2% chance per tick to distort trajectory
            if (random.nextInt(50) == 0) {
                Vec3d velocity = entity.getVelocity();
                double speed = velocity.length();
                if (speed > 0.1) {
                    // Apply sine wave motion
                    double time = world.getTime() * 0.1;
                    double verticalOffset = Math.sin(time) * 0.05;
                    double horizontalOffset = Math.cos(time) * 0.05;

                    entity.setVelocity(
                            velocity.x + horizontalOffset,
                            velocity.y + verticalOffset,
                            velocity.z + horizontalOffset,
                            (float)speed, 0
                    );

                    // Spawn particles to show distortion
                    world.spawnParticles(
                            ParticleTypes.END_ROD,
                            entity.getX(), entity.getY(), entity.getZ(),
                            1, 0, 0, 0, 0.01
                    );
                }
            }
        }
    }

    private void tryLevitateRandomBlocks(ServerWorld world) {
        Random random = world.getRandom();

        // Try to find blocks near players to levitate
        for (PlayerEntity player : world.getPlayers()) {
            // Skip if there are too many levitating blocks already
            if (levitatingBlocks.size() >= 20) {
                return;
            }

            BlockPos playerPos = player.getBlockPos();

            // Try multiple times to find valid blocks
            for (int attempt = 0; attempt < 5; attempt++) {
                int xOffset = random.nextInt(16) - 8;
                int yOffset = random.nextInt(8) - 2;
                int zOffset = random.nextInt(16) - 8;

                BlockPos targetPos = playerPos.add(xOffset, yOffset, zOffset);

                // Check if block is visible
                if (!world.isAir(targetPos) && world.isAir(targetPos.up())) {
                    BlockState blockState = world.getBlockState(targetPos);

                    // Don't levitate locked containers, spawners, etc.
                    if (isLevitationAllowed(blockState)) {
                        // Create levitation effect
                        float floatHeight = 0.5f + random.nextFloat();
                        int floatDuration = 20 * (3 + random.nextInt(5));

                        BlockLevitation levitation = new BlockLevitation(
                                targetPos,
                                blockState,
                                floatHeight,
                                floatDuration
                        );

                        levitatingBlocks.add(levitation);

                        // Visual effect
                        world.spawnParticles(
                                ParticleTypes.REVERSE_PORTAL,
                                targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5,
                                10, 0.4, 0.4, 0.4, 0.05
                        );

                        // Sound effect
                        world.playSound(null, targetPos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
                                SoundCategory.BLOCKS, 0.5f, 0.5f + random.nextFloat() * 0.5f);

                        break;
                    }
                }
            }
        }
    }

    private boolean isLevitationAllowed(BlockState state) {
        Block block = state.getBlock();

        // Exclude blocks that shouldn't levitate
        return block != Blocks.BEDROCK &&
                block != Blocks.OBSIDIAN &&
                block != Blocks.END_PORTAL_FRAME &&
                block != Blocks.END_PORTAL &&
                block != Blocks.NETHER_PORTAL &&
                block != Blocks.CHEST &&
                block != Blocks.TRAPPED_CHEST &&
                block != Blocks.ENDER_CHEST &&
                block != Blocks.SPAWNER &&
                block != Blocks.COMMAND_BLOCK &&
                block != Blocks.BARRIER;
    }

    private void updateLevitatingBlocks(ServerWorld world) {
        List<BlockLevitation> completed = new ArrayList<>();

        for (BlockLevitation levitation : levitatingBlocks) {
            // Update the levitation
            levitation.tick();

            // Visual effects
            if (world.getTime() % 10 == 0) {
                BlockPos pos = levitation.getPos();
                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        pos.getX() + 0.5, pos.getY() + levitation.getCurrentHeight(), pos.getZ() + 0.5,
                        1, 0.1, 0.1, 0.1, 0.01
                );
            }

            // Check if levitation is complete
            if (levitation.isComplete()) {
                // Return the block to its original state
                BlockPos pos = levitation.getPos();
                world.setBlockState(pos, levitation.getBlockState());

                // Add to completed list
                completed.add(levitation);

                // Sound and particles for "landing"
                world.playSound(null, pos, SoundEvents.BLOCK_STONE_PLACE,
                        SoundCategory.BLOCKS, 0.3f, 0.8f);

                world.spawnParticles(
                        ParticleTypes.POOF,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        5, 0.4, 0.2, 0.4, 0.05
                );
            }
        }

        // Remove completed levitations
        levitatingBlocks.removeAll(completed);
    }

    private void triggerTerrainFlicker(ServerWorld world) {
        Random random = world.getRandom();

        // Determine if we should start flickering
        if (!isFlickering && random.nextInt(3) == 0) {
            isFlickering = true;
            flickerDuration = 20 * (2 + random.nextInt(3)); // 2-4 seconds

            // Notify players
            for (PlayerEntity player : world.getPlayers()) {
                player.sendMessage(Text.literal("Reality flickers as The End bleeds through..."), true);
            }

            // Visual and sound effects
            for (PlayerEntity player : world.getPlayers()) {
                world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_END_PORTAL_FRAME_FILL,
                        SoundCategory.AMBIENT, 0.7f, 0.8f);
            }
        }
        // If already flickering, check if we should end it
        else if (isFlickering) {
            flickerDuration--;

            if (flickerDuration <= 0) {
                isFlickering = false;

                // Notify players it stopped
                for (PlayerEntity player : world.getPlayers()) {
                    player.sendMessage(Text.literal("The dimensional flicker subsides..."), true);
                }
            }
        }
    }

    private void performTemporalShift(ServerWorld world) {
        Random random = world.getRandom();

        // Only affect day/night cycle in the overworld
        if (world.getRegistryKey() == World.OVERWORLD && world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
            // Get current time
            long currentTime = world.getTimeOfDay();

            // Decide whether to shift forward or backward
            boolean shiftForward = random.nextBoolean();

            // Amount to shift (1000-3000 ticks, which is about 50 seconds to 2.5 minutes)
            long timeShift = 1000 + random.nextLong();
            if (!shiftForward) {
                timeShift = -timeShift;
            }

            // Apply the shift
            world.setTimeOfDay(currentTime + timeShift);

            // Notify players
            for (PlayerEntity player : world.getPlayers()) {
                String direction = shiftForward ? "forward" : "backward";
                player.sendMessage(Text.literal("Time stutters " + direction + " as dimensional energies clash..."), true);
            }

            // Visual effect
            for (PlayerEntity player : world.getPlayers()) {
                // Create a flash of particles
                world.spawnParticles(
                        ParticleTypes.FLASH,
                        player.getX(), player.getY() + 2, player.getZ(),
                        1, 0, 0, 0, 0
                );

                // End particles burst
                world.spawnParticles(
                        ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1, player.getZ(),
                        40, 3, 2, 3, 0.05
                );

                // Sound effect
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
                        SoundCategory.AMBIENT, 1.0f, 0.5f);
            }
        }
    }

    private void updateTeleportCooldowns() {
        // Update cooldowns
        List<PlayerEntity> toRemove = new ArrayList<>();

        for (Map.Entry<PlayerEntity, Integer> entry : playerTeleportCooldowns.entrySet()) {
            int newValue = entry.getValue() - 1;

            if (newValue <= 0) {
                toRemove.add(entry.getKey());
            } else {
                playerTeleportCooldowns.put(entry.getKey(), newValue);
            }
        }

        // Remove expired cooldowns
        for (PlayerEntity player : toRemove) {
            playerTeleportCooldowns.remove(player);
        }
    }

    @Override
    public boolean isComplete() {
        return ticks >= duration;
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    // Class to track and update levitating blocks
    private static class BlockLevitation {
        private final BlockPos pos;
        private final BlockState blockState;
        private final float maxHeight;
        private final int duration;
        private int timer = 0;
        private float currentHeight = 0;

        public BlockLevitation(BlockPos pos, BlockState blockState, float maxHeight, int duration) {
            this.pos = pos;
            this.blockState = blockState;
            this.maxHeight = maxHeight;
            this.duration = duration;
        }

        public void tick() {
            timer++;

            // Calculate current height using a sine wave pattern
            float progress = (float) timer / duration;

            // Full sine wave: 0 -> 1 -> 0
            if (progress <= 0.5f) {
                // Rising phase
                currentHeight = maxHeight * MathHelper.sin((float) (progress * Math.PI));
            } else {
                // Falling phase
                currentHeight = maxHeight * MathHelper.sin((float) ((1.0f - progress) * Math.PI));
            }
        }

        public boolean isComplete() {
            return timer >= duration;
        }

        public BlockPos getPos() {
            return pos;
        }

        public BlockState getBlockState() {
            return blockState;
        }

        public float getCurrentHeight() {
            return currentHeight;
        }

    }
}