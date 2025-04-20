package end3r.verdant_arcanum.spell.tier2;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.registry.ModEntities;
import end3r.verdant_arcanum.spell.Spell;
import net.minecraft.entity.Entity;
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
    public void playClientEffects(World world, PlayerEntity player) {
        world.playSound(player, player.getBlockPos(), SOLAR_BEAM_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    @Override
    public void playFailureEffects(World world, PlayerEntity player) {
        player.sendMessage(Text.literal("Failed to cast Solar Bloom!").formatted(Formatting.RED), true);
        world.playSound(player, player.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5f, 1.0f);
    }

    public static void tickActiveSpells(World world, Object o) {
        // Don't process if no active spells
        if (activeSpells.isEmpty()) {
            return;
        }

        // Use iterator to safely remove while iterating
        Iterator<Map.Entry<UUID, SpellInstance>> iterator = activeSpells.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, SpellInstance> entry = iterator.next();
            SpellInstance instance = entry.getValue();

            // Tick the spell instance
            instance.tick(world);

            // Remove expired spells
            if (instance.isExpired()) {
                // Make sure the beam entity is removed
                if (instance.beamEntity != null && instance.beamEntity.isAlive()) {
                    instance.beamEntity.kill();
                    LOGGER.info("Final cleanup of SolarBeamEntity on spell expiration");
                }

                // Remove spell from active list
                iterator.remove();
                LOGGER.info("Removed expired SolarBloomSpell for player: {}", entry.getKey());
            }
        }
    }

    public class SpellInstance {
        private final PlayerEntity caster;
        private final UUID casterUUID; // Add this field
        private int remainingTicks;
        private SolarBeamEntity beamEntity;

        public Vec3d start;
        public Vec3d direction;
        public Vec3d end;

        public SpellInstance(PlayerEntity caster, int duration) {
            this.caster = caster;
            this.casterUUID = caster.getUuid();
            this.remainingTicks = duration;

            // Set exact middle of viewpoint for start position
            Vec3d povDirection = caster.getRotationVector().normalize();
            Vec3d eyePosition = caster.getEyePos();

            // Update start to realize it from the center direction
            this.start = eyePosition.add(povDirection.multiply(0.1)); // Slight offset to avoid self-collision

            // Use precise direction vector
            this.direction = povDirection;

            // Correctly calculate end based on specified range
            this.end = start.add(direction.multiply(RANGE));

            this.beamEntity = null;
        }


        public boolean isExpired() {
            return remainingTicks <= 0;
        }

        public void tick(World world) {
            // Decrease remaining time
            remainingTicks--;


            // Update start position to match player's current position
            start = new Vec3d(caster.getX(), caster.getY() + caster.getStandingEyeHeight(), caster.getZ());
            direction = caster.getRotationVector().normalize();
            end = start.add(direction.multiply(RANGE));

            // Always update the beam entity with current positions
            updateBeamEntity(world);



            if (!isExpired()) {
                // On server side, always ensure beam entity exists and is correctly positioned
                if (!world.isClient) {
                    updateBeamEntity(world);
                }

                // Process damage and effects
                // Your existing code...
            } else {
                // When expired, remove the beam entity
                if (beamEntity != null) {
                    beamEntity.remove(Entity.RemovalReason.DISCARDED);
                    beamEntity = null;
                }
            }
        }

        private void updateBeamEntity(World world) {
            // Get exact current eye position and direction
            Vec3d povDirection = caster.getRotationVector().normalize();
            Vec3d eyePosition = caster.getEyePos();

            // Update start position accurately
            this.start = eyePosition.add(povDirection.multiply(0.1)); // Small offset to visualize from viewpoint

            // Update direction and end based on player's POV
            this.direction = povDirection;
            this.end = start.add(direction.multiply(RANGE));

            if (beamEntity == null || beamEntity.isRemoved()) {
                // Create new beam with exact position from player's POV
                beamEntity = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, world);
                beamEntity.setPosition(start.x, start.y, start.z);
                beamEntity.setCaster(caster);
                world.spawnEntity(beamEntity);

                // Debug logging
                LOGGER.info("Beam created at: {} with end: {}", start, end);
            } else {
                // Update existing beam with precise positions
                beamEntity.updateBeam(start, end);
            }
        }

    }

    @Override
    public void cast(World world, PlayerEntity player) {
        // Create or update spell instance
        UUID playerId = player.getUuid();

        // Cancel existing spell if active
        if (activeSpells.containsKey(playerId)) {
            SpellInstance oldSpell = activeSpells.remove(playerId);
            if (oldSpell.beamEntity != null && !oldSpell.beamEntity.isRemoved()) {
                oldSpell.beamEntity.remove(Entity.RemovalReason.DISCARDED);
            }
            player.sendMessage(Text.translatable("spell.verdant_arcanum.solar_bloom.canceled").formatted(Formatting.GOLD), true);
        }

        // Get player position and look
        Vec3d povDirection = player.getRotationVector().normalize();
        Vec3d eyePosition = player.getEyePos();
        Vec3d startPos = eyePosition.add(povDirection.multiply(0.1)); // Slight offset for POV alignment
        Vec3d endPos = startPos.add(povDirection.multiply(RANGE));

        // Create new spell instance
        SpellInstance spellInstance = new SpellInstance(player, DURATION_TICKS);
        // The constructor now handles position initialization correctly

        // Store for client effects
        START_POS = eyePosition;

        if (!world.isClient) {
            // Create beam entity on server
            SolarBeamEntity beamEntity = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, world);

            // CRITICAL: Set position BEFORE doing anything else
            beamEntity.setPosition(startPos.x, startPos.y, startPos.z);

            // Now set caster and update beam
            beamEntity.setCaster(player);
            beamEntity.updateBeam(startPos, endPos);

            // Verify position is correct before spawning
            if (beamEntity.getX() == 0 && beamEntity.getY() == 0 && beamEntity.getZ() == 0) {
                LOGGER.error("BEAM POSITION RESET TO ORIGIN BEFORE SPAWN! Fixing...");
                beamEntity.setPosition(startPos.x, startPos.y, startPos.z);
            }

            // Spawn the entity
            world.spawnEntity(beamEntity);

            // Store reference
            spellInstance.beamEntity = beamEntity;

            // Sound and other effects
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SOLAR_BEAM_SOUND, SoundCategory.PLAYERS, 1.0F, 1.0F);

            // Store active spell
            activeSpells.put(playerId, spellInstance);
        }
    }
    public static void registerSounds() {
        Registry.register(Registry.SOUND_EVENT, SOLAR_BEAM_SOUND_ID, SOLAR_BEAM_SOUND);
    }
}
