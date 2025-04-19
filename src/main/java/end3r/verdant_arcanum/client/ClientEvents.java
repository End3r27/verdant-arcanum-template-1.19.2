package end3r.verdant_arcanum.client;

import end3r.verdant_arcanum.magic.ClientManaData;
import end3r.verdant_arcanum.magic.ManaParticleSystem;
import end3r.verdant_arcanum.magic.ManaSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.Identifier;

public class ClientEvents {
    // Define the identifier for mana regen enchantment (should match your server-side registration)
    private static final Identifier MANA_REGEN_ENCHANTMENT_ID = new Identifier("verdant_arcanum", "mana_regen");

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
                // Using correct parameters for mana sync
                ClientManaData.setMana(
                        ManaSystem.getInstance().getPlayerMana(client.player).getCurrentMana(),
                        ManaSystem.getInstance().getPlayerMana(client.player).getMaxMana(),
                        1.0f  // Default regen multiplier
                );
            }
        });

        // Check for inventory changes that might affect max mana and regen rate
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PlayerEntity player = client.player;
            if (player != null) {
                // This should be called periodically to check for inventory changes
                checkInventoryForManaChanges(player);

                // Add check for mana regen enchantment changes
                checkManaRegenEnchantment(player);
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
                ManaSystem.getInstance().syncManaToClient((ServerPlayerEntity) player);
            }
        }
    }

    // New method to check for mana regen enchantment changes
    private static void checkManaRegenEnchantment(PlayerEntity player) {
        // Calculate the regen multiplier based on current head equipment
        float newRegenMultiplier = calculateManaRegenMultiplier(player);

        // Get current regen multiplier from ClientManaData
        float currentRegenMultiplier = ClientManaData.getRegenMultiplier();

        // If there's a change in the multiplier, update the client-side data
        // Note: The actual server-side update is handled by ManaEventHandler
        if (Math.abs(newRegenMultiplier - currentRegenMultiplier) > 0.01f) {
            // This is just for visual feedback on the client side
            // (The server handles the actual regen calculation)
            if (player.world.isClient) {

                // Update client-side data with the new multiplier (keeps current and max mana the same)
                ClientManaData.setMana(
                        ClientManaData.getCurrentMana(),
                        ClientManaData.getMaxMana(),
                        newRegenMultiplier
                );
            }
        }
    }

    // Calculate mana regen multiplier based on enchantment level
    private static float calculateManaRegenMultiplier(PlayerEntity player) {
        ItemStack headItem = player.getEquippedStack(EquipmentSlot.HEAD);
        if (headItem.isEmpty()) {
            return 1.0f; // Default multiplier with no head item
        }

        // Get the enchantment registry instance
        var enchantment = Registry.ENCHANTMENT.get(MANA_REGEN_ENCHANTMENT_ID);
        if (enchantment == null) {
            return 1.0f; // Enchantment doesn't exist or isn't registered yet
        }

        // Get the level of the mana regen enchantment
        int regenLevel = EnchantmentHelper.getLevel(enchantment, headItem);

        // Calculate and return the multiplier (same formula as server-side)
        return 1.0f + (regenLevel * 0.25f);
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