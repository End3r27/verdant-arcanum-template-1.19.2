package end3r.verdant_arcanum.spell.tier2;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.spell.Spell;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.util.registry.Registry;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolarBloomSpell implements Spell {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolarBloomSpell.class);

    private static final int MANA_COST = 200;
    private static final int DURATION_TICKS = 150; // 7.5 seconds
    private static final double RANGE = 64.0;
    private static final double WIDTH = 2.5;
    private static final float DAMAGE_PER_TICK = 6f;

    private static final Map<UUID, SpellInstance> activeSpells = new HashMap<>();

    public static final Identifier SOLAR_BEAM_SOUND_ID = new Identifier(VerdantArcanum.MOD_ID, "solar_beam");
    public static SoundEvent SOLAR_BEAM_SOUND = new SoundEvent(SOLAR_BEAM_SOUND_ID);

    @Override
    public String getType() {
        return "Solar Bloom";
    }

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        if (!world.isClient) {
            Vec3d start = player.getEyePos();
            Vec3d direction = player.getRotationVec(1.0F);
            Vec3d end = start.add(direction.multiply(RANGE));

            SolarBeamEntity beamEntity = new SolarBeamEntity(world, start, end, WIDTH);
            boolean success = world.spawnEntity(beamEntity); // Returns false if spawning fails

            if (success) {
                LOGGER.info("SolarBeamEntity successfully spawned at {}, {}", start, end);
            } else {
                LOGGER.warn("Failed to spawn SolarBeamEntity at {}, {}", start, end);
            }

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SOLAR_BEAM_SOUND, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }



    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        world.playSound(player, player.getBlockPos(), SOLAR_BEAM_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void playFailureEffects(World world, PlayerEntity player) {
        player.sendMessage(Text.literal("Failed to cast Solar Bloom!").formatted(Formatting.RED), true);
        world.playSound(player, player.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5f, 1.0f);
    }

    public static void tickActiveSpells(World world, Object o) {
        Iterator<Map.Entry<UUID, SpellInstance>> iterator = activeSpells.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SpellInstance> entry = iterator.next();
            SpellInstance instance = entry.getValue();

            if (instance.isExpired()) {
                iterator.remove();
            } else {
                instance.tick(world);
            }
        }
    }

    public static class SpellInstance {
        private final PlayerEntity caster;
        private int remainingTicks;
        private SolarBeamEntity beamEntity;

        private Vec3d start;
        private Vec3d direction;
        private Vec3d end;

        public SpellInstance(PlayerEntity caster, int duration) {
            this.caster = caster;
            this.remainingTicks = duration;

            // Initialize beam properties
            this.start = caster.getEyePos();
            this.direction = caster.getRotationVec(1.0f);
            this.end = start.add(direction.multiply(64.0)); // 64 blocks range

            // Spawn the SolarBeamEntity with initial position
            this.beamEntity = new SolarBeamEntity(caster.world, start, end, 2.5);
            caster.world.spawnEntity(this.beamEntity);

        }


        public boolean isExpired() {
            return remainingTicks <= 0;
        }

        public void tick(World world) {
            if (!isExpired()) {
                remainingTicks--;

                // Update the beam end position
                Vec3d newStart = caster.getEyePos();
                Vec3d newDirection = caster.getRotationVec(1.0F);
                Vec3d newEnd = newStart.add(newDirection.multiply(SolarBloomSpell.RANGE));

                // Update the beam entity
                beamEntity.updateBeam(newStart, newEnd);
            } else {
                // Expire the beam after the duration is over
                if (beamEntity != null) {
                    beamEntity.discard();
                }
            }
        }

        private void updateBeamEntity(World world) {
            if (beamEntity == null) return;

            // Calculate the starting position (caster's eye position)
            Vec3d start = caster.getEyePos();

            // Calculate the end position (along the caster's line of sight)
            Vec3d direction = caster.getRotationVec(1.0F);
            Vec3d end = start.add(direction.multiply(RANGE));

            // Update the SolarBeamEntity
            beamEntity.updateBeam(start, end);
        }
    }

    public static void registerSounds() {
        Registry.register(Registry.SOUND_EVENT, SOLAR_BEAM_SOUND_ID, SOLAR_BEAM_SOUND);
    }
}
