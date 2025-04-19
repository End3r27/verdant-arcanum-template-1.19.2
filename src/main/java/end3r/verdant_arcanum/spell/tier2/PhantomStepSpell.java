package end3r.verdant_arcanum.spell.tier2;

import end3r.verdant_arcanum.spell.Spell;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PhantomStepSpell implements Spell {
    private static final int MANA_COST = 75;
    private static final int DURATION_TICKS = 200;
    private static final int COOLDOWN_TICKS = 20 * 20; // 20s cooldown

    @Override
    public String getType() {
        return "Phantom Step";
    }

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        if (world.isClient) return;

        // Apply brief invisibility and resistance to simulate phasing
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, DURATION_TICKS, 4, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, DURATION_TICKS, 0, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, DURATION_TICKS, 0, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, DURATION_TICKS, 0, false, false));

        // Play sound effect
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS,
                0.6f, 1.2f);

        // Set cooldown
        player.getItemCooldownManager().set(player.getMainHandStack().getItem(), COOLDOWN_TICKS);
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        if (world.isClient) {
            Vec3d pos = player.getPos();
            for (int i = 0; i < 20; i++) {
                double x = pos.x + (world.getRandom().nextDouble() - 0.5) * 1.5;
                double y = pos.y + world.getRandom().nextDouble() * 1.5;
                double z = pos.z + (world.getRandom().nextDouble() - 0.5) * 1.5;
                world.addParticle(ParticleTypes.WITCH, x, y, z, 0, 0.01, 0);
            }
        }
    }
}
