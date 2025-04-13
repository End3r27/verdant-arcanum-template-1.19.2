package end3r.verdant_arcanum.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
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

            // Spawn vine particles at the entity's feet
            for (int i = 0; i < 10; i++) {
                world.addParticle(
                        ParticleTypes.HAPPY_VILLAGER, // Green particle for the "vine" effect
                        livingEntity.getX() + (world.random.nextDouble() - 0.5),
                        livingEntity.getY() + 0.1,
                        livingEntity.getZ() + (world.random.nextDouble() - 0.5),
                        0, 0.05, 0
                );
            }
        }

        // Play a "root/growth" sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_GRASS_PLACE, SoundCategory.PLAYERS,
                1.0F, 0.8F);
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Create a circle of particles around the player to show area of effect
        for (int i = 0; i < 50; i++) {
            double angle = 2 * Math.PI * i / 50;
            double x = Math.cos(angle) * EFFECT_RADIUS;
            double z = Math.sin(angle) * EFFECT_RADIUS;

            world.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.getX() + x,
                    player.getY() + 0.1,
                    player.getZ() + z,
                    0, 0.05, 0
            );
        }

        // Add some rising particles in the area
        for (int i = 0; i < 30; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * EFFECT_RADIUS * 2;
            double offsetZ = (world.random.nextDouble() - 0.5) * EFFECT_RADIUS * 2;

            world.addParticle(
                    ParticleTypes.HAPPY_VILLAGER,
                    player.getX() + offsetX,
                    player.getY() + 0.1,
                    player.getZ() + offsetZ,
                    0, 0.1, 0
            );
        }
    }
}