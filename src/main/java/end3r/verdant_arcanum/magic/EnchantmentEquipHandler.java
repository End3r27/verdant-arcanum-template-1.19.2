package end3r.verdant_arcanum.magic;

import end3r.verdant_arcanum.registry.ModEnchantments;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Handles application and removal of mana-related enchantment effects
 * when armor is equipped or unequipped.
 */
public class EnchantmentEquipHandler {

    /**
     * Register event handlers for equipment changes
     */
    public static void registerEvents() {
        // This handles cases where the player respawns
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // Re-apply mana effects after respawn
            ManaHandler.applyManaEffects(newPlayer);
        });

        // Initialize the system
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            // Re-apply mana effects when player data is copied (e.g., dimension changes)
            ManaHandler.applyManaEffects(newPlayer);
        });
    }

    /**
     * Called when an item is equipped in any slot
     *
     * @param player The player equipping the item
     * @param slot The equipment slot being used
     * @param oldStack The previous item in the slot
     * @param newStack The new item being equipped
     */
    public static void onEquipmentChange(PlayerEntity player, EquipmentSlot slot, ItemStack oldStack, ItemStack newStack) {
        // Only process armor slots
        if (isArmorSlot(slot)) {
            // We don't need to compare old and new stacks - just trigger a full recalculation
            // This ensures the effects are properly applied when equipping enchanted armor
            ManaHandler.applyManaEffects(player);
        }
    }

    /**
     * Check if the equipment slot is an armor slot
     */
    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD ||
                slot == EquipmentSlot.CHEST ||
                slot == EquipmentSlot.LEGS ||
                slot == EquipmentSlot.FEET;
    }
}