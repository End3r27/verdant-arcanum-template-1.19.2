package end3r.verdant_arcanum.magic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.util.math.random.Random;

import static end3r.verdant_arcanum.magic.ClientManaData.getManaPercentage;


/**
 * Handles mana-related particle effects
 */
public class ManaParticleSystem {
    // Singleton instance
    private static ManaParticleSystem INSTANCE;

    // Random for particle variation
    private final Random random;

    // Thresholds for different particle effects
    private static final float LOW_MANA_THRESHOLD = 0.25f; // 25% or less mana
    private static final float REGEN_PARTICLE_CHANCE = 0.2f; // 20% chance per tick for regen particles

    // Colors for particles
    private static final Vec3f LOW_MANA_COLOR = new Vec3f(0.7f, 0.0f, 0.0f); // Red
    private static final Vec3f REGEN_MANA_COLOR = new Vec3f(0.0f, 0.7f, 1.0f); // Light blue

    private ManaParticleSystem() {
        this.random = Random.create();
    }

    /**
     * Get the singleton instance
     */
    public static ManaParticleSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ManaParticleSystem();
        }
        return INSTANCE;
    }

    /**
     * Create particles based on player's mana state
     * Call this method each tick for players
     */
    public void updateManaParticles(PlayerEntity player) {
        ManaSystem.PlayerMana playerMana = ManaSystem.getInstance().getPlayerMana(player);
        float manaPercent = getManaPercentage();

        // Only show particles for the client's player
        if (MinecraftClient.getInstance().player != player) {
            return;
        }

        // Low mana warning particles
        if (manaPercent <= LOW_MANA_THRESHOLD) {
            spawnLowManaParticles(player);
        }

        // Mana regeneration particles (check if mana is regenerating and not at max)
        if (manaPercent < 1.0f && random.nextFloat() < REGEN_PARTICLE_CHANCE) {
            spawnManaRegenParticles(player);
        }
    }

    /**
     * Spawn particles when player's mana is low
     */
    private void spawnLowManaParticles(PlayerEntity player) {
        // Get player position
        Vec3d pos = player.getPos();

        // Add slight offset from player's feet
        double x = pos.x;
        double y = pos.y + 0.1;
        double z = pos.z;

        // Spawn 1-2 red dust particles at player's feet
        int particleCount = random.nextBetween(1, 2);
        for (int i = 0; i < particleCount; i++) {
            // Add random offset
            double offsetX = (random.nextFloat() - 0.5) * 0.5;
            double offsetZ = (random.nextFloat() - 0.5) * 0.5;

            player.getWorld().addParticle(
                    new DustParticleEffect(LOW_MANA_COLOR, 1.0f),
                    x + offsetX,
                    y,
                    z + offsetZ,
                    0.0, 0.0, 0.0);
        }
    }

    /**
     * Spawn particles when player's mana is regenerating
     */
    private void spawnManaRegenParticles(PlayerEntity player) {
        // Get player position
        Vec3d pos = player.getPos();

        // Calculate particle position (spiral around player)
        double angle = random.nextFloat() * Math.PI * 2;
        double radius = 0.7;
        double x = pos.x + Math.cos(angle) * radius;
        double y = pos.y + random.nextFloat() * 1.8; // Random height from feet to head
        double z = pos.z + Math.sin(angle) * radius;

        // Spiral movement
        double motionX = Math.cos(angle) * 0.02;
        double motionY = 0.05 + (random.nextFloat() * 0.05);
        double motionZ = Math.sin(angle) * 0.02;

        // Spawn dust particle
        player.getWorld().addParticle(
                new DustParticleEffect(REGEN_MANA_COLOR, 0.8f),
                x, y, z,
                motionX, motionY, motionZ);

        // Occasionally add sparkle effect
        if (random.nextFloat() < 0.3) {
            player.getWorld().addParticle(
                    ParticleTypes.END_ROD,
                    x, y, z,
                    motionX * 0.5, motionY * 0.5, motionZ * 0.5);
        }
    }

    /**
     * Create a burst of particles when a large amount of mana is consumed (for spells)
     */
    public void createManaConsumptionBurst(PlayerEntity player, float manaAmount) {
        // Only show particles for significant mana usage (10+)
        if (manaAmount < 10) {
            return;
        }

        // Get player position
        Vec3d pos = player.getPos();
        double x = pos.x;
        double y = pos.y + 1.0; // Around chest height
        double z = pos.z;

        // Scale particle count with mana used
        int particleCount = Math.min(50, (int)(manaAmount / 2));

        // Create burst of particles
        for (int i = 0; i < particleCount; i++) {
            double offsetX = (random.nextFloat() - 0.5) * 0.8;
            double offsetY = (random.nextFloat() - 0.5) * 0.8;
            double offsetZ = (random.nextFloat() - 0.5) * 0.8;

            double motionX = offsetX * 0.1;
            double motionY = offsetY * 0.1 + 0.05;
            double motionZ = offsetZ * 0.1;

            // Mix of blue dust and sparkles
            if (random.nextFloat() < 0.7) {
                // Calculate color based on mana amount (more red for higher mana usage)
                float redComponent = Math.min(1.0f, manaAmount / 100f);
                Vec3f color = new Vec3f(redComponent, 0.2f, 0.8f);

                player.getWorld().addParticle(
                        new DustParticleEffect(color, 1.0f),
                        x + offsetX, y + offsetY, z + offsetZ,
                        motionX, motionY, motionZ);
            } else {
                player.getWorld().addParticle(
                        ParticleTypes.WITCH,
                        x + offsetX, y + offsetY, z + offsetZ,
                        motionX, motionY, motionZ);
            }
        }
    }
}