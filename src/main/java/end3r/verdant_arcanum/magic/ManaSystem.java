package end3r.verdant_arcanum.magic;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player mana for spell casting
 */
public class ManaSystem {
    // Singleton instance
    private static ManaSystem INSTANCE;

    // Map of player UUIDs to their current mana
    private final Map<UUID, PlayerMana> playerManaMap = new HashMap<>();

    // Default max mana for new players
    private static final int DEFAULT_MAX_MANA = 100;

    // Mana regeneration rate (per tick)
    private static final float MANA_REGEN_RATE = 0.05f;

    private ManaSystem() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance
     */
    public static ManaSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ManaSystem();
        }
        return INSTANCE;
    }

    /**
     * Get a player's mana data, creating it if it doesn't exist
     */
    public PlayerMana getPlayerMana(PlayerEntity player) {
        UUID playerUuid = player.getUuid();

        if (!playerManaMap.containsKey(playerUuid)) {
            playerManaMap.put(playerUuid, new PlayerMana(DEFAULT_MAX_MANA));
        }

        return playerManaMap.get(playerUuid);
    }

    /**
     * Use mana for a spell if available
     * @return true if the spell can be cast, false if not enough mana
     */
    public boolean useMana(PlayerEntity player, int amount) {
        PlayerMana mana = getPlayerMana(player);

        if (mana.getCurrentMana() >= amount) {
            mana.consumeMana(amount);

            // Spawn mana consumption particles on the client side
            if (player.getWorld().isClient()) {
                ManaParticleSystem.getInstance().createManaConsumptionBurst(player, amount);
            }

            return true;
        }

        return false;
    }

    /**
     * Update mana regeneration for a player (call each tick)
     */
    public void updateManaRegen(PlayerEntity player) {
        PlayerMana mana = getPlayerMana(player);
        float prevMana = mana.getCurrentMana();
        mana.regenerateMana(MANA_REGEN_RATE);

        // Update particles on client side
        if (player.getWorld().isClient()) {
            ManaParticleSystem.getInstance().updateManaParticles(player);
        }
    }

    /**
     * Save player mana to NBT data
     */
    public void savePlayerMana(PlayerEntity player, NbtCompound nbt) {
        PlayerMana mana = getPlayerMana(player);
        nbt.putFloat("currentMana", mana.getCurrentMana());
        nbt.putInt("maxMana", mana.getMaxMana());
    }

    /**
     * Load player mana from NBT data
     */
    public void loadPlayerMana(PlayerEntity player, NbtCompound nbt) {
        if (nbt.contains("currentMana") && nbt.contains("maxMana")) {
            float currentMana = nbt.getFloat("currentMana");
            int maxMana = nbt.getInt("maxMana");
            playerManaMap.put(player.getUuid(), new PlayerMana(maxMana, currentMana));
        }
    }

    /**
     * Inner class to store mana data for a player
     */
    public static class PlayerMana {
        private int maxMana;
        private float currentMana;

        public PlayerMana(int maxMana) {
            this.maxMana = maxMana;
            this.currentMana = maxMana;
        }

        public PlayerMana(int maxMana, float currentMana) {
            this.maxMana = maxMana;
            this.currentMana = Math.min(currentMana, maxMana);
        }

        public int getMaxMana() {
            return maxMana;
        }

        public float getCurrentMana() {
            return currentMana;
        }

        public void setMaxMana(int maxMana) {
            this.maxMana = maxMana;
            // Cap current mana to max
            if (currentMana > maxMana) {
                currentMana = maxMana;
            }
        }

        public void consumeMana(float amount) {
            currentMana = Math.max(0, currentMana - amount);
        }

        public void regenerateMana(float amount) {
            currentMana = Math.min(maxMana, currentMana + amount);
        }

        /**
         * Get current mana as a percentage (0.0 to 1.0)
         */
        public float getManaPercentage() {
            return currentMana / maxMana;
        }
    }
    // Add this method to ManaSystem.java
    public boolean hasEnoughMana(PlayerEntity player, int manaCost) {
        // Check if the player has enough mana without consuming it
        return getPlayerMana(player).getCurrentMana() >= manaCost;
    }
}