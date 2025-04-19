package end3r.verdant_arcanum.spell.tier2;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.item.LivingStaffItem;
import end3r.verdant_arcanum.magic.ManaSystem;
import end3r.verdant_arcanum.spell.Spell;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.ClientPlayerEntity;

public class SolarBloomSpell implements Spell {
    private static final Logger LOGGER = LoggerFactory.getLogger("SolarBloomSpell");
    private static final int MANA_COST = 200;
    private static final int DURATION_TICKS = 150; // 5 seconds
    private static final double RANGE = 64.0;
    private static final double WIDTH = 2.5;
    private static final float DAMAGE_PER_TICK = 6f;
    private static final Map<UUID, Long> clientActiveSpells = new HashMap<>();

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
    public int getManaCost(ItemStack staffStack) {
        if (staffStack != null && staffStack.getItem() instanceof LivingStaffItem) {
            return MANA_COST; // For now, return the base mana cost
        }
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        // Get the staff the player is using
        ItemStack staffStack = player.getMainHandStack();
        if (!(staffStack.getItem() instanceof LivingStaffItem)) {
            staffStack = player.getOffHandStack();
        }

        // Get the actual mana cost with this staff
        int actualManaCost = getManaCost(staffStack);


        LOGGER.info("Player {} attempting to cast Solar Bloom", player.getUuid());

        // NOTE: We no longer need to check mana here - that should be done by the staff before calling this method
        // The mana deduction should happen in the staff's castSpell method AFTER this method returns successfully


        // Create a new spell instance and add it to the active spells map
        SpellInstance spellInstance = new SpellInstance(player, DURATION_TICKS);
        activeSpells.put(player.getUuid(), spellInstance);

        // Play sound effects
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SOLAR_BLOOM_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // If this is the server, create the initial beam effect
        if (!world.isClient) {
            spellInstance.createBeamEffect(world);
        }

        // Sync to clients
        if (world instanceof ServerWorld) {
            // Notify clients to show particle effects
            // This would typically use networking to send to all clients
            // For brevity, we're just handling the server-side logic here
        }

        // No return value needed as method is void
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Client-side effects like particles
        if (world.isClient) {
            Vec3d playerPos = player.getEyePos();
            Vec3d lookDir = player.getRotationVector();
            createBeamParticles(world, playerPos, lookDir);
        }
    }

    @Override
    public void playFailureEffects(World world, PlayerEntity player) {
        // Play failure sounds or visual effects
        if (world.isClient) {
            // Add failure particles or sounds here
            world.playSound(player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sound.SoundEvents.BLOCK_FIRE_EXTINGUISH,
                    SoundCategory.PLAYERS, 0.6f, 1.0f, false);
        }
    }

    public static void addClientSpell(UUID playerId, int durationTicks) {
        clientActiveSpells.put(playerId, System.currentTimeMillis() + (durationTicks * 50));
    }

    /**
     * Called every server tick to update all active Solar Bloom spells
     */
    public static void tickActiveSpells(World world, ClientPlayerEntity player) {
        if (world.isClient) {
            return; // Don't process on client
        }

        activeSpells.entrySet().removeIf(entry -> {
            SpellInstance spell = entry.getValue();
            if (spell.isExpired()) {
                return true;
            }
            spell.tick(world);
            return false;
        });
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
        }

        public boolean isExpired() {
            return remainingTicks <= 0;
        }

        public void tick(World world) {
            remainingTicks--;
            tickCounter++;

            // Every 5 ticks (0.25 seconds), cast the beam and damage entities
            if (tickCounter % 5 == 0) {
                createBeamEffect(world);
            }
        }

        public void createBeamEffect(World world) {
            if (caster == null || !caster.isAlive()) {
                return;
            }

            Vec3d start = caster.getEyePos();
            Vec3d direction = caster.getRotationVector();
            Vec3d end = start.add(direction.multiply(RANGE));

            // Create a collision box along the beam
            Box beamBox = createBeamCollisionBox(start, end, WIDTH);

            // Get all entities in the beam path
            List<LivingEntity> entities = world.getEntitiesByClass(
                    LivingEntity.class,
                    beamBox,
                    entity -> entity != caster && entity instanceof LivingEntity);

            // Damage each entity
            for (Entity entity : entities) {
                entity.damage(DamageSource.player((PlayerEntity) caster), DAMAGE_PER_TICK);

                // Set the entity on fire briefly
                entity.setFireTicks(40); // 2 seconds
            }

            // If server, send particles to clients
            if (world instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) world;

                // Create particles along the beam path
                double distance = start.distanceTo(end);
                for (double d = 0; d < distance; d += 0.5) {
                    double t = d / distance;
                    Vec3d pos = start.lerp(end, t);

                    serverWorld.spawnParticles(
                            ParticleTypes.FLAME,
                            pos.x, pos.y, pos.z,
                            1, 0.1, 0.1, 0.1, 0.01);
                }
            }
        }
    }

    private Box createBeamCollisionBox(Vec3d start, Vec3d end, double width) {
        double minX = Math.min(start.x, end.x) - width;
        double minY = Math.min(start.y, end.y) - width;
        double minZ = Math.min(start.z, end.z) - width;
        double maxX = Math.max(start.x, end.x) + width;
        double maxY = Math.max(start.y, end.y) + width;
        double maxZ = Math.max(start.z, end.z) + width;

        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static void createBeamParticles(World world, Vec3d startPos, Vec3d direction) {
        Vec3d endPos = startPos.add(direction.multiply(RANGE));

        double distance = startPos.distanceTo(endPos);
        for (double d = 0; d < distance; d += 0.5) {
            double t = d / distance;
            Vec3d pos = startPos.lerp(endPos, t);

            world.addParticle(
                    ParticleTypes.FLAME,
                    pos.x, pos.y, pos.z,
                    0, 0, 0);
        }
    }

    public void tickActiveSpellsClient(ClientWorld world, ClientPlayerEntity player) {
        long currentTime = System.currentTimeMillis();
        clientActiveSpells.entrySet().removeIf(entry -> {
            UUID playerId = entry.getKey();
            long expiryTime = entry.getValue();

            if (currentTime > expiryTime) {
                return true;
            }

            // Create particles for active spells
            if (playerId.equals(player.getUuid())) {
                Vec3d playerPos = player.getEyePos();
                Vec3d lookDir = player.getRotationVector();
                createBeamParticles(world, playerPos, lookDir);
            }

            return false;
        });
    }

    public static void registerSounds() {
        SOLAR_BLOOM_SOUND = new SoundEvent(SOLAR_BLOOM_SOUND_ID);
        Registry.register(Registry.SOUND_EVENT, SOLAR_BLOOM_SOUND_ID, SOLAR_BLOOM_SOUND);
    }
}