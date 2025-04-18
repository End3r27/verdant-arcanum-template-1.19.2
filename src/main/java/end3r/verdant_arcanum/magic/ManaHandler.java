package end3r.verdant_arcanum.magic;

import end3r.verdant_arcanum.registry.ModEnchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class ManaHandler {
    // Track the player’s last-known enchantment levels
    private static final Map<UUID, PlayerManaData> playerManaDataMap = new HashMap<>();

    public static void applyManaEffects(PlayerEntity player) {
        ManaSystem manaSystem = ManaSystem.getInstance();
        ManaSystem.PlayerMana playerMana = manaSystem.getPlayerMana(player);

        // Current enchantment levels
        int currentMaxManaBonus = EnchantmentHelper.getEquipmentLevel(ModEnchantments.MAX_MANA, player);
        int currentRegenBonus = EnchantmentHelper.getEquipmentLevel(ModEnchantments.MANA_REGEN, player);

        // Retrieve or initialize the previously stored data for this player
        UUID playerId = player.getUuid();
        PlayerManaData previousData = playerManaDataMap.getOrDefault(playerId, new PlayerManaData(0, 0));

        // Check if maxManaBonus has changed
        if (currentMaxManaBonus != previousData.maxManaBonus) {
            if (currentMaxManaBonus > 0) {
                int extraMana = currentMaxManaBonus * 10;
                playerMana.setMaxMana(ManaSystem.DEFAULT_MAX_MANA + extraMana);
                System.out.println("[DEBUG] Updated maxMana for player: " + player.getName().getString());
            } else {
                playerMana.setMaxMana(ManaSystem.DEFAULT_MAX_MANA); // Reset to default
                System.out.println("[DEBUG] Reset maxMana to default for player: " + player.getName().getString());
            }
        }

        // Check if manaRegeneration multiplier has changed
        if (currentRegenBonus != previousData.regenBonus) {
            if (currentRegenBonus > 0) {
                float regenMultiplier = 1 + (currentRegenBonus * 0.1f);
                manaSystem.updateManaRegen(player, regenMultiplier);
                System.out.println("[DEBUG] Updated manaRegen multiplier for player: " + player.getName().getString());
            } else {
                manaSystem.updateManaRegen(player, 1.0f); // Reset to default regeneration
                System.out.println("[DEBUG] Reset manaRegen multiplier to default for player: " + player.getName().getString());
            }
        }

        // Update the cached data for this player
        playerManaDataMap.put(playerId, new PlayerManaData(currentMaxManaBonus, currentRegenBonus));
    }

    // Helper class for storing player's enchantment data
    private static class PlayerManaData {
        public int maxManaBonus;
        public int regenBonus;

        public PlayerManaData(int maxManaBonus, int regenBonus) {
            this.maxManaBonus = maxManaBonus;
            this.regenBonus = regenBonus;
        }
    }
}