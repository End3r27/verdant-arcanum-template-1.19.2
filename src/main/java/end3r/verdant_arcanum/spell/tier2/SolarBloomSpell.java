package end3r.verdant_arcanum.spell.tier2;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.registry.ModEntities;
import end3r.verdant_arcanum.spell.Spell;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
        if (world.isClient) {
            return; // Client-side handling is done in playClientEffects
        }

        // Create a new spell instance
        SpellInstance instance = new SpellInstance(player, DURATION_TICKS);
        activeSpells.put(player.getUuid(), instance);

        // Initialize the beam entity
        instance.updateBeamEntity(world);

        // Play the spell sound
        world.playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SOLAR_BEAM_SOUND,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );

        player.sendMessage(Text.literal("Solar Bloom unleashed!").formatted(Formatting.GOLD), false);
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Client-side particle effects
        Vec3d lookVec = player.getRotationVector();
        Vec3d eyePos = player.getEyePos();

        LOGGER.info("SolarBloomSpell: Playing client effects");

        // Check if the entity exists on the client
        if (activeSpells.containsKey(player.getUuid())) {
            SpellInstance instance = activeSpells.get(player.getUuid());
            if (instance != null && instance.beamEntity != null) {
                LOGGER.info("SolarBloomSpell: Beam entity exists with ID: {}", instance.beamEntity.getId());
            } else {
                LOGGER.info("SolarBloomSpell: No beam entity found for this player");
            }
        }

        for (int i = 0; i < 20; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetY = (world.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * 0.5;

            Vec3d particlePos = eyePos.add(
                    lookVec.x * 0.5 + offsetX,
                    lookVec.y * 0.5 + offsetY,
                    lookVec.z * 0.5 + offsetZ
            );

            world.addParticle(
                    net.minecraft.particle.ParticleTypes.END_ROD,
                    particlePos.x, particlePos.y, particlePos.z,
                    lookVec.x * 0.3, lookVec.y * 0.3, lookVec.z * 0.3
            );
        }



    }

    @Override
    public void playFailureEffects(World world, PlayerEntity player) {
        // Failure effects
        world.playSound(
                player,
                player.getX(), player.getY(), player.getZ(),
                net.minecraft.sound.SoundEvents.BLOCK_FIRE_EXTINGUISH,
                SoundCategory.PLAYERS,
                0.6F,
                0.8F
        );

        for (int i = 0; i < 5; i++) {
            world.addParticle(
                    net.minecraft.particle.ParticleTypes.SMOKE,
                    player.getX(), player.getEyeY(), player.getZ(),
                    (world.random.nextDouble() - 0.5) * 0.2,
                    world.random.nextDouble() * 0.1,
                    (world.random.nextDouble() - 0.5) * 0.2
            );
        }
    }

    public static void tickActiveSpells(World world, Object o) {
        if (world.isClient || activeSpells.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<UUID, SpellInstance>> it = activeSpells.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, SpellInstance> entry = it.next();
            SpellInstance instance = entry.getValue();

            if (instance.isExpired()) {
                // Remove the beam entity
                if (instance.beamEntity != null) {
                    instance.beamEntity.remove(Entity.RemovalReason.DISCARDED);
                }
                it.remove();
                continue;
            }

            instance.tick(world);
        }
    }

    public class SpellInstance {
        private final PlayerEntity caster;
        private final UUID casterUUID;
        private int remainingTicks;
        private SolarBeamEntity beamEntity;

        public Vec3d start;
        public Vec3d direction;
        public Vec3d end;

        public SpellInstance(PlayerEntity caster, int duration) {
            this.caster = caster;
            this.casterUUID = caster.getUuid();
            this.remainingTicks = duration;

            // Initialize vectors with safe default values
            this.start = caster.getEyePos();
            this.direction = caster.getRotationVector();
            this.end = this.start.add(this.direction.multiply(RANGE));
        }

        public boolean isExpired() {
            return remainingTicks <= 0 || caster == null || !caster.isAlive();
        }

        public void tick(World world) {
            remainingTicks--;

            if (caster == null || !caster.isAlive()) {
                remainingTicks = 0;
                return;
            }

            // Update beam position based on player's view
            updateBeamPositions();

            // Update the beam entity
            updateBeamEntity(world);

            // Damage entities in the beam's path
            damageEntitiesInBeam(world);
        }

        private void updateBeamPositions() {
            this.start = caster.getEyePos();
            this.direction = caster.getRotationVector();

            // Perform raycast to find end position
            HitResult hitResult = caster.raycast(RANGE, 0, false);
            if (hitResult.getType() != HitResult.Type.MISS) {
                this.end = hitResult.getPos();
            } else {
                this.end = this.start.add(this.direction.multiply(RANGE));
            }
        }


        private void updateBeamEntity(World world) {
            if (beamEntity == null) {
                // Create a new beam entity if it doesn't exist
                beamEntity = new SolarBeamEntity(ModEntities.SOLAR_BEAM, world);
                beamEntity.setOwner(caster);
                world.spawnEntity(beamEntity);

                // Log entity creation
                LOGGER.info("Created SolarBeamEntity with ID {} on {}",
                        beamEntity.getId(),
                        world.isClient ? "CLIENT" : "SERVER");
            }

            // Update beam entity properties
            beamEntity.setStart(start);
            beamEntity.setEnd(end);
            beamEntity.setBeamWidth((float) WIDTH);

            // Log updates
            LOGGER.info("Updated beam entity {} at {} -> {} on {}",
                    beamEntity.getId(),
                    start,
                    end,
                    world.isClient ? "CLIENT" : "SERVER");

            // Send sync packet to all players tracking this entity
            if (!world.isClient && caster instanceof ServerPlayerEntity) {
                // If we're on the server, send sync packet to all nearby players
                for (ServerPlayerEntity player : ((ServerWorld)world).getPlayers()) {
                    if (player.squaredDistanceTo(caster) < 256 * 256) { // 256 block radius
                        end3r.verdant_arcanum.network.BeamSyncPacket.sendToClient(player, beamEntity);
                    }
                }
            }
        }

        private void damageEntitiesInBeam(World world) {
            // Create a bounding box that encompasses the beam
            double minX = Math.min(start.x, end.x) - WIDTH/2;
            double minY = Math.min(start.y, end.y) - WIDTH/2;
            double minZ = Math.min(start.z, end.z) - WIDTH/2;
            double maxX = Math.max(start.x, end.x) + WIDTH/2;
            double maxY = Math.max(start.y, end.y) + WIDTH/2;
            double maxZ = Math.max(start.z, end.z) + WIDTH/2;

            Box beamBox = new Box(minX, minY, minZ, maxX, maxY, maxZ);

            // Get entities in the box around the beam
            List<Entity> entitiesInBox = world.getEntitiesByClass(Entity.class, beamBox,
                    entity -> entity != caster && !(entity instanceof SolarBeamEntity));

            // Check each entity if it's actually in the beam (not just the box)
            for (Entity entity : entitiesInBox) {
                if (isEntityInBeam(entity, start, direction, end.distanceTo(start))) {
                    // Only damage living entities
                    if (entity instanceof LivingEntity livingEntity) {
                        livingEntity.damage(net.minecraft.entity.damage.DamageSource.MAGIC, DAMAGE_PER_TICK);
                        // Apply a fiery visual effect to affected entities
                        entity.setFireTicks(40); // 2 seconds of fire
                    }
                }
            }
        }

        private boolean isEntityInBeam(Entity entity, Vec3d beamStart, Vec3d beamDirection, double beamLength) {
            // Get entity position (center of hitbox)
            Vec3d entityPos = entity.getBoundingBox().getCenter();

            // Calculate vector from beam start to entity
            Vec3d startToEntity = entityPos.subtract(beamStart);

            // Project this vector onto the beam direction
            double dotProduct = startToEntity.dotProduct(beamDirection);

            // If projection is negative or greater than beam length, entity is not along the beam line
            if (dotProduct < 0 || dotProduct > beamLength) {
                return false;
            }

            // Find the closest point on the beam to the entity
            Vec3d closestPoint = beamStart.add(beamDirection.multiply(dotProduct));

            // Return true if entity is within WIDTH/2 of the beam
            return entityPos.distanceTo(closestPoint) <= WIDTH / 2.0;
        }
    }

    public static void registerSounds() {
        // Register the custom spell sound
        Registry.register(Registry.SOUND_EVENT, SOLAR_BEAM_SOUND_ID, SOLAR_BEAM_SOUND);
    }
}