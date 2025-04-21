package end3r.verdant_arcanum.event;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import end3r.verdant_arcanum.registry.ModItems;

import java.util.*;

public class StrongWindsEvent implements CustomWorldEvent {
    public static final Identifier ID = new Identifier("verdant_arcanum", "strong_winds");
    public static final Identifier SOUND_ID = new Identifier("verdant_arcanum", "ambient.strong_wind");
    public static final SoundEvent STRONG_WIND_SOUND = new SoundEvent(SOUND_ID);

    private static final int DEFAULT_DURATION_TICKS = 250 * 32; // 32 seconds to match sound
    private int ticksRemaining = DEFAULT_DURATION_TICKS;
    private Vec3d windDirection = Vec3d.ZERO;
    private boolean hasStartedFade = false;
    private static final int FADE_OUT_TICKS = 20 * 10; // 10 second fade-out
    private int strength = 2; // Default medium strength (1=mild, 2=moderate, 3=severe)
    private boolean userDefinedDirection = false;
    public static final Identifier WIND_PACKET_ID = new Identifier("verdant_arcanum", "wind_direction");

    // For gust seed particles
    private static final Vec3f GUST_SEED_PARTICLE_COLOR = new Vec3f(0.0f, 0.8f, 0.8f); // Cyan color
    private static final float GUST_SEED_PARTICLE_SIZE = 1.2f;
    private static final int GUST_SEED_SPAWN_CHANCE = 10; // 1 in X chance per spawn cycle
    private static final double GUST_SEED_COLLECTION_RADIUS = 1.0; // Player must be this close to collect

    // Track active gust seed particles
    private final Map<UUID, Vec3d> activeGustSeedParticles = new HashMap<>();
    private final Random random = new Random();

    /**
     * Set the strength of the wind event
     * @param strength 1=mild, 2=moderate, 3=severe
     */
    public void setStrength(int strength) {
        this.strength = Math.max(1, Math.min(3, strength)); // Clamp between 1-3
    }

    /**
     * Set the duration of the wind event in ticks
     * @param ticks Duration in ticks (20 ticks = 1 second)
     */
    public void setDuration(int ticks) {
        this.ticksRemaining = Math.max(20, ticks); // Minimum 1 second
    }

    /**
     * Set the wind direction manually
     * @param direction Direction vector (will be normalized)
     */
    public void setWindDirection(Vec3d direction) {
        // Only set if not zero vector
        if (direction.x != 0 || direction.z != 0) {
            this.windDirection = new Vec3d(direction.x, 0, direction.z).normalize();
            this.userDefinedDirection = true;
        }
    }

    @Override
    public void start(ServerWorld world) {
        // Adjust message based on strength
        String strengthDesc;
        switch (strength) {
            case 1:
                strengthDesc = "Mild";
                break;
            case 3:
                strengthDesc = "Severe";
                break;
            default:
                strengthDesc = "Strong";
                break;
        }

        world.getPlayers().forEach(player ->
                player.sendMessage(Text.literal("\uD83C\uDF2C️ " + strengthDesc + " Winds sweep across the land..."), true)
        );

        // Only generate a random direction if not user-defined
        if (!userDefinedDirection) {
            // Pick base cardinal direction
            Vec3d baseDir = switch (new Random().nextInt(4)) {
                case 0 -> new Vec3d(1, 0, 0);   // East
                case 1 -> new Vec3d(-1, 0, 0);  // West
                case 2 -> new Vec3d(0, 0, 1);   // South
                default -> new Vec3d(0, 0, -1); // North
            };

            // Apply ±15° gust offset
            double angleOffset = Math.toRadians((new Random().nextDouble() - 0.5) * 30); // ±15°
            double cos = Math.cos(angleOffset);
            double sin = Math.sin(angleOffset);

            double newX = baseDir.x * cos - baseDir.z * sin;
            double newZ = baseDir.x * sin + baseDir.z * cos;

            windDirection = new Vec3d(newX, 0, newZ).normalize();
        }

        // Move existing particles immediately when the wind starts
        moveExistingParticles(world);

        sendWindDataToClients(world);
    }

    /**
     * Attempt to move existing particles in the world when wind starts
     * Note: This is challenging in Minecraft as particles aren't typical entities
     * This implementation uses a client-side particle packet approach
     */
    private void moveExistingParticles(ServerWorld world) {
        try {
            // Find all particles within range of players
            for (PlayerEntity player : world.getPlayers()) {
                Vec3d playerPos = player.getPos();

                // Define search area around player
                double searchRadius = 30.0;
                Box searchBox = new Box(
                        playerPos.getX() - searchRadius,
                        playerPos.getY() - searchRadius,
                        playerPos.getZ() - searchRadius,
                        playerPos.getX() + searchRadius,
                        playerPos.getY() + searchRadius,
                        playerPos.getZ() + searchRadius
                );

                // Using the Entity.getEntitiesByType method to find particles
                // Since particles aren't typical entities, we need to use the ParticleEffect system

                // First, find all existing particle emitters in range
                world.getEntitiesByClass(Entity.class, searchBox,
                                entity -> entity instanceof net.minecraft.particle.ParticleEffect)
                        .forEach(entity -> {
                            // Apply wind force to existing particle entities
                            Vec3d velocity = entity.getVelocity();
                            float particleSpeed = 0.2f * strength;
                            entity.setVelocity(
                                    velocity.x + windDirection.x * particleSpeed,
                                    velocity.y + 0.05 * strength, // Slight upward drift
                                    velocity.z + windDirection.z * particleSpeed
                            );
                            entity.velocityModified = true;
                        });

                // For gameplay effects, also spawn a burst of wind particles in the wind direction
                int particleCount = strength * 20;
                for (int i = 0; i < particleCount; i++) {
                    double offsetX = (new Random().nextDouble() - 0.5) * 10;
                    double offsetY = (new Random().nextDouble() - 0.5) * 5;
                    double offsetZ = (new Random().nextDouble() - 0.5) * 10;

                    Vec3d particlePos = playerPos.add(offsetX, offsetY, offsetZ);

                    float particleSpeed = 0.1f * strength;
                    world.spawnParticles(
                            new DustParticleEffect(new Vec3f(0.8f, 0.8f, 0.9f), 0.8f),
                            particlePos.x, particlePos.y, particlePos.z,
                            1, // count
                            windDirection.x * particleSpeed, // speed X
                            0.02, // speed Y
                            windDirection.z * particleSpeed, // speed Z
                            0.1 * strength // max speed
                    );
                }

                // Create "wind stream" effect along the wind direction
                for (int i = 0; i < 5; i++) {
                    // Start position ahead of player in wind direction
                    Vec3d startPos = playerPos.add(
                            -windDirection.x * 10,
                            1.5 + new Random().nextDouble() * 3,
                            -windDirection.z * 10
                    );

                    // Create stream of particles moving in wind direction
                    for (int j = 0; j < 15; j++) {
                        Vec3d streamPos = startPos.add(windDirection.multiply(j * 1.5));
                        world.spawnParticles(
                                new DustParticleEffect(new Vec3f(0.85f, 0.85f, 0.95f), 0.7f),
                                streamPos.x, streamPos.y, streamPos.z,
                                1,
                                windDirection.x * 0.2 * strength,
                                0.01,
                                windDirection.z * 0.2 * strength,
                                0.05 * strength
                        );
                    }
                }
            }
        } catch (Exception e) {
            // Graceful fallback if there's any issue with particle handling
            System.out.println("Error while attempting to move existing particles: " + e.getMessage());
        }
    }

    @Override
    public void tick(ServerWorld world) {
        if (ticksRemaining % 20 == 0) { // Update once per second
            sendWindDataToClients(world);
        }

        // In the tick method, add additional particles showing wind movement
        if (ticksRemaining % 5 == 0) {
            for (PlayerEntity player : world.getPlayers()) {
                Vec3d playerPos = player.getPos();

                // Create wind "streams" in the direction of the wind
                for (int i = 0; i < 3; i++) {
                    double offsetX = (new Random().nextDouble() - 0.5) * 10;
                    double offsetY = (new Random().nextDouble() - 0.5) * 5 + 1.5;
                    double offsetZ = (new Random().nextDouble() - 0.5) * 10;

                    Vec3d startPos = playerPos.add(offsetX, offsetY, offsetZ);

                    for (int j = 0; j < 8; j++) {
                        Vec3d particlePos = startPos.add(windDirection.multiply(j * 2));

                        // Set velocity in the wind direction with increasing speed
                        float baseSpeed = 0.05f * strength;
                        float speedMultiplier = (j + 1) / 4.0f; // Particles further along move faster

                        world.spawnParticles(
                                new DustParticleEffect(new Vec3f(0.8f, 0.8f, 0.95f), 0.8f),
                                particlePos.x, particlePos.y, particlePos.z,
                                1, // count - emit just one particle at each position
                                windDirection.x * 0.1, // x velocity component
                                0.02, // slight upward drift
                                windDirection.z * 0.1, // z velocity component
                                baseSpeed * speedMultiplier // overall speed - increases along the stream
                        );

                        // Add a few smaller "trail" particles behind each main particle
                        if (j % 2 == 0) {
                            world.spawnParticles(
                                    new DustParticleEffect(new Vec3f(0.85f, 0.85f, 1.0f), 0.5f),
                                    particlePos.x - (windDirection.x * 0.5),
                                    particlePos.y + 0.2,
                                    particlePos.z - (windDirection.z * 0.5),
                                    2, // count - emit two smaller particles
                                    windDirection.x * 0.15, // slightly different x velocity
                                    0.01, // minimal upward drift
                                    windDirection.z * 0.15, // slightly different z velocity
                                    baseSpeed * speedMultiplier * 0.7f // slightly slower
                            );
                        }
                    }
                }
            }
        }

        // Spawn special cyan gust seed particles occasionally
        spawnGustSeedParticles(world);

        // Check for players collecting gust seed particles
        checkGustSeedCollection(world);

        // Check if the event has finished
        if (ticksRemaining <= 0) {
            return;
        }

        // Decrease remaining time
        ticksRemaining--;

        // Calculate fade factor (1.0 = full strength, 0.0 = no effect)
        float fadeMultiplier = 1.0f;
        if (ticksRemaining < FADE_OUT_TICKS) {
            // Use a quadratic easing for more gradual fade at the beginning
            float linearFade = ticksRemaining / (float)FADE_OUT_TICKS;
            fadeMultiplier = linearFade * linearFade;

            // Mark that we're in the fade-out phase
            if (!hasStartedFade) {
                hasStartedFade = true;
                world.getPlayers().forEach(player ->
                        player.sendMessage(Text.literal("The winds begin to calm..."), true)
                );
            }
        }

        // Scale effects by strength
        float strengthMultiplier = strength / 2.0f; // 0.5 for mild, 1.0 for moderate, 1.5 for severe

        for (PlayerEntity player : world.getPlayers()) {
            // Apply more gentle fading to velocity
            float velocityMultiplier = fadeMultiplier * 0.9f + 0.1f; // Range from 0.1 to 1.0 instead of 0 to 1.0
            Vec3d velocity = player.getVelocity();

            // Scale push force by strength
            double pushForce = 0.05 * strengthMultiplier;
            player.setVelocity(velocity.add(windDirection.multiply(pushForce * velocityMultiplier)));
            player.velocityModified = true;

            Vec3d origin = player.getPos();
            double px = origin.x - windDirection.z * 0.5;
            double pz = origin.z + windDirection.x * 0.5;

            // Particles fade more slowly than other effects
            float particleFactor = (float)Math.sqrt(fadeMultiplier); // Square root for slower particle reduction

            // Scale particle effects by strength
            int particleCount = Math.max(1, (int)(5 * particleFactor * strengthMultiplier));
            float particleSpeed = 0.05f * (0.5f + fadeMultiplier * 0.5f) * strengthMultiplier;

            world.spawnParticles(
                    new DustParticleEffect(new Vec3f(0.8f, 0.8f, 0.9f), 1.0f),
                    px, origin.y + 1.2, pz,
                    particleCount, 0.1, 0.1, 0.1, particleSpeed
            );

            // Sound fades gradually but maintains presence longer
            // Scale sound frequency by strength
            int soundInterval = strength == 3 ? 20 : strength == 2 ? 40 : 60;
            if (ticksRemaining % (fadeMultiplier < 0.3f ? soundInterval * 2 : soundInterval) == 0) {
                // Cubic easing for sound volume - stays louder longer
                float volumeFactor = fadeMultiplier * fadeMultiplier * fadeMultiplier;
                float volume = (0.4f + 0.8f * volumeFactor) * strengthMultiplier; // Range scales with strength
                float pitch = 0.9f + 0.2f * fadeMultiplier; // Slightly lower pitch as wind dies down
                player.playSound(STRONG_WIND_SOUND, SoundCategory.AMBIENT, volume, pitch);
            }

            // More gradual status effect reduction
            if (fadeMultiplier > 0.2f) {
                // Scale effect amplifier by strength
                int baseAmplifier = strength - 2; // -1 for mild, 0 for moderate, 1 for severe
                int effectAmplifier = fadeMultiplier > 0.6f ? baseAmplifier : baseAmplifier - 1;

                // Don't apply slowness for mild winds during fade-out
                if (!(strength == 1 && fadeMultiplier <= 0.6f)) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, effectAmplifier, false, false));
                }
            }
        }

        // Apply effects to projectiles and entities
        affectAllEntities(world, fadeMultiplier * strengthMultiplier);

        // Handle block pushing with graduated frequency based on fade and strength
        int blockPushInterval = strength == 3 ? 5 : strength == 2 ? 10 : 20;
        if (ticksRemaining % Math.max(5, (int)(blockPushInterval / fadeMultiplier)) == 0) {
            pushLightBlocks(world);
        }
    }

    /**
     * Spawn special cyan particles that represent gust seeds
     */
    private void spawnGustSeedParticles(ServerWorld world) {
        // Only spawn new seed particles every 20 ticks (1 second)
        if (ticksRemaining % 20 != 0) {
            return;
        }

        // Cap the maximum number of active gust seed particles based on strength
        int maxParticles = 5 + (strength * 5);
        if (activeGustSeedParticles.size() >= maxParticles) {
            return;
        }

        // Generate particles around players
        for (PlayerEntity player : world.getPlayers()) {
            // Skip if player is too far away
            if (random.nextInt(GUST_SEED_SPAWN_CHANCE) != 0) {
                continue;
            }

            Vec3d playerPos = player.getPos();

            // Generate particle at a random position in the direction against the wind
            double distanceFromPlayer = 15.0 + (random.nextDouble() * 10.0);

            // Position against the wind direction
            Vec3d spawnPos = playerPos.add(
                    -windDirection.x * distanceFromPlayer + (random.nextDouble() - 0.5) * 10,
                    1.5 + random.nextDouble() * 3,
                    -windDirection.z * distanceFromPlayer + (random.nextDouble() - 0.5) * 10
            );

            // Create a UUID for this particle to track it
            UUID particleId = UUID.randomUUID();
            activeGustSeedParticles.put(particleId, spawnPos);

            // Spawn visible cyan particle
            world.spawnParticles(
                    // Cyan colored dust particle
                    new DustParticleEffect(GUST_SEED_PARTICLE_COLOR, GUST_SEED_PARTICLE_SIZE),
                    spawnPos.x, spawnPos.y, spawnPos.z,
                    1, // count - just one per location
                    0.0, 0.0, 0.0, // no random motion
                    0 // speed factor
            );

            // Add "glittering" effect around the main particle
            for (int i = 0; i < 3; i++) {
                world.spawnParticles(
                        new DustParticleEffect(new Vec3f(0.0f, 0.9f, 1.0f), 0.5f),
                        spawnPos.x + (random.nextDouble() - 0.5) * 0.5,
                        spawnPos.y + (random.nextDouble() - 0.5) * 0.5,
                        spawnPos.z + (random.nextDouble() - 0.5) * 0.5,
                        1,
                        (random.nextDouble() - 0.5) * 0.02,
                        (random.nextDouble() - 0.5) * 0.02,
                        (random.nextDouble() - 0.5) * 0.02,
                        0.01f
                );
            }
        }
    }

    /**
     * Move gust seed particles with the wind and check if players collect them
     */
    private void checkGustSeedCollection(ServerWorld world) {
        // Skip if no particles or no players
        if (activeGustSeedParticles.isEmpty() || world.getPlayers().isEmpty()) {
            return;
        }

        // Update positions of all active gust seed particles
        Iterator<Map.Entry<UUID, Vec3d>> iterator = activeGustSeedParticles.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, Vec3d> entry = iterator.next();
            UUID particleId = entry.getKey();
            Vec3d currentPos = entry.getValue();

            // Move the particle in the wind direction
            double moveSpeed = 0.15 * strength;
            Vec3d newPos = currentPos.add(
                    windDirection.x * moveSpeed,
                    random.nextDouble() * 0.05 - 0.025, // Slight vertical wobble
                    windDirection.z * moveSpeed
            );

            // Update position
            entry.setValue(newPos);

            // Spawn visual particle at the new position
            world.spawnParticles(
                    new DustParticleEffect(GUST_SEED_PARTICLE_COLOR, GUST_SEED_PARTICLE_SIZE),
                    newPos.x, newPos.y, newPos.z,
                    1,
                    0.0, 0.0, 0.0,
                    0
            );

            // Check if any player is close enough to collect
            for (PlayerEntity player : world.getPlayers()) {
                Vec3d playerPos = player.getPos().add(0, 1.0, 0); // Adjust to player eye level
                double distance = playerPos.distanceTo(newPos);

                if (distance <= GUST_SEED_COLLECTION_RADIUS) {
                    // Player collected the gust seed!
                    collectGustSeed(player, newPos, world);
                    iterator.remove();
                    break;
                }
            }

            // Remove particles that have travelled too far
            double traveledDistance = currentPos.distanceTo(newPos);
            if (traveledDistance > 50.0) {
                iterator.remove();
            }
        }
    }

    /**
     * Handle when a player collects a gust seed
     */
    private void collectGustSeed(PlayerEntity player, Vec3d particlePos, ServerWorld world) {
        // Give the player gust flower seeds
        ItemStack seedStack = new ItemStack(ModItems.GUST_FLOWER_SEEDS, 1);
        player.giveItemStack(seedStack);

        // Display message
        player.sendMessage(Text.literal("§b✦ You collected Gust Seeds from the wind!"), true);

        // Play sound
        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, 0.5f, 1.5f);

        // Visual effect for collection
        world.spawnParticles(
                new DustParticleEffect(new Vec3f(0.0f, 1.0f, 1.0f), 1.2f),
                particlePos.x, particlePos.y, particlePos.z,
                15, // More particles for collection effect
                0.2, 0.2, 0.2, // Spread out
                0.1f // Speed
        );
    }

    // Apply wind effects to mobs
    private void applyMobWindEffect(LivingEntity entity, float strengthFactor) {
        // Don't affect bosses or very large mobs
        if (entity.getWidth() > 2.0 || entity.getHeight() > 3.0 ||
                entity.getType() == EntityType.ENDER_DRAGON ||
                entity.getType() == EntityType.WITHER) {
            return;
        }

        // Calculate push force based on size
        float sizeFactor = 1.0f - (entity.getWidth() * 0.3f);
        sizeFactor = Math.max(0.2f, sizeFactor); // Minimum factor of 0.2

        // Apply wind force based on size and strength
        Vec3d velocity = entity.getVelocity();
        double pushForce = 0.12 * strengthFactor * sizeFactor;

        // Apply stronger push if entity is airborne
        if (!entity.isOnGround()) {
            pushForce *= 1.5;
        }

        // Add wind knockback
        entity.setVelocity(
                velocity.x + windDirection.x * pushForce,
                velocity.y,
                velocity.z + windDirection.z * pushForce
        );
        entity.velocityModified = true;

        // Visual indication of the wind affecting the mob
        if (entity.age % 10 == 0) {
            ((ServerWorld)entity.getWorld()).spawnParticles(
                    new DustParticleEffect(new Vec3f(0.8f, 0.8f, 1.0f), 1.0f),
                    entity.getX(), entity.getY() + entity.getHeight() * 0.7, entity.getZ(),
                    1, 0.2, 0.2, 0.2, 0.05 * strength
            );

        }

        // Add slight resistance to movement against the wind
        if (entity instanceof MobEntity) {
            MobEntity mob = (MobEntity)entity;

            // Fix for navigation check
            if (mob.getNavigation() != null && mob.getNavigation().getCurrentPath() != null) {
                Vec3d moveDirection = new Vec3d(
                        mob.getX() - mob.prevX,
                        0,
                        mob.getZ() - mob.prevZ
                ).normalize();

                // If moving against the wind, apply slowness effect
                if (moveDirection.dotProduct(windDirection) < -0.5) {
                    int amplifier = strength - 1; // 0 for mild, 1 for moderate, 2 for severe
                    mob.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS, 40, amplifier, false, false, false));
                }
            }
        }
    }

    /**
     * Apply wind effects to all applicable entities in the world
     */
    private void affectAllEntities(ServerWorld world, float strengthMultiplier) {
        // Get all players to establish search areas
        for (PlayerEntity player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();

            // Create a larger box to find entities
            Box searchBox = new Box(
                    playerPos.getX() - 50,
                    playerPos.getY() - 20,
                    playerPos.getZ() - 50,
                    playerPos.getX() + 50,
                    playerPos.getY() + 20,
                    playerPos.getZ() + 50
            );

            // Special case for items
            for (Entity entity : world.getEntitiesByType(EntityType.ITEM, searchBox, e -> true)) {
                // Apply stronger effect to items, even when on ground
                applyItemWindEffect(entity, strengthMultiplier * 2.0f);
            }

            // Handle other entities
            for (Entity entity : world.getEntitiesByClass(Entity.class, searchBox,
                    e -> !(e instanceof PlayerEntity || e instanceof ItemEntity))) {

                // Check if the entity is a mob (LivingEntity but not a player)
                if (entity instanceof LivingEntity) {
                    applyMobWindEffect((LivingEntity)entity, strengthMultiplier);
                } else {
                    applyWindToEntity(entity, strengthMultiplier);
                }
            }
        }
    }

    // Special handling for items on the ground
    private void applyItemWindEffect(Entity entity, float strengthFactor) {
        if (entity instanceof ItemEntity) {
            // Apply more aggressive effect to items
            Vec3d velocity = entity.getVelocity();

            // Stronger push force for items
            double pushForce = 0.2 * strengthFactor;

            // Add slight upward motion to help items "hop" when on ground
            double upwardForce = entity.isOnGround() ? 0.1 : 0.0;

            entity.setVelocity(
                    velocity.x + windDirection.x * pushForce,
                    velocity.y + upwardForce,
                    velocity.z + windDirection.z * pushForce
            );

            // Increase pickup delay to prevent players from easily grabbing the items
            ((ItemEntity)entity).setPickupDelay(10);

            entity.velocityModified = true;
        }
    }

    /**
     * Apply wind force to a specific entity
     */
    private void applyWindToEntity(Entity entity, float strengthFactor) {
        if (entity != null && !entity.isOnGround()) {
            Vec3d velocity = entity.getVelocity();
            double pushForce = 0.05 * strengthFactor;
            entity.setVelocity(velocity.add(windDirection.multiply(pushForce)));
            entity.velocityModified = true;
        }
    }

    private void pushLightBlocks(ServerWorld world) {
        // Scale search radius based on strength
        int baseRadius = 15 + strength * 5;
        int searchRadius = Math.min(baseRadius, 10 + (int)(ticksRemaining / 20));

        for (PlayerEntity player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();

            int radiusX = windDirection.x != 0 ? searchRadius : searchRadius / 2;
            int radiusZ = windDirection.z != 0 ? searchRadius : searchRadius / 2;

            Box searchBox = new Box(
                    playerPos.getX() - radiusX,
                    playerPos.getY() - 5,
                    playerPos.getZ() - radiusZ,
                    playerPos.getX() + radiusX,
                    playerPos.getY() + 5,
                    playerPos.getZ() + radiusZ
            );

            BlockPos.iterate(
                    (int)searchBox.minX, (int)searchBox.minY, (int)searchBox.minZ,
                    (int)searchBox.maxX, (int)searchBox.maxY, (int)searchBox.maxZ
            ).forEach(pos -> {
                BlockState state = world.getBlockState(pos);
                Block block = state.getBlock();

                boolean isLightBlock = block == Blocks.TORCH ||
                        block == Blocks.CANDLE ||
                        block == Blocks.REDSTONE_TORCH ||
                        block == Blocks.GLOWSTONE ||
                        block == Blocks.SOUL_LANTERN ||
                        block == Blocks.LILY_PAD ||
                        block == Blocks.FIRE ||
                        block == Blocks.SOUL_FIRE;

                if (strength == 3) {
                    isLightBlock = isLightBlock ||
                            block == Blocks.LANTERN ||
                            block == Blocks.FLOWER_POT ||
                            block == Blocks.BROWN_MUSHROOM ||
                            block == Blocks.RED_MUSHROOM;
                }

                if (isLightBlock) {
                    // Chance of converting to falling block based on strength
                    float moveChance = 0.2f * strength;

                    if (new Random().nextFloat() < moveChance) {
                        // Use spawnEntity with FallingBlockEntity.fall instead
                        FallingBlockEntity fallingBlock = FallingBlockEntity.spawnFromBlock(
                                world, pos, state);

                        if (fallingBlock != null) {
                            // Give initial velocity in wind direction
                            fallingBlock.setVelocity(
                                    windDirection.x * 0.3 * strength,
                                    0.1,
                                    windDirection.z * 0.3 * strength);

                            // Set to not drop item when landing
                            fallingBlock.dropItem = false;

                            // Remove the original block
                            world.setBlockState(pos, Blocks.AIR.getDefaultState());
                        }
                    }
                }
            });
        }
    }

    private Direction getPrimaryDirection() {
        if (Math.abs(windDirection.x) > Math.abs(windDirection.z)) {
            return windDirection.x > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return windDirection.z > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    @Override
    public boolean isComplete() {

        boolean completed = ticksRemaining <= 0;
        if (completed) {
            System.out.println("Wind event completed naturally");
        }



        return ticksRemaining <= 0;



    }

    @Override
    public Identifier getId() {
        return ID;
    }

    private void sendWindDataToClients(ServerWorld world) {
        // Create packet with wind direction and strength
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(windDirection.x);
        buf.writeDouble(windDirection.y);
        buf.writeDouble(windDirection.z);
        buf.writeFloat(strength);
        buf.writeBoolean(ticksRemaining > 0);

        // Send to all players in range
        for (PlayerEntity player : world.getPlayers()) {
            ServerPlayNetworking.send((ServerPlayerEntity)player, WIND_PACKET_ID, buf);
        }
    }

}