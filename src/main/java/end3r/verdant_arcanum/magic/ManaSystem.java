package end3r.verdant_arcanum.magic;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

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
        return playerManaMap.computeIfAbsent(player.getUuid(), uuid -> new PlayerMana(DEFAULT_MAX_MANA));
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
        PlayerMana playerMana = getPlayerMana(player);
        if (playerMana.getCurrentMana() >= amount) {
            playerMana.consumeMana(amount);

            // Add visual effects for significant mana usage
            if (!player.getWorld().isClient && amount >= 10) {
                // Server-side: sync to client
                syncManaToClient(player);
            } else if (player.getWorld().isClient) {
                // Client-side: show particles
                ManaParticleSystem.getInstance().createManaConsumptionBurst(player, amount);
            }

            return true;
        }
        return false;
    }



    public void updateManaRegen(PlayerEntity player) {
        updateManaRegen(player, 1.0f); // Default multiplier of 1.0 (no buff)
    }

    /**
     * Update mana regeneration for a player, allowing custom multipliers.
     *
     * @param player the player entity
     * @param regenMultiplier a multiplier to apply to the default mana regen rate
     */
    public void updateManaRegen(PlayerEntity player, float regenMultiplier) {
        PlayerMana playerMana = getPlayerMana(player);
        regenMultipliers.put(player.getUuid(), regenMultiplier);


        float currentMana = playerMana.getCurrentMana();
        if (currentMana < playerMana.getMaxMana()) {
            float regenRate = DEFAULT_MANA_REGEN_RATE * regenMultiplier;
            float oldMana = playerMana.getCurrentMana();
            playerMana.regenerateMana(regenRate);

            // Add this line to make sure changes sync to client
            syncManaToClient(player);
        }
    }

    /**
     * Save player mana to NBT data.
     */
    public void savePlayerMana(PlayerEntity player, NbtCompound nbt) {
        PlayerMana playerMana = getPlayerMana(player);
        NbtCompound manaData = new NbtCompound();
        manaData.putInt("MaxMana", playerMana.getMaxMana());
        manaData.putFloat("CurrentMana", playerMana.getCurrentMana());
        nbt.put("PlayerMana", manaData);
    }

    /**
     * Load player mana from NBT data.
     */
    public void loadPlayerMana(PlayerEntity player, NbtCompound nbt) {
        if (nbt.contains("PlayerMana")) {
            NbtCompound manaData = nbt.getCompound("PlayerMana");
            int maxMana = manaData.getInt("MaxMana");
            float currentMana = manaData.getFloat("CurrentMana");
            playerManaMap.put(player.getUuid(), new PlayerMana(maxMana, currentMana));
        }
    }

    /**
     * Inner class to represent a player's mana data.
     */
    public static class PlayerMana {
        private int maxMana;
        private float currentMana;

        public PlayerMana(int maxMana) {
            this.maxMana = maxMana;
            this.currentMana = maxMana; // Start with full mana
        }

        public PlayerMana(int maxMana, float currentMana) {
            this.maxMana = maxMana;
            this.currentMana = currentMana;
        }



        public int getMaxMana() {
            return maxMana;
        }

        public void setMaxMana(int newMaxMana) {
            // Save the percentage of mana the player had
            float manaPercentage = currentMana / (float)maxMana;

            // Update max mana
            this.maxMana = newMaxMana;

            // Set current mana to keep the same percentage (with bounds checking)
            this.currentMana = Math.min(newMaxMana, Math.max(0, manaPercentage * newMaxMana));
        }

        public float getCurrentMana() {
            return currentMana;
        }

        public void consumeMana(float amount) {
            currentMana -= amount;
            if (currentMana < 0) {
                currentMana = 0; // Mana can't go negative
            }
        }

        public void regenerateMana(float amount) {
            currentMana += amount;
            if (currentMana > maxMana) {
                currentMana = maxMana; // Mana can't exceed max mana
            }
        }

        /**
         * Get current mana as a percentage of the maximum mana.
         * This method returns a float (range 0.0 to 1.0).
         *
         * @return current mana percentage
         */
        public float getManaPercentage() {
            return currentMana / maxMana;
        }
    }
    // In ManaSystem.java, add a method to sync mana data:
    public void syncManaToClient(PlayerEntity player) {
        if (player instanceof ServerPlayerEntity serverPlayer) {
            PlayerMana playerMana = getPlayerMana(player);
            float multiplier = regenMultipliers.getOrDefault(player.getUuid(), 1.0f); // Get the stored multiplier
            ManaSyncPacket packet = new ManaSyncPacket(playerMana.getCurrentMana(), playerMana.getMaxMana(), multiplier);
            ManaSyncPacket.send(serverPlayer, packet);
        }
    }

    public void updatePlayerMaxMana(PlayerEntity player, int newMaxMana) {
        PlayerMana playerMana = getPlayerMana(player);
        playerMana.setMaxMana(newMaxMana);
    }
    // In a commands registration class
    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, registryAccess) -> {
            dispatcher.register(
                    CommandManager.literal("setmana")
                            .requires(source -> source.hasPermissionLevel(2)) // Op level 2+
                            .then(CommandManager.argument("amount", IntegerArgumentType.integer(0))
                                    .executes(context -> {
                                        ServerPlayerEntity player = context.getSource().getPlayer();
                                        int amount = IntegerArgumentType.getInteger(context, "amount");

                                        ManaSystem manaSystem = ManaSystem.getInstance();
                                        ManaSystem.PlayerMana playerMana = manaSystem.getPlayerMana(player);
                                        playerMana.setMaxMana(amount);

                                        // Sync to client if needed
                                        manaSystem.syncManaToClient(player);

                                        context.getSource().sendFeedback(Text.of("Set mana to " + amount), false);
                                        return 1;
                                    })
                            )
            );
        });
    }
}