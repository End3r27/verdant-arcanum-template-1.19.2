package end3r.verdant_arcanum.spell;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.magic.ManaSystem;
import end3r.verdant_arcanum.network.BeamParticlePacket;
import end3r.verdant_arcanum.network.NetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SolarBloomSpell implements Spell {
    private static final Logger LOGGER = LoggerFactory.getLogger("SolarBloomSpell");
    private static final int MANA_COST = 200;
    private static final int DURATION_TICKS = 150; // 5 seconds
    private static final double RANGE = 64.0;
    private static final double WIDTH = 2.5;
    private static final float DAMAGE_PER_TICK = 6f;
    private static final Map<UUID, Long> clientActiveSpells = new HashMap<>();
    public static void addClientSpell(UUID playerId, int durationTicks) {
        clientActiveSpells.put(playerId, System.currentTimeMillis() + (durationTicks * 50));
    }




    // Static map to track active spell instances
    private static final Map<UUID, SpellInstance> activeSpells = new HashMap<>();

    // Sound definition
    public static final Identifier SOLAR_BLOOM_SOUND_ID = new Identifier(VerdantArcanum.MOD_ID, "solar_bloom_cast");
    public static SoundEvent SOLAR_BLOOM_SOUND = new SoundEvent(SOLAR_BLOOM_SOUND_ID);

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
        UUID playerId = player.getUuid();

        ManaSystem.PlayerMana playerMana = ManaSystem.getInstance().getPlayerMana(player);
        float playerCurrentMana = playerMana.getCurrentMana();
        int playerMaxMana = playerMana.getMaxMana();

        LOGGER.info("Player {} has {} mana out of {} max and is trying to cast spell with cost {}",
                playerId, playerCurrentMana, playerMaxMana, MANA_COST);

        // Debug and force mana if needed
        if (playerCurrentMana <= 0 && !world.isClient) {
            LOGGER.info("Player has zero mana. Initializing to default max.");
            playerMana.setCurrentMana(playerMaxMana); // Set to max mana for debugging
            playerCurrentMana = playerMana.getCurrentMana(); // Update local variable

            // Force sync to client
            ManaSystem.getInstance().syncManaToClient(player);
        }

        LOGGER.info("Player {} attempting to cast Solar Bloom", playerId);


        // Check if player already has an active spell
        if (activeSpells.containsKey(playerId)) {
            LOGGER.info("Player already has an active Solar Bloom");
            return;
        }

        // Check mana
        if (ManaSystem.getInstance().useMana(player, MANA_COST)) {
            LOGGER.info("Spell cast successful - adding to active spells");

            // Add to active spells
            activeSpells.put(playerId, new SpellInstance(player, DURATION_TICKS));

            // Play sound
            world.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SOLAR_BLOOM_SOUND,
                    SoundCategory.PLAYERS,
                    100.0f,
                    1.0f
            );

            // Visual effect
            playClientEffects(world, player);
        } else {
            LOGGER.info("Spell cast failed - not enough mana");
            playFailureEffects(world, player);
        }
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Spiral particles around the player
        Vec3d lookDir = player.getRotationVec(1.0f);

        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 1.0;
            double offsetX = radius * Math.cos(angle);
            double offsetY = i * 0.1;
            double offsetZ = radius * Math.sin(angle);

            world.addParticle(
                    ParticleTypes.END_ROD,
                    player.getX() + offsetX,
                    player.getY() + offsetY,
                    player.getZ() + offsetZ,
                    lookDir.x * 0.1,
                    0.2,
                    lookDir.z * 0.1
            );
        }
    }

    @Override
    public void playFailureEffects(World world, PlayerEntity player) {
        world.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS,
                0.6f,
                1.2f
        );

        for (int i = 0; i < 10; i++) {
            world.addParticle(
                    ParticleTypes.SMOKE,
                    player.getX() + world.random.nextGaussian() * 0.2,
                    player.getY() + 1.5 + world.random.nextGaussian() * 0.2,
                    player.getZ() + world.random.nextGaussian() * 0.2,
                    0, 0.1, 0
            );
        }
    }

    /**
     * Called every server tick to update all active Solar Bloom spells
     */
    /**
     * Called every server tick to update all active Solar Bloom spells
     */
    public static void tickActiveSpells(World world, ClientPlayerEntity player) {
        // Skip if no active spells
        if (activeSpells.isEmpty()) {
            return;
        }

        // Create a copy of the keys to avoid concurrent modification
        List<UUID> casterIds = new ArrayList<>(activeSpells.keySet());

        for (UUID casterId : casterIds) {
            SpellInstance spellInstance = activeSpells.get(casterId);

            if (spellInstance == null) {
                // Remove invalid entries
                activeSpells.remove(casterId);
                continue;
            }

            if (spellInstance.isExpired()) {
                // Remove expired spells
                activeSpells.remove(casterId);
                LOGGER.debug("Solar Bloom spell for player {} has expired", casterId);
            } else {
                // Tick valid spells
                spellInstance.tick(world);
            }
        }

        // Process any client-side visual effects
        for (UUID clientSpellId : clientActiveSpells.keySet()) {
            // Use a different name than 'player' to avoid conflicts
            PlayerEntity spellCaster = getPlayerById(world, clientSpellId);

            if (spellCaster != null) {
                // Get current time for comparison
                long currentTime = System.currentTimeMillis();
                long endTime = clientActiveSpells.get(clientSpellId);

                // If spell effect should still be active
                if (currentTime <= endTime) {
                    // Create visual effects for this client-side spell
                    Vec3d eyePos = spellCaster.getEyePos();
                    Vec3d lookDir = spellCaster.getRotationVec(1.0f);

                    // Call the static method to create particles
                    createBeamParticles(world, eyePos, lookDir);
                } else {
                    // Mark for removal
                    clientActiveSpells.remove(clientSpellId);
                }
            }
        }

        // Log any debug information if needed
        if (!activeSpells.isEmpty() && world.getTime() % 100 == 0) {
            LOGGER.debug("Currently active Solar Bloom spells: {}", activeSpells.size());
        }
    }

    private static PlayerEntity getPlayerById(World world, UUID playerId) {
        for (PlayerEntity player : world.getPlayers()) {
            if (player.getUuid().equals(playerId)) {
                return player;
            }
        }
        return null;
    }




    /**
     * Class to track an individual instance of the spell being cast
     */
    public class SpellInstance {
        private final PlayerEntity caster;
        private int remainingTicks;
        private int tickCounter = 0;

        public SpellInstance(PlayerEntity caster, int duration) {
            this.caster = caster;
            this.remainingTicks = duration;
            LOGGER.debug("Created new spell instance for player {} with duration {}",
                    caster.getUuid(), duration);
        }

        public boolean isExpired() {
            return remainingTicks <= 0;
        }

        public void tick(World world) {
            if (isExpired()) return;

            remainingTicks--;
            tickCounter++;

            if (tickCounter % 20 == 0) { // Log once per second
                LOGGER.debug("Spell instance for player {} has {} ticks remaining",
                        caster.getUuid(), remainingTicks);
            }

            // Create the beam effect
            createBeamEffect(world);

            // Add sound effects every 20 ticks
            if (tickCounter % 20 == 0) {
                world.playSound(
                        null,
                        caster.getX(),
                        caster.getY(),
                        caster.getZ(),
                        SoundEvents.BLOCK_FIRE_AMBIENT,
                        SoundCategory.PLAYERS,
                        0.3f,
                        1.0f + world.random.nextFloat() * 0.2f
                );
            }
        }

        public void createBeamEffect(World world) {
            // Calculate position for ray based on player's look direction
            Vec3d eyePos = caster.getEyePos();
            Vec3d lookDir = caster.getRotationVec(1.0f);
            Vec3d targetPos = eyePos.add(lookDir.multiply(RANGE));

            // Create a box along the ray to detect entities
            Box detectionBox = createBeamCollisionBox(eyePos, targetPos, WIDTH);

            // Get all living entities in range except the caster
            List<LivingEntity> targets = world.getEntitiesByClass(
                    LivingEntity.class,
                    detectionBox,
                    entity -> entity != caster && entity.isAlive()
            );

            // Apply damage to entities
            for (LivingEntity target : targets) {
                target.damage(DamageSource.player(caster), DAMAGE_PER_TICK);

                if (remainingTicks % 20 == 0) { // Apply fire every second
                    target.setOnFireFor(1);
                }
            }

            // Spawn particles for the beam (this should only happen on the client)
            // Using a packet system or client event
            if (world.isClient) {
                createBeamParticles(world, eyePos, lookDir);
            } else if (caster instanceof ServerPlayerEntity) {
                // Send particle packet from server to client
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity)caster;

                // Create a packet with beam particle data
                BeamParticlePacket packet = new BeamParticlePacket(eyePos, lookDir);

                // Send the packet to the specific player
                NetworkHandler.sendToClient(packet, serverPlayer);
            }

            }
        }

        private Box createBeamCollisionBox(Vec3d start, Vec3d end, double width) {
            return new Box(
                    Math.min(start.x, end.x) - width,
                    Math.min(start.y, end.y) - width,
                    Math.min(start.z, end.z) - width,
                    Math.max(start.x, end.x) + width,
                    Math.max(start.y, end.y) + width,
                    Math.max(start.z, end.z) + width
            );
        }

    public static void createBeamParticles(World world, Vec3d startPos, Vec3d direction) {
        // This should only run on the client side
        if (!world.isClient) return;

        // Dense beam of particles
        for (int i = 1; i <= 30; i++) {
            double distance = i * (RANGE / 30.0);
            Vec3d pos = startPos.add(direction.multiply(distance));

            // Main beam particles - larger size
            ((ClientWorld)world).addParticle(
                    ParticleTypes.END_ROD,
                    true, // Force parameter - show particles regardless of distance/settings
                    pos.x, pos.y, pos.z,
                    0, 0, 0
            );

            // Add some flame particles for effect - larger size
            if (i % 3 == 0) {
                ((ClientWorld)world).addParticle(
                        ParticleTypes.FLAME,
                        true,
                        pos.x, pos.y, pos.z,
                        0, 0.02, 0
                );
            }

            // Add some randomness to create beam width - larger size
            if (i % 2 == 0) {
                double offsetX = world.random.nextGaussian() * 0.1;
                double offsetY = world.random.nextGaussian() * 0.1;
                double offsetZ = world.random.nextGaussian() * 0.1;

                ((ClientWorld)world).addParticle(
                        ParticleTypes.END_ROD,
                        true,
                        pos.x + offsetX, pos.y + offsetY, pos.z + offsetZ,
                        0, 0, 0
                );
            }
        }

        // Add a bloom effect at the end - larger size
        Vec3d endPos = startPos.add(direction.multiply(RANGE));
        for (int i = 0; i < 10; i++) {
            double offsetX = world.random.nextGaussian() * 0.4; // Increased spread
            double offsetY = world.random.nextGaussian() * 0.4;
            double offsetZ = world.random.nextGaussian() * 0.4;

            ((ClientWorld)world).addParticle(
                    ParticleTypes.FLAME,
                    true,
                    endPos.x + offsetX, endPos.y + offsetY, endPos.z + offsetZ,
                    offsetX * 0.05, offsetY * 0.05, offsetZ * 0.05
            );
        }
    }


        public void tickActiveSpellsClient(ClientWorld world, ClientPlayerEntity player) {
            long currentTime = System.currentTimeMillis();
            // Remove expired spells
            clientActiveSpells.entrySet().removeIf(entry -> {
                if (entry.getValue() < currentTime) {
                    return true;
                }

                // Get the player
                PlayerEntity caster = world.getPlayerByUuid(entry.getKey());
                if (caster != null) {
                    // Create the beam particle effect
                    Vec3d eyePos = caster.getEyePos();
                    Vec3d lookDir = caster.getRotationVec(1.0f);
                    createBeamParticles(world, eyePos, lookDir);
                }

                return false;
            });

    }
    public static void registerSounds() {
        // Register the Solar Bloom cast sound and assign it to our static variable
        SOLAR_BLOOM_SOUND = Registry.register(
                Registry.SOUND_EVENT,
                SOLAR_BLOOM_SOUND_ID,
                new SoundEvent(SOLAR_BLOOM_SOUND_ID)
        );
    }


}
