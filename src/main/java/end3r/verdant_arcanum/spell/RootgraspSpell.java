package end3r.verdant_arcanum.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Rootgrasp spell implementation that snares nearby entities.
 */
public class RootgraspSpell implements Spell {
    // Mana cost for casting this spell
    private static final int MANA_COST = 35;
    // Effect radius in blocks
    private static final double EFFECT_RADIUS = 5.0;
    // Duration of the slow effect in ticks (5 seconds)
    private static final int EFFECT_DURATION = 100;
    // Slow effect amplifier (0 = level I, 1 = level II, etc.)
    private static final int SLOW_AMPLIFIER = 3;

    // Inner, middle, and outer circle radius for the visualization
    private static final double INNER_CIRCLE_RADIUS = EFFECT_RADIUS * 0.3;
    private static final double MIDDLE_CIRCLE_RADIUS = EFFECT_RADIUS * 0.65;
    private static final double OUTER_CIRCLE_RADIUS = EFFECT_RADIUS;

    // Particle size factors (vanilla particles don't support direct size control,
    // but we can control velocity which affects perceived size)
    private static final double PARTICLE_SIZE_FACTOR = 0.02;

    // Particle life (controlled by vertical velocity)
    private static final double PARTICLE_RISING_SPEED = 0.08;

    @Override
    public String getType() {
        return "Rootgrasp";
    }

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        // Define area of effect
        Box areaOfEffect = new Box(
                player.getX() - EFFECT_RADIUS, player.getY() - 1, player.getZ() - EFFECT_RADIUS,
                player.getX() + EFFECT_RADIUS, player.getY() + 2, player.getZ() + EFFECT_RADIUS
        );

        // Find all living entities in the area (excluding the caster)
        List<LivingEntity> entities = world.getEntitiesByClass(
                LivingEntity.class,
                areaOfEffect,
                entity -> entity != player
        );

        // Apply slow effect to all affected entities
        for (LivingEntity livingEntity : entities) {
            // Apply a strong slowness effect
            livingEntity.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            EFFECT_DURATION,
                            SLOW_AMPLIFIER
                    )
            );

            // Also apply a brief mining fatigue for "root" feeling
            livingEntity.addStatusEffect(
                    new StatusEffectInstance(
                            StatusEffects.MINING_FATIGUE,
                            EFFECT_DURATION / 2,
                            1
                    )
            );

            // Create more substantial root particles around affected entities
            spawnEntityRootEffects(world, livingEntity);
        }

        // Play a "root/growth" sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.PLAYERS,
                1.0F, 0.8F);

        // Add a second deeper sound for more impact
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_ROOTS_PLACE, SoundCategory.PLAYERS,
                0.8F, 0.6F);
    }

    /**
     * Creates root particles around an affected entity
     */
    private void spawnEntityRootEffects(World world, LivingEntity entity) {
        // Spawn more substantial vine particles at the entity's feet and legs
        for (int i = 0; i < 16; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.8;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.8;
            double height = world.random.nextDouble() * 1.0; // From feet up to 1 block

            world.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    entity.getX() + offsetX,
                    entity.getY() + height,
                    entity.getZ() + offsetZ,
                    0, PARTICLE_RISING_SPEED * 1.5, 0 // Faster rising for longer life
            );

            // Add some complementary particles for effect
            if (i % 3 == 0) {
                world.addParticle(
                        ParticleTypes.COMPOSTER,
                        entity.getX() + offsetX,
                        entity.getY() + height * 0.5,
                        entity.getZ() + offsetZ,
                        0, PARTICLE_RISING_SPEED, 0
                );
            }
        }
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Schedule the three circles to appear in sequence
        spawnCircleEffect(world, player, INNER_CIRCLE_RADIUS, 0);

        // We'd typically use a scheduler here, but for simplicity
        // we'll just spawn all three at once with different parameters

        // First circle - inner (smallest, closest to player)
        spawnCircleEffect(world, player, INNER_CIRCLE_RADIUS, 30,
                ParticleTypes.HAPPY_VILLAGER, PARTICLE_RISING_SPEED * 1.2);

        // Second circle - middle
        spawnCircleEffect(world, player, MIDDLE_CIRCLE_RADIUS, 45,
                ParticleTypes.HAPPY_VILLAGER, PARTICLE_RISING_SPEED);

        // Third circle - outer (largest, farthest from player)
        spawnCircleEffect(world, player, OUTER_CIRCLE_RADIUS, 60,
                ParticleTypes.HAPPY_VILLAGER, PARTICLE_RISING_SPEED * 0.8);

        // Add rising particles throughout the effect area for additional visual impact
        spawnRisingAreaEffects(world, player);
    }

    /**
     * Creates a circle of particles at the specified radius
     */
    private void spawnCircleEffect(World world, PlayerEntity player, double radius,
                                   int particleCount) {
        spawnCircleEffect(world, player, radius, particleCount,
                ParticleTypes.HAPPY_VILLAGER, PARTICLE_RISING_SPEED);
    }

    /**
     * Creates a circle of particles with specified parameters
     */
    private void spawnCircleEffect(World world, PlayerEntity player, double radius,
                                   int particleCount, ParticleEffect particleType,
                                   double risingSpeed) {
        for (int i = 0; i < particleCount; i++) {
            double angle = 2 * Math.PI * i / particleCount;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            // Add small random variation to make it look more natural
            double xVar = (world.random.nextDouble() - 0.5) * 0.2 * radius;
            double zVar = (world.random.nextDouble() - 0.5) * 0.2 * radius;

            world.addParticle(
                    particleType,
                    player.getX() + x + xVar,
                    player.getY() + 0.1,
                    player.getZ() + z + zVar,
                    0, risingSpeed, 0 // Vertical velocity for longer particle life
            );

            // For larger circles, add some particles at varying heights
            if (radius > INNER_CIRCLE_RADIUS && i % 3 == 0) {
                double height = world.random.nextDouble() * 0.4;
                world.addParticle(
                        ParticleTypes.COMPOSTER, // Alternative green particle
                        player.getX() + x + xVar,
                        player.getY() + height,
                        player.getZ() + z + zVar,
                        0, risingSpeed * 0.8, 0
                );
            }
        }
    }

    /**
     * Creates rising particles throughout the effect area
     */
    private void spawnRisingAreaEffects(World world, PlayerEntity player) {
        // Add some rising particles in the area
        for (int i = 0; i < 40; i++) {
            double distance = world.random.nextDouble() * EFFECT_RADIUS;
            double angle = world.random.nextDouble() * Math.PI * 2;

            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;

            // Add variation in the height
            double height = world.random.nextDouble() * 0.5;

            // Main green particles
            world.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.getX() + offsetX,
                    player.getY() + height,
                    player.getZ() + offsetZ,
                    (world.random.nextDouble() - 0.5) * PARTICLE_SIZE_FACTOR,  // Small horizontal drift
                    PARTICLE_RISING_SPEED + (world.random.nextDouble() * 0.05), // Variable rising speed
                    (world.random.nextDouble() - 0.5) * PARTICLE_SIZE_FACTOR   // Small horizontal drift
            );

            // Add some complementary particles occasionally
            if (i % 5 == 0) {
                world.addParticle(
                        ParticleTypes.COMPOSTER,
                        player.getX() + offsetX,
                        player.getY() + height * 0.8,
                        player.getZ() + offsetZ,
                        0, PARTICLE_RISING_SPEED * 0.7, 0
                );
            }
        }
    }
}