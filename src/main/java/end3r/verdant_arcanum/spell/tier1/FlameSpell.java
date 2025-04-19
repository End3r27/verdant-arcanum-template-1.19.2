package end3r.verdant_arcanum.spell.tier1;

import end3r.verdant_arcanum.spell.Spell;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Flame spell implementation that shoots a fireball.
 */
public class FlameSpell implements Spell {
    // Mana cost for casting this spell
    private static final int MANA_COST = 25;

    @Override
    public String getType() {
        return "Flame";
    }

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        // Calculate fireball direction from where the player is looking
        Vec3d lookVec = player.getRotationVector();

        // Create a small fireball entity
        SmallFireballEntity fireball = new SmallFireballEntity(
                world,
                player.getX() + lookVec.x * 0.5,
                player.getY() + player.getStandingEyeHeight() - 0.1,
                player.getZ() + lookVec.z * 0.5,
                lookVec.x * 0.5,
                lookVec.y * 0.5,
                lookVec.z * 0.5
        );

        // Set the fireball owner to the player
        fireball.setOwner(player);

        // Spawn the fireball in the world
        world.spawnEntity(fireball);

        // Play sound effect
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS,
                0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Spawn flame particles around the player
        Vec3d lookVec = player.getRotationVector();
        double x = player.getX() + lookVec.x * 0.5;
        double y = player.getY() + player.getStandingEyeHeight() - 0.1;
        double z = player.getZ() + lookVec.z * 0.5;

        for (int i = 0; i < 10; i++) {
            world.addParticle(
                    ParticleTypes.FLAME,
                    x + world.random.nextGaussian() * 0.1,
                    y + world.random.nextGaussian() * 0.1,
                    z + world.random.nextGaussian() * 0.1,
                    lookVec.x * 0.2 + world.random.nextGaussian() * 0.02,
                    lookVec.y * 0.2 + world.random.nextGaussian() * 0.02,
                    lookVec.z * 0.2 + world.random.nextGaussian() * 0.02
            );
        }
    }
}