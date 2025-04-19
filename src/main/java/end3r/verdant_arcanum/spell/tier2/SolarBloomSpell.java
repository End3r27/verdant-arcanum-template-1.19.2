package end3r.verdant_arcanum.spell.tier2;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
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

    // Beam entity registry entry
    public static EntityType<SolarBeamEntity> SOLAR_BEAM_ENTITY;

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
        // Implementation as before
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        LOGGER.info("Player {} casting Solar Bloom", player.getUuid());

        // Create a new spell instance and add it to the active spells map
        SpellInstance spellInstance = new SpellInstance(player, DURATION_TICKS);
        activeSpells.put(player.getUuid(), spellInstance);

        // Play sound effects
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SOLAR_BLOOM_SOUND, SoundCategory.PLAYERS, 1.0f, 1.0f);

        // If this is the server, create the beam entity
        if (!world.isClient) {
            spellInstance.createBeamEffect(world);
        }
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Client-side effects are now handled by the beam entity renderer
    }

    @Override
    public void playFailureEffects(World world, PlayerEntity player) {
        // Play failure sounds or visual effects
        if (world.isClient) {
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
        private SolarBeamEntity beamEntity;

        public SpellInstance(PlayerEntity caster, int duration) {
            this.caster = caster;
            this.remainingTicks = duration;
            this.beamEntity = null;
        }

        public boolean isExpired() {
            return remainingTicks <= 0;
        }

        public void tick(World world) {
            remainingTicks--;
            tickCounter++;

            // Every tick, update the beam entity
            if (beamEntity != null && beamEntity.isAlive()) {
                updateBeamEntity(world);
            } else {
                // If the beam entity is gone for some reason, create a new one
                createBeamEffect(world);
            }

            // Every 5 ticks (0.25 seconds), damage entities in the beam path
            if (tickCounter % 5 == 0) {
                applyBeamDamage(world);
            }

            // If spell has expired, remove the beam entity
            if (isExpired() && beamEntity != null) {
                beamEntity.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        private void updateBeamEntity(World world) {
            if (caster == null || !caster.isAlive()) {
                if (beamEntity != null) {
                    beamEntity.discard();
                    beamEntity = null;
                }
                return;
            }

            // Get the player's eye position as the beam start position
            Vec3d start = caster.getEyePos();

            // Get the player's look direction vector
            Vec3d direction = caster.getRotationVector();

            // Calculate the beam end position by extending in the direction of player's look
            Vec3d end = start.add(direction.multiply(RANGE));

            // Update or create the beam entity
            if (beamEntity != null && !beamEntity.isRemoved()) {
                // Update existing beam
                beamEntity.updateBeam(start, end);
            } else {
                // Create new beam
                createBeamEffect(world);
            }
        }


        public void createBeamEffect(World world) {
            if (caster == null || !world.isClient) {
                return;
            }

            // Get the player's eye position
            Vec3d start = caster.getEyePos();

            // Get the player's look direction
            Vec3d direction = caster.getRotationVector();

            // Calculate the end position by extending from start along the player's look direction
            Vec3d end = start.add(direction.multiply(RANGE));

            // Create the beam entity
            beamEntity = new SolarBeamEntity(world, caster, start, end);
            world.spawnEntity(beamEntity);

            // Log the creation of the beam
            VerdantArcanum.LOGGER.info("Created solar beam from {} to {}", start, end);
        }

        private void applyBeamDamage(World world) {
            if (caster == null || !caster.isAlive() || beamEntity == null) {
                return;
            }

            // Create a collision box along the beam
            Box beamBox = createBeamCollisionBox(beamEntity.getStartPos(), beamEntity.getEndPos(), WIDTH);

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

    // This is now primarily used for client-side preview effects
    public static void createBeamParticles(World world, Vec3d startPos, Vec3d direction) {
        // This method can be removed or simplified as the beam is now an entity
    }

    public void tickActiveSpellsClient(ClientWorld world, ClientPlayerEntity player) {
        // Most of the client-side beam management is now handled by the entity renderer
        clientActiveSpells.entrySet().removeIf(entry -> {
            long expiryTime = entry.getValue();
            return System.currentTimeMillis() > expiryTime;
        });
    }

    public static void registerSounds() {
        SOLAR_BLOOM_SOUND = new SoundEvent(SOLAR_BLOOM_SOUND_ID);
        Registry.register(Registry.SOUND_EVENT, SOLAR_BLOOM_SOUND_ID, SOLAR_BLOOM_SOUND);
    }

    /**
     * Entity class for the solar beam visual effect
     */
    public static class SolarBeamEntity extends Entity {
        private UUID casterUUID;
        private Vec3d startPos;
        private Vec3d endPos;
        private float beamWidth = (float)WIDTH;
        public int age; // Track the entity's age for animations

        // Default constructor needed for entity registration
        public SolarBeamEntity(EntityType<? extends SolarBeamEntity> type, World world) {
            super(type, world);
            this.noClip = true; // Beam can pass through blocks
            this.ignoreCameraFrustum = true; // Always render beam even if not in camera view
        }

        // Constructor for creating a new beam
        public SolarBeamEntity(World world, PlayerEntity caster, Vec3d startPos, Vec3d endPos) {
            super(SOLAR_BEAM_ENTITY, world);
            this.casterUUID = caster.getUuid();
            this.startPos = startPos;
            this.endPos = endPos;
            this.noClip = true;
            this.ignoreCameraFrustum = true;

            // Position the entity at the start of the beam
            this.setPosition(startPos.x, startPos.y, startPos.z);
        }

        @Override
        protected void initDataTracker() {
            // No tracked data needed for this entity
        }

        @Override
        protected void readCustomDataFromNbt(NbtCompound nbt) {
            if (nbt.containsUuid("CasterUUID")) {
                this.casterUUID = nbt.getUuid("CasterUUID");
            }

            if (nbt.contains("StartPosX")) {
                double x = nbt.getDouble("StartPosX");
                double y = nbt.getDouble("StartPosY");
                double z = nbt.getDouble("StartPosZ");
                this.startPos = new Vec3d(x, y, z);
            }

            if (nbt.contains("EndPosX")) {
                double x = nbt.getDouble("EndPosX");
                double y = nbt.getDouble("EndPosY");
                double z = nbt.getDouble("EndPosZ");
                this.endPos = new Vec3d(x, y, z);
            }

            this.beamWidth = nbt.getFloat("BeamWidth");
            this.age = nbt.getInt("Age");
        }

        @Override
        protected void writeCustomDataToNbt(NbtCompound nbt) {
            if (this.casterUUID != null) {
                nbt.putUuid("CasterUUID", this.casterUUID);
            }

            if (this.startPos != null) {
                nbt.putDouble("StartPosX", this.startPos.x);
                nbt.putDouble("StartPosY", this.startPos.y);
                nbt.putDouble("StartPosZ", this.startPos.z);
            }

            if (this.endPos != null) {
                nbt.putDouble("EndPosX", this.endPos.x);
                nbt.putDouble("EndPosY", this.endPos.y);
                nbt.putDouble("EndPosZ", this.endPos.z);
            }

            nbt.putFloat("BeamWidth", this.beamWidth);
            nbt.putInt("Age", this.age);
        }

        public void updateBeam(Vec3d startPos, Vec3d endPos) {
            this.startPos = startPos;
            this.endPos = endPos;

            // Update the entity position to match the start position
            this.setPosition(startPos.x, startPos.y, startPos.z);
        }

        public Vec3d getStartPos() {
            return this.startPos != null ? this.startPos : this.getPos();
        }

        public Vec3d getEndPos() {
            return this.endPos != null ? this.endPos : this.getPos().add(0, 10, 0);
        }

        public float getBeamWidth() {
            return this.beamWidth;
        }

        @Override
        public void tick() {
            super.tick();

            // Increment the age for animation effects
            this.age++;

            // If the entity has existed for too long without updates, remove it
            if (this.age > 200) { // 10 seconds
                this.discard();
            }
        }

        @Override
        public Packet<?> createSpawnPacket() {
            return new EntitySpawnS2CPacket(this);
        }
    }

    /**
     * Register the beam entity and its renderer
     */
    public static void registerEntities() {
        // Register the solar beam entity
        SOLAR_BEAM_ENTITY = Registry.register(
                Registry.ENTITY_TYPE,
                new Identifier(VerdantArcanum.MOD_ID, "solar_beam"),
                FabricEntityTypeBuilder.<SolarBeamEntity>create(SpawnGroup.MISC, SolarBeamEntity::new)
                        .dimensions(EntityDimensions.fixed(0.1f, 0.1f)) // Small hitbox since visual is handled by renderer
                        .trackRangeBlocks(256) // Visible from far away
                        .trackedUpdateRate(1) // Update frequently
                        .build()
        );

        // Client-side registration should be done in your client initializer
    }
}