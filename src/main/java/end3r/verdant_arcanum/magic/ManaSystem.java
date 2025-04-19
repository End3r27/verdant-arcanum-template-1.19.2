package end3r.verdant_arcanum.magic;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static end3r.verdant_arcanum.magic.ClientManaData.currentMana;
import static end3r.verdant_arcanum.magic.ClientManaData.maxMana;


/**
 * Manages player mana for spell casting
 */
public class ManaSystem {
    // Singleton instance
    private static ManaSystem INSTANCE;

    // Map of player UUIDs to their current mana
    private final Map<UUID, PlayerMana> playerManaMap = new HashMap<>();

    // Default max mana for new players
    public static final int DEFAULT_MAX_MANA = 100;

    // Default mana regeneration rate
    public static final float DEFAULT_MANA_REGEN_RATE = 2f;

    // Map for tracking custom regeneration multipliers per player
    private final Map<UUID, Float> regenMultipliers = new HashMap<>();

    private ManaSystem() {
        // Private constructor for singleton
    }

    /**
     * Get the singleton instance of ManaSystem.
     */
    public static ManaSystem getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ManaSystem();
        }
        return INSTANCE;
    }

    /**
     * Get a player's mana data, creating it if it doesn't exist.
     */
    public PlayerMana getPlayerMana(PlayerEntity player) {
        UUID playerId = player.getUuid();
        return playerManaMap.computeIfAbsent(playerId, id -> new PlayerMana(DEFAULT_MAX_MANA));
    }

    /**
     * Check if a player has enough mana to cast a spell or perform an action.
     *
     * @param player the player entity
     * @param manaCost the mana amount required
     * @return true if the player has enough mana, false otherwise
     */
    public boolean hasEnoughMana(PlayerEntity player, int manaCost) {
        PlayerMana playerMana = getPlayerMana(player);
        return playerMana.getCurrentMana() >= manaCost;
    }

    /**
     * Use mana for a spell or action if the player has enough.
     *
     * @param player the player entity
     * @param amount the mana amount to consume
     * @return true if mana was consumed, false if not enough mana
     */
    public boolean useMana(PlayerEntity player, int amount) {
        if (hasEnoughMana(player, amount)) {
            PlayerMana playerMana = getPlayerMana(player);
            playerMana.consumeMana(amount);
            ManaSyncPacket.sendToClient((ServerPlayerEntity) player, playerMana);

            return true;
        }
        return false;
    }

    public void updateManaRegen(PlayerEntity player) {
        updateManaRegen(player, 1.0f);
    }

    /**
     * Update mana regeneration for a player, allowing custom multipliers.
     *
     * @param player the player entity
     * @param regenMultiplier a multiplier to apply to the default mana regen rate
     */
    public void updateManaRegen(PlayerEntity player, float regenMultiplier) {
        UUID playerId = player.getUuid();
        regenMultipliers.put(playerId, regenMultiplier);

        PlayerMana playerMana = getPlayerMana(player);
        float regenAmount = DEFAULT_MANA_REGEN_RATE * regenMultiplier;
        playerMana.regenerateMana(regenAmount);

        // If this is a server player, sync to client
        if (player instanceof ServerPlayerEntity) {
            ManaSyncPacket.sendToClient((ServerPlayerEntity) player, playerMana);
        }
    }

    /**
     * Save player mana data to NBT
     */
    public void savePlayerData(ServerPlayerEntity player) {
        PlayerMana playerMana = getPlayerMana(player);

        // Create a new NBT compound for storing mana data
        NbtCompound manaData = new NbtCompound();

        // Store the relevant mana data
        manaData.putInt("MaxMana", playerMana.getMaxMana());
        manaData.putFloat("CurrentMana", playerMana.getCurrentMana());

        // Get player's NBT data
        NbtCompound playerTag = new NbtCompound();
        player.writeNbt(playerTag);

        // Add our custom data
        if (!playerTag.contains("verdant_arcanum")) {
            playerTag.put("verdant_arcanum", new NbtCompound());
        }

        NbtCompound modData = playerTag.getCompound("verdant_arcanum");
        modData.put("mana", manaData);

        // Handle storage via server events/hooks in your main mod class
        // This data will need to be saved when the player data is saved

        System.out.println("Prepared mana data for " + player.getName().getString() +
                ": " + playerMana.getCurrentMana() + "/" +
                playerMana.getMaxMana());
    }

    /**
     * Load player mana data from NBT
     */
    public void loadPlayerData(ServerPlayerEntity player) {
        // Get player's NBT data
        NbtCompound playerTag = new NbtCompound();
        player.writeNbt(playerTag);

        if (playerTag.contains("verdant_arcanum")) {
            NbtCompound modData = playerTag.getCompound("verdant_arcanum");

            if (modData.contains("mana")) {
                // Retrieved stored mana data
                NbtCompound manaData = modData.getCompound("mana");

                int maxMana = manaData.contains("MaxMana") ?
                        manaData.getInt("MaxMana") : DEFAULT_MAX_MANA;

                float currentMana = manaData.contains("CurrentMana") ?
                        manaData.getFloat("CurrentMana") : DEFAULT_MAX_MANA;

                // Get or create the player's mana object
                PlayerMana playerMana = getPlayerMana(player);

                // Update with saved values
                playerMana.setMaxMana(maxMana);
                playerMana.setCurrentMana(currentMana);

                System.out.println("Loaded mana for " + player.getName().getString() +
                        ": " + currentMana + "/" + maxMana);

                // Make sure to sync with client
                syncManaToClient(player);
                return;
            }
        }

        // No saved data found, initialize with defaults
        PlayerMana playerMana = getPlayerMana(player);
        playerMana.setMaxMana(DEFAULT_MAX_MANA);
        playerMana.setCurrentMana(DEFAULT_MAX_MANA); // Start with full mana

        System.out.println("No mana data found for " + player.getName().getString() +
                ". Initializing with default values.");

        // Make sure to sync with client
        syncManaToClient(player);
    }

    /**
     * Sync player mana data to client
     */
    public void syncManaToClient(PlayerEntity player) {
        PlayerMana playerMana = getPlayerMana(player);

        // Create packet data
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(playerMana.getMaxMana());
        buf.writeFloat(playerMana.getCurrentMana());

        // Send to client
        ServerPlayNetworking.send(
                (ServerPlayerEntity) player,
                new Identifier("verdant_arcanum", "mana_sync"),
                buf
        );
    }

    /**
     * Updates a player's maximum mana capacity
     *
     * @param player the player entity
     * @param newMaxMana the new maximum mana value
     * @return true if the max mana changed, false otherwise
     */
    public boolean updatePlayerMaxMana(PlayerEntity player, int newMaxMana) {
        PlayerMana playerMana = getPlayerMana(player);

        // Only update if the value is different
        if (playerMana.getMaxMana() != newMaxMana) {
            playerMana.setMaxMana(newMaxMana);

            // If on server side and the player is a server player, sync to client
            if (!player.world.isClient && player instanceof ServerPlayerEntity) {
                syncManaToClient((ServerPlayerEntity) player);
            }

            return true;
        }

        return false;
    }

    /**
     * Inner class to represent a player's mana data.
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

        public void setMaxMana(int newMaxMana) {
            this.maxMana = newMaxMana;
            // Ensure current mana doesn't exceed new max
            if (this.currentMana > newMaxMana) {
                this.currentMana = newMaxMana;
            }
        }

        public float getCurrentMana() {
            return currentMana;
        }

        /**
         * Set the player's current mana to a specific value
         * @param value The new mana value
         */
        public void setCurrentMana(float value) {
            // Ensure mana doesn't exceed max mana
            this.currentMana = Math.min(value, ClientManaData.maxMana);
        }

        public void consumeMana(float amount) {
            this.currentMana = Math.max(0, this.currentMana - amount);
        }

        public void regenerateMana(float amount) {
            this.currentMana = Math.min(this.maxMana, this.currentMana + amount);
        }
    }
    /**
     * Calculates the player's current mana as a percentage of maximum mana
     * @return The percentage of mana (0.0f to 1.0f)
     */
    public float getManaPercentage() {
        // Avoid division by zero
        if (maxMana <= 0) {
            return 0.0f;
        }

        // Calculate percentage (0.0f to 1.0f)
        return Math.min(1.0f, currentMana / (float)maxMana);
    }
}