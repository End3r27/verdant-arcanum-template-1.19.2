package end3r.verdant_arcanum.spell.tier2;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.registry.ModEntities;
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
    public static Vec3d START_POS = Vec3d.ZERO;



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
        // Create or update spell instance for this player
        UUID playerId = player.getUuid();

        // Check if player already has an active spell
        if (activeSpells.containsKey(playerId)) {
            // Cancel existing spell
            activeSpells.remove(playerId);
            player.sendMessage(Text.translatable("spell.verdant_arcanum.solar_bloom.canceled").formatted(Formatting.GOLD), true);
        }

        // Get player eye position and look vector
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVector();

        // Calculate beam end position
        Vec3d endPos = eyePos.add(lookVec.multiply(RANGE));

        // Create new spell instance
        SpellInstance spellInstance = new SpellInstance(player, DURATION_TICKS);
        spellInstance.start = eyePos;
        spellInstance.direction = lookVec;
        spellInstance.end = endPos;

        // Store the start position for client effects
        START_POS = eyePos;

        // Create SolarBeamEntity on the server
        if (!world.isClient) {
            // Create the beam entity
            SolarBeamEntity beamEntity = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, world);

            // Set position and beam properties
            beamEntity.setPos(eyePos.x, eyePos.y, eyePos.z);
            beamEntity.updateBeam(eyePos, endPos);

            // Spawn the entity in the world
            world.spawnEntity(beamEntity);

            // Store the entity reference in spell instance
            spellInstance.beamEntity = beamEntity;

            // Log to confirm entity creation
            LOGGER.info("Server created SolarBeamEntity at pos={}, start={}, end={}",
                    eyePos, eyePos, endPos);

            // Play sound effect
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SOLAR_BEAM_SOUND, SoundCategory.PLAYERS, 1.0F, 1.0F);

            // Add the active spell
            activeSpells.put(playerId, spellInstance);
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

        public Vec3d start;
        public Vec3d direction;
        public Vec3d end;

        public SpellInstance(PlayerEntity caster, int duration) {
            this.caster = caster;
            this.remainingTicks = duration;
            SolarBloomSpell.START_POS = this.start;

            // Initialize start, direction, and end
            this.start = caster.getEyePos(); // The starting position
            this.direction = caster.getRotationVec(1.0F).normalize(); // Caster's facing direction
            this.end = this.start.add(this.direction.multiply(SolarBloomSpell.RANGE)); // Calculate beam's endpoint
        }


        public boolean isExpired() {
            return remainingTicks <= 0;
        }

        public void tick(World world) {
            if (!isExpired()) {
                this.remainingTicks--;

                // Calculate/update the beam positions based on caster
                this.start = caster.getEyePos();
                this.direction = caster.getRotationVec(1.0F).normalize();
                this.end = this.start.add(this.direction.multiply(SolarBloomSpell.RANGE));

                // Update the static field if you're using it (not recommended long-term)
                SolarBloomSpell.START_POS = this.start;

                // Create or update the beam entity
                updateBeamEntity(world);

                // Log current positions for debugging
                LOGGER.info("SpellInstance tick - start: {}, end: {}", this.start, this.end);

            } else {
                // Expire the beam after the duration is over
                if (beamEntity != null) {
                    beamEntity.discard();
                }
            }
        }

        private void updateBeamEntity(World world) {
            // Update beam entity position and properties
            if (beamEntity != null && beamEntity.isAlive()) {
                // Calculate new beam end position based on caster's current look
                Vec3d eyePos = caster.getEyePos();
                Vec3d lookVec = caster.getRotationVector();
                Vec3d endPos = eyePos.add(lookVec.multiply(RANGE));

                // Update beam entity
                beamEntity.setPos(eyePos.x, eyePos.y, eyePos.z);
                beamEntity.updateBeam(eyePos, endPos);

                // Update stored values
                start = eyePos;
                direction = lookVec;
                end = endPos;
            } else if (!world.isClient) {
                // Create a new beam entity if the old one is gone
                SolarBeamEntity entity = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, world);

                // Calculate beam properties
                Vec3d eyePos = caster.getEyePos();
                Vec3d lookVec = caster.getRotationVector();
                Vec3d endPos = eyePos.add(lookVec.multiply(RANGE));

                // Set entity properties
                entity.setPos(eyePos.x, eyePos.y, eyePos.z);
                entity.updateBeam(eyePos, endPos);

                // Spawn the entity in the world
                world.spawnEntity(entity);

                // Update reference
                beamEntity = entity;

                // Update stored values
                start = eyePos;
                direction = lookVec;
                end = endPos;

                LOGGER.info("Re-created SolarBeamEntity for continuing spell");
            }
        }
    }

    public static void registerSounds() {
        Registry.register(Registry.SOUND_EVENT, SOLAR_BEAM_SOUND_ID, SOLAR_BEAM_SOUND);
    }
}
