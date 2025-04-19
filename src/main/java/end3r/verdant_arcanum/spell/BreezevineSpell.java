package end3r.verdant_arcanum.spell;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Breezevine spell implementation that pushes the caster backward and provides brief fall damage immunity.
 */
public class BreezevineSpell implements Spell {
    // Mana cost for casting this spell
    private static final int MANA_COST = 20;
    // Strength of the backward push
    private static final double PUSH_STRENGTH = 1.5;
    // Duration of slow falling effect in ticks (5 seconds)
    private static final int SLOW_FALLING_DURATION = 100;

    @Override
    public String getType() {
        return "Breezevine";
    }

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        // Calculate backward direction (opposite of where player is looking)
        Vec3d lookVec = player.getRotationVector();
        Vec3d backwardVec = new Vec3d(-lookVec.x, 0.2, -lookVec.z).normalize().multiply(PUSH_STRENGTH);

        // Apply velocity to push player backward
        player.addVelocity(backwardVec.x, backwardVec.y, backwardVec.z);

        // Need to send velocity updates to client
        player.velocityModified = true;

        // Give slow falling effect if player is in the air
        if (!player.isOnGround()) {
            player.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING,
                    SLOW_FALLING_DURATION,
                    0, // Amplifier level 0 (default strength)
                    false, // Is ambient
                    true,  // Show particles
                    true   // Show icon
            ));
        }

        // Play sound effect
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS,
                0.5F, 1.8F);
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Spawn swirling air particles around the player
        Vec3d lookVec = player.getRotationVector();
        double x = player.getX();
        double y = player.getY() + player.getStandingEyeHeight() - 0.1;
        double z = player.getZ();

        // Create a spiral of particles behind the player
        for (int i = 0; i < 20; i++) {
            double angle = i * Math.PI / 10;
            double offsetX = Math.cos(angle) * 0.5;
            double offsetZ = Math.sin(angle) * 0.5;

            world.addParticle(
                    ParticleTypes.CLOUD,
                    x - lookVec.x + offsetX,
                    y + (i * 0.05),
                    z - lookVec.z + offsetZ,
                    -lookVec.x * 0.2,
                    0.05,
                    -lookVec.z * 0.2
            );
        }
    }
}