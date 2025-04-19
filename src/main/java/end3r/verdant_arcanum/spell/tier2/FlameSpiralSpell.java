package end3r.verdant_arcanum.spell.tier2;

import end3r.verdant_arcanum.spell.Spell;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

/**
 * Flame Spiral spell implementation that creates a rotating ring of fire
 * around the caster, damaging nearby enemies over 2 seconds.
 */
public class FlameSpiralSpell implements Spell {
    // Mana cost for casting this spell
    private static final int MANA_COST = 65;

    // Effect radius in blocks
    private static final double EFFECT_RADIUS = 6.5;

    // Duration of the spell effect in ticks (2 seconds = 40 ticks)
    private static final int EFFECT_DURATION = 60;

    // Damage dealt per hit
    private static final float DAMAGE_PER_HIT = 3.0f;

    // How often to damage entities (every 10 ticks = 0.5 seconds)
    private static final int DAMAGE_INTERVAL = 10;

    // Total damage hits during the spell (4 hits over 2 seconds)
    private static final int TOTAL_DAMAGE_HITS = EFFECT_DURATION / DAMAGE_INTERVAL;

    // Track active spiral animations
    private static boolean animationActive = false;
    private static int animationTick = 0;
    private static PlayerEntity activePlayer = null;
    private static World activeWorld = null;

    @Override
    public String getType() {
        return "Flame Spiral";
    }

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        // Initial sound effect
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS,
                1.0F, 0.6F / (world.getRandom().nextFloat() * 0.2F + 0.9F));

        // Apply damage to entities in range
        applyDamageToEntities(world, player, 0);

        // Schedule the remaining damage hits
        for (int i = 1; i < TOTAL_DAMAGE_HITS; i++) {
            final int hitIndex = i;
            // This would normally use a task scheduler, but for simplicity we'll
            // rely on the client animation to sync roughly with these effects
            world.getServer().execute(() -> {
                // Make sure player is still valid
                if (player.isAlive() && !player.isRemoved()) {
                    applyDamageToEntities(world, player, hitIndex);

                    // Play whoosh sound at each damage interval
                    world.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS,
                            0.3F, 0.9F + (world.getRandom().nextFloat() * 0.2F));
                }
            });
        }
    }

    /**
     * Apply damage to all entities in range of the flame spiral
     */
    private void applyDamageToEntities(World world, PlayerEntity caster, int hitIndex) {
        // Get entities in range
        Box effectBox = new Box(
                caster.getX() - EFFECT_RADIUS, caster.getY() - 1, caster.getZ() - EFFECT_RADIUS,
                caster.getX() + EFFECT_RADIUS, caster.getY() + 2, caster.getZ() + EFFECT_RADIUS
        );

        List<LivingEntity> nearbyEntities = world.getEntitiesByClass(
                LivingEntity.class, effectBox, entity -> entity != caster
        );

        // Apply damage to each entity
        for (LivingEntity entity : nearbyEntities) {
            entity.damage(DamageSource.player(caster), DAMAGE_PER_HIT);

            // Set entity on fire for a short time (only on first hit)
            if (hitIndex == 0 && world.getRandom().nextFloat() < 0.4f) {
                entity.setOnFireFor(2);
            }
        }
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Start the spiral animation
        startSpiralAnimation(world, player);

        // Play initial flame particles around the player
        for (int i = 0; i < 20; i++) {
            double x = player.getX() + (world.getRandom().nextFloat() * 2 - 1) * 0.5;
            double y = player.getY() + player.getStandingEyeHeight() / 2;
            double z = player.getZ() + (world.getRandom().nextFloat() * 2 - 1) * 0.5;

            world.addParticle(
                    ParticleTypes.FLAME,
                    x, y, z,
                    0, 0.2, 0
            );
        }
    }

    /**
     * Initializes the spiral animation sequence
     */
    private void startSpiralAnimation(World world, PlayerEntity player) {
        // Set animation state
        animationActive = true;
        animationTick = 0;
        activePlayer = player;
        activeWorld = world;

        // Register tick handler if not already done
        if (!tickHandlerRegistered) {
            registerClientTickHandler();
        }
    }

    // Flag to ensure we only register the tick handler once
    private static boolean tickHandlerRegistered = false;

    /**
     * Register for client tick events to handle animation timing
     * Uses Fabric's ClientTickEvents system
     */
    private void registerClientTickHandler() {
        tickHandlerRegistered = true;

        // Register the client tick event
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (animationActive) {
                tickClientAnimation();
            }
        });
    }

    /**
     * Handles the client-side animation ticks
     */
    private void tickClientAnimation() {
        if (activePlayer == null || activeWorld == null) {
            animationActive = false;
            return;
        }

        // Update animation tick
        animationTick++;

        // Create spiral effect
        createSpiralEffect(activeWorld, activePlayer, animationTick);

        // Check if animation is complete
        if (animationTick >= EFFECT_DURATION) {
            animationActive = false;
            activePlayer = null;
            activeWorld = null;
        }
    }

    /**
     * Creates the flame spiral particle effect
     */
    private void createSpiralEffect(World world, PlayerEntity player, int tick) {
        // Number of flame particles per ring
        final int PARTICLES_PER_RING = 16;

        // Create multiple rings at different heights
        for (int ring = 0; ring < 3; ring++) {
            // Calculate ring height offset
            double heightOffset = ring * 0.4 - 0.4;

            // Calculate ring radius (slightly different for each ring)
            double ringRadius = EFFECT_RADIUS * (0.7 + ring * 0.15);

            // Calculate rotation offset for this ring
            double ringRotationOffset = ring * Math.PI / 3;

            // Create particles for this ring
            for (int i = 0; i < PARTICLES_PER_RING; i++) {
                // Calculate angle based on particle index and current tick
                double angle = ((double) i / PARTICLES_PER_RING * 2 * Math.PI) +
                        (tick * 0.15) + ringRotationOffset;

                // Calculate spiral radius that grows and shrinks over time
                double spiralRadiusFactor = 0.7 + 0.3 * Math.sin(tick * 0.1);
                double particleRadius = ringRadius * spiralRadiusFactor;

                // Calculate particle position
                double x = player.getX() + Math.cos(angle) * particleRadius;
                double y = player.getY() + 1.0 + heightOffset + Math.sin(tick * 0.2) * 0.1;
                double z = player.getZ() + Math.sin(angle) * particleRadius;

                // Calculate particle motion for the "flame dance" effect
                double motionX = Math.cos(angle) * 0.01;
                double motionY = 0.05 + world.getRandom().nextFloat() * 0.05;
                double motionZ = Math.sin(angle) * 0.01;

                // Add the flame particle
                world.addParticle(
                        ParticleTypes.FLAME,
                        x, y, z,
                        motionX, motionY, motionZ
                );

                // Occasionally add a smaller flame or smoke particle for effect
                if (world.getRandom().nextFloat() < 0.3) {
                    world.addParticle(
                            world.getRandom().nextBoolean() ? ParticleTypes.SMALL_FLAME : ParticleTypes.SMOKE,
                            x, y, z,
                            motionX * 0.5, motionY * 0.7, motionZ * 0.5
                    );
                }
            }
        }

        // Add extra particles at the floor level for a ground-burning effect
        if (tick % 3 == 0) {
            for (int i = 0; i < 5; i++) {
                double angle = world.getRandom().nextFloat() * 2 * Math.PI;
                double distance = world.getRandom().nextFloat() * EFFECT_RADIUS;

                double x = player.getX() + Math.cos(angle) * distance;
                double y = player.getY() + 0.1;
                double z = player.getZ() + Math.sin(angle) * distance;

                world.addParticle(
                        ParticleTypes.FLAME,
                        x, y, z,
                        0, 0.05, 0
                );
            }
        }
    }
}