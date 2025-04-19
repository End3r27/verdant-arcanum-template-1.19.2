package end3r.verdant_arcanum.magic;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;



public class ManaEventHandler {
    private static Enchantment maxManaEnchantment;
    private static Enchantment manaRegenEnchantment;

    public static final Identifier MANA_SYNC_ID = new Identifier("verdant_arcanum", "mana_sync");

    private static final Map<UUID, LastEquipmentState> playerEquipmentStates = new HashMap<>();


    public static void initialize(Enchantment maxManaEnchant, Enchantment manaRegenEnchant) {
        maxManaEnchantment = maxManaEnchant;
        manaRegenEnchantment = manaRegenEnchant;
        registerEvents();
    }


    private static void registerEvents() {
        ServerTickEvents.END_SERVER_TICK.register(ManaEventHandler::onServerTick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> updateEquipmentState(handler.player));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> playerEquipmentStates.remove(handler.player.getUuid()));
    }


    private static void onServerTick(MinecraftServer server) {
        ManaSystem manaSystem = ManaSystem.getInstance();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            checkEquipmentChange(player);

            // Check if it's time to update mana regen (once per second)
            if (player.age % 20 == 0) {
                float regenMultiplier = calculateManaRegenMultiplier(player);

                if (regenMultiplier > 1.0f) {
                }

                // This line is critical - add it to apply regeneration for ALL players
                ManaSystem.getInstance().updateManaRegen(player, regenMultiplier);
            }
        }
    }


    private static void checkEquipmentChange(ServerPlayerEntity player) {
        LastEquipmentState lastState = playerEquipmentStates.get(player.getUuid());
        if (lastState == null) {
            updateEquipmentState(player);
            updatePlayerMaxMana(player);
            return;
        }

        ItemStack currentChestItem = player.getEquippedStack(EquipmentSlot.CHEST);
        if (!ItemStack.areEqual(currentChestItem, lastState.chestItem)) {
            updatePlayerMaxMana(player);
            lastState.chestItem = currentChestItem.copy();
        }

        ItemStack currentHeadItem = player.getEquippedStack(EquipmentSlot.HEAD);
        if (!ItemStack.areEqual(currentHeadItem, lastState.headItem)) {
            // Immediately update mana regen when head item changes
            float regenMultiplier = calculateManaRegenMultiplier(player);
            ManaSystem.getInstance().updateManaRegen(player, regenMultiplier);
            lastState.headItem = currentHeadItem.copy();
        }
    }


    private static void updateEquipmentState(ServerPlayerEntity player) {
        LastEquipmentState state = new LastEquipmentState(
                player.getEquippedStack(EquipmentSlot.CHEST).copy(),
                player.getEquippedStack(EquipmentSlot.HEAD).copy()
        );
        playerEquipmentStates.put(player.getUuid(), state);
    }


    private static void updatePlayerMaxMana(ServerPlayerEntity player) {
        ManaSystem manaSystem = ManaSystem.getInstance(); // Get instance first
        ManaSystem.PlayerMana playerMana = manaSystem.getPlayerMana(player);

        int maxManaBonus = calculateMaxManaBonus(player);
        int newMaxMana = ManaSystem.DEFAULT_MAX_MANA + maxManaBonus;

        if (playerMana.getMaxMana() != newMaxMana) {
            playerMana.setMaxMana(newMaxMana);

            // Need to sync after changing max mana
            manaSystem.syncManaToClient(player); // Use the instance
        }
    }




    private static int calculateMaxManaBonus(ServerPlayerEntity player) {
        int maxManaLevel = EnchantmentHelper.getLevel(
                maxManaEnchantment,
                player.getEquippedStack(EquipmentSlot.CHEST));

        return maxManaLevel * 20;
    }


    private static float calculateManaRegenMultiplier(ServerPlayerEntity player) {
        int regenLevel = EnchantmentHelper.getLevel(
                manaRegenEnchantment,
                player.getEquippedStack(EquipmentSlot.HEAD));

        return 1.0f + (regenLevel * 0.25f);
    }


    private static class LastEquipmentState {
        ItemStack chestItem;
        ItemStack headItem;

        LastEquipmentState(ItemStack chestItem, ItemStack headItem) {
            this.chestItem = chestItem;
            this.headItem = headItem;
        }
    }

}