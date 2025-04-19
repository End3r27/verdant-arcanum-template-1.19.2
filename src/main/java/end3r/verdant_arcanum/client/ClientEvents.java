package end3r.verdant_arcanum.client;

import end3r.verdant_arcanum.magic.ClientManaData;
import end3r.verdant_arcanum.magic.ManaParticleSystem;
import end3r.verdant_arcanum.magic.ManaSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class ClientEvents {
    public static void registerClientEvents() {
        // Register a tick handler to update mana particles
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null && !client.isPaused()) {
                // Update mana particles for the client player
                ManaParticleSystem.getInstance().updateManaParticles(client.player);
            }
        });
    }

    // In your client events registration class
    public static void registerInventoryChangeEvents() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Initial sync when player joins
            if (client.player != null) {
                // Replace the undefined syncPlayerMana call with:
                ClientManaData.setMana(
                        ManaSystem.getInstance().getPlayerMana(client.player).getCurrentMana(),
                        ManaSystem.getInstance().getPlayerMana(client.player).getMaxMana()
                );
            }
        });

        // Check for inventory changes that might affect max mana
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;
            if (player != null) {
                // This should be called periodically to check for inventory changes
                checkInventoryForManaChanges(player);
            }
        });
    }

    private static void checkInventoryForManaChanges(PlayerEntity player) {
        // Calculate new max mana based on current inventory
        int newMaxMana = calculatePlayerMaxMana(player);

        // Get current max mana
        int currentMaxMana = ManaSystem.getInstance().getPlayerMana(player).getMaxMana();

        // If max mana has changed, update it
        if (newMaxMana != currentMaxMana) {
            // Server-side: update the player's max mana
            if (!player.world.isClient) {
                ManaSystem.getInstance().updatePlayerMaxMana(player, newMaxMana);
                ManaSystem.getInstance().syncManaToClient(player);
            }
        }
    }

    // Method to calculate max mana based on player's inventory and enchantments
    private static int calculatePlayerMaxMana(PlayerEntity player) {
        int baseMana = ManaSystem.DEFAULT_MAX_MANA;
        int bonusMana = 0;

        // Check all equipment slots and inventory for items that increase max mana
        // For example, check armor pieces for enchantments

        // Check for enchantments on all equipped items
        for (ItemStack stack : player.getArmorItems()) {
            // Look for your custom mana enchantment
            // Example: bonusMana += getManaEnchantmentBonus(stack);
        }


        bonusMana += getManaEnchantmentBonus(player.getMainHandStack());
        bonusMana += getManaEnchantmentBonus(player.getOffHandStack());

        return baseMana + bonusMana;
    }

    // Helper method to get mana bonus from an enchanted item
    private static int getManaEnchantmentBonus(ItemStack stack) {
        // Replace this with your actual enchantment check
        // For example:
        // int level = EnchantmentHelper.getLevel(ModEnchantments.MANA_BOOST, stack);
        // return level * 25; // 25 mana per level
        return 0; // Placeholder
    }
}