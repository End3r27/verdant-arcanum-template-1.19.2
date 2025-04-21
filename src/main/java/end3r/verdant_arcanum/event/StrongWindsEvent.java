package end3r.verdant_arcanum.event;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.DustParticleEffect;
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

import java.util.List;
import java.util.Random;

public class StrongWindsEvent implements CustomWorldEvent {
    public static final Identifier ID = new Identifier("verdant_arcanum", "strong_winds");
    public static final Identifier SOUND_ID = new Identifier("verdant_arcanum", "ambient.strong_wind");
    public static final SoundEvent STRONG_WIND_SOUND = new SoundEvent(SOUND_ID);

    private static final int DEFAULT_DURATION_TICKS = 200 * 32; // 32 seconds to match sound
    private int ticksRemaining = DEFAULT_DURATION_TICKS;
    private Vec3d windDirection = Vec3d.ZERO;
    private boolean hasStartedFade = false;
    private static final int FADE_OUT_TICKS = 20 * 10; // 10 second fade-out
    private int strength = 2; // Default medium strength (1=mild, 2=moderate, 3=severe)
    private boolean userDefinedDirection = false;

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
    }

    /**
     * Attempt to move existing particles in the world when wind starts
     * Note: This is challenging in Minecraft as particles aren't typical entities
     * This implementation uses a client-side particle packet approach
     */
    private void moveExistingParticles(ServerWorld world) {
        try {
            // Scale particle count based on strength
            int particleCount = strength * 20;

            // For gameplay effects, we can immediately spawn burst of particles in wind direction
            for (PlayerEntity player : world.getPlayers()) {
                Vec3d playerPos = player.getPos();

                // Create a wind gust particle effect
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
            }
        } catch (Exception e) {
            // Graceful fallback if there's any issue with particle handling
            System.out.println("Error while attempting to move existing particles: " + e.getMessage());
        }
    }

    @Override
    public void tick(ServerWorld world) {

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
     * Apply wind effects to all applicable entities in the world
     */
    private void affectAllEntities(ServerWorld world, float strengthMultiplier) {
        // Get all entities in loaded chunks
        for (Entity entity : world.getEntitiesByType(EntityType.ARROW, e -> true)) {
            applyWindToEntity(entity, strengthMultiplier * 0.8f);
        }

        for (Entity entity : world.getEntitiesByType(EntityType.SNOWBALL, e -> true)) {
            applyWindToEntity(entity, strengthMultiplier * 1.0f);
        }

        for (Entity entity : world.getEntitiesByType(EntityType.EGG, e -> true)) {
            applyWindToEntity(entity, strengthMultiplier * 1.0f);
        }

        for (Entity entity : world.getEntitiesByType(EntityType.FIREBALL, e -> true)) {
            applyWindToEntity(entity, strengthMultiplier * 0.5f);
        }

        for (Entity entity : world.getEntitiesByType(EntityType.EXPERIENCE_ORB, e -> true)) {
            applyWindToEntity(entity, strengthMultiplier * 1.2f);
        }

        // Affect items more strongly
        for (Entity entity : world.getEntitiesByType(EntityType.ITEM, e -> true)) {
            applyWindToEntity(entity, strengthMultiplier * 1.5f);
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

        // Reduce the search area during fade-out to improve performance
        int searchRadius = Math.min(baseRadius, 10 + (int)(ticksRemaining / 20));

        // Create a box around each player for searching
        for (PlayerEntity player : world.getPlayers()) {
            BlockPos playerPos = player.getBlockPos();

            // Iterate through blocks in the wind direction more than to the sides
            int radiusX = windDirection.x != 0 ? searchRadius : searchRadius / 2;
            int radiusZ = windDirection.z != 0 ? searchRadius : searchRadius / 2;

            // Search area that's biased in the wind direction
            Box searchBox = new Box(
                    playerPos.getX() - radiusX,
                    playerPos.getY() - 5,
                    playerPos.getZ() - radiusZ,
                    playerPos.getX() + radiusX,
                    playerPos.getY() + 5,
                    playerPos.getZ() + radiusZ
            );

            // Find light blocks in range that could be affected
            BlockPos.iterate(
                    (int)searchBox.minX, (int)searchBox.minY, (int)searchBox.minZ,
                    (int)searchBox.maxX, (int)searchBox.maxY, (int)searchBox.maxZ
            ).forEach(pos -> {
                BlockState state = world.getBlockState(pos);
                Block block = state.getBlock();

                // Expanded list of blocks that can be affected by wind
                boolean isLightBlock = block == Blocks.TORCH ||
                        block == Blocks.CANDLE ||
                        block == Blocks.REDSTONE_TORCH ||
                        block == Blocks.GLOWSTONE ||
                        block == Blocks.SOUL_LANTERN ||
                        block == Blocks.LILY_PAD ||
                        block == Blocks.FIRE ||
                        block == Blocks.SOUL_FIRE;

                // Additional chance for stronger winds to affect more blocks
                if (strength == 3) {
                    isLightBlock = isLightBlock ||
                            block == Blocks.LANTERN ||
                            block == Blocks.FLOWER_POT ||
                            block == Blocks.BROWN_MUSHROOM ||
                            block == Blocks.RED_MUSHROOM;
                }

                if (isLightBlock) {
                    Direction moveDir = getPrimaryDirection();
                    BlockPos target = pos.offset(moveDir);

                    // Don't try to place blocks in solid blocks
                    if (world.getBlockState(target).isAir()) {
                        // Chance of moving based on strength
                        float moveChance = 0.2f * strength;

                        if (new Random().nextFloat() < moveChance) {
                            world.setBlockState(target, state);
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
}