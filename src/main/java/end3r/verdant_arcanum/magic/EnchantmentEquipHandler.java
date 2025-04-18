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
        // This handles cases where the player equips/unequips armor directly
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
            processEnchantmentChange(player, oldStack, newStack);
        }
    }

    /**
     * Process enchantment changes between two item stacks
     */
    private static void processEnchantmentChange(PlayerEntity player, ItemStack oldStack, ItemStack newStack) {
        // Get enchantment levels on old and new items
        int oldMaxMana = getEnchantmentLevel(oldStack, ModEnchantments.MAX_MANA);
        int oldManaRegen = getEnchantmentLevel(oldStack, ModEnchantments.MANA_REGEN);

        int newMaxMana = getEnchantmentLevel(newStack, ModEnchantments.MAX_MANA);
        int newManaRegen = getEnchantmentLevel(newStack, ModEnchantments.MANA_REGEN);

        // If there's any change in enchantments, recalculate all player mana effects
        if (oldMaxMana != newMaxMana || oldManaRegen != newManaRegen) {
            ManaHandler.applyManaEffects(player);
        }
    }

    /**
     * Helper method to get enchantment level from an item stack
     */
    private static int getEnchantmentLevel(ItemStack stack, net.minecraft.enchantment.Enchantment enchantment) {
        if (stack.isEmpty()) {
            return 0;
        }
        return EnchantmentHelper.getLevel(enchantment, stack);
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