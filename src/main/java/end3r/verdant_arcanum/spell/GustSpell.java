package end3r.verdant_arcanum.spell;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Gust spell implementation that pushes entities away from the caster.
 */
public class GustSpell implements Spell {
    // Mana cost for casting this spell
    private static final int MANA_COST = 15;
    // Effect radius in blocks
    private static final double EFFECT_RADIUS = 4.0;
    // Push strength
    private static final double PUSH_STRENGTH = 1.5;

    @Override
    public String getType() {
        return "Gust";
    }

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        // Define area of effect - cone shape in front of player
        Vec3d playerPos = player.getPos();
        Vec3d lookDir = player.getRotationVector();

        // Find all entities in a box in front of the player
        Box areaOfEffect = new Box(
                player.getX() - EFFECT_RADIUS, player.getY() - 1, player.getZ() - EFFECT_RADIUS,
                player.getX() + EFFECT_RADIUS, player.getY() + 2, player.getZ() + EFFECT_RADIUS
        );

        List<Entity> entities = world.getEntitiesByClass(
                Entity.class,
                areaOfEffect,
                entity -> {
                    if (entity == player) return false;

                    // Check if entity is in the cone in front of player
                    Vec3d dirToEntity = entity.getPos().subtract(playerPos).normalize();
                    double dotProduct = lookDir.dotProduct(dirToEntity);

                    // Entity should be in front of player (within ~120 degree cone)
                    return dotProduct > 0.5;
                }
        );

        // Apply push effect to all affected entities
        for (Entity entity : entities) {
            // Calculate push direction (away from look direction)
            Vec3d pushDir = player.getRotationVector();

            // Calculate push strength based on distance (closer = stronger)
            double distance = entity.getPos().distanceTo(playerPos);
            double strength = PUSH_STRENGTH * (1.0 - distance / EFFECT_RADIUS);
            if (strength < 0.1) strength = 0.1;

            // Apply velocity
            entity.addVelocity(
                    pushDir.x * strength,
                    0.2 + pushDir.y * strength * 0.5, // Add some upward push
                    pushDir.z * strength
            );

            // Mark the entity as being affected by the push
            entity.velocityModified = true;
        }

        // Play the gust wind sound
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS,
                1.0F, 1.2F);
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        Vec3d lookVec = player.getRotationVector();

        // Create a cone of particles in front of the player
        for (int i = 0; i < 40; i++) {
            // Calculate a random point in the cone
            double distance = world.random.nextDouble() * EFFECT_RADIUS;
            double angle = world.random.nextDouble() * Math.PI * 0.5 - Math.PI * 0.25;

            // Calculate offset direction from look vector
            Vec3d right = new Vec3d(-lookVec.z, 0, lookVec.x).normalize();
            Vec3d offset = right.multiply(Math.sin(angle)).add(lookVec.multiply(Math.cos(angle)));

            // Add some randomness to the particle position
            world.addParticle(
                    ParticleTypes.CLOUD,
                    player.getX() + offset.x * distance,
                    player.getY() + player.getStandingEyeHeight() - 0.1 + offset.y * distance,
                    player.getZ() + offset.z * distance,
                    lookVec.x * 0.2,
                    lookVec.y * 0.1,
                    lookVec.z * 0.2
            );
        }
    }
}