package end3r.verdant_arcanum.client.ui;

import end3r.verdant_arcanum.magic.ClientManaData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import end3r.verdant_arcanum.item.SpellEssenceItem;
import end3r.verdant_arcanum.item.LivingStaffItem;
import end3r.verdant_arcanum.spell.Spell;
import end3r.verdant_arcanum.registry.SpellRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ManaHudRenderer implements HudRenderCallback {
    // Mana bar dimensions
    private static final int MANA_BAR_WIDTH = 100;
    private static final int MANA_BAR_HEIGHT = 8;
    private static final int MANA_BAR_BORDER = 1;
    private static final int PADDING = 5;

    // Mana bar colors
    private static final int MANA_BAR_BORDER_COLOR = 0xFF000080; // Dark blue
    private static final int MANA_BAR_BACKGROUND_COLOR = 0x80000040; // Semi-transparent dark blue
    private static final int MANA_BAR_FILL_COLOR = 0xFF0080FF; // Light blue

    // Low mana warning effect
    private static final int LOW_MANA_FILL_COLOR = 0xFFFF3030; // Red
    private static final float LOW_MANA_THRESHOLD = 0.25f; // 25% or less

    // Spell icon dimensions and position
    private static final int SPELL_ICON_SIZE = 24;
    private static final int SPELL_ICON_PADDING = 5;

    @Override
    public void onHudRender(MatrixStack matrixStack, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null || client.options.hudHidden) {
            return;
        }

        // Check if player has magical items and should display the mana bar
        boolean hasMagicalItems = playerHasMagicalItems(player) || isPlayerHoldingMagicalItem(player);

        // Only render mana bar if player has magical items
        if (hasMagicalItems) {
            renderManaBar(matrixStack, client, player);
        } else {
            // When player has no magical items, reset client mana data or mark it as inactive
            ClientManaData.resetOrMarkInactive();
        }

        // Render active spell icon if player is holding a staff
        ItemStack mainHandStack = player.getMainHandStack();
        ItemStack offHandStack = player.getOffHandStack();

        if (mainHandStack.getItem() instanceof LivingStaffItem) {
            renderActiveSpellIcon(matrixStack, client, mainHandStack);
        } else if (offHandStack.getItem() instanceof LivingStaffItem) {
            renderActiveSpellIcon(matrixStack, client, offHandStack);
        }

        // Get the item the player is holding
        Item mainHandItem = mainHandStack.getItem();
        Item offHandItem = offHandStack.getItem();

        // Check if either hand has a SpellEssenceItem
        Item spellItem = null;
        if (mainHandItem instanceof SpellEssenceItem) {
            spellItem = mainHandItem;
        } else if (offHandItem instanceof SpellEssenceItem) {
            spellItem = offHandItem;
        }

        // If the player is holding a spell essence, show the cooldown if applicable
        if (spellItem != null && player.getItemCooldownManager().isCoolingDown(spellItem)) {
            renderSpellCooldown(matrixStack, client, player, spellItem);
        }
    }

    /**
     * Render the mana bar in the bottom left corner
     */
    private void renderManaBar(MatrixStack matrixStack, MinecraftClient client, PlayerEntity player) {
        // Get player's mana - first try from ClientManaData for client-side
        float manaPercent = ClientManaData.getManaPercentage();

        // Calculate positions
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int x = PADDING;
        int y = screenHeight - PADDING - MANA_BAR_HEIGHT;

        // Draw mana bar background and border
        DrawableHelper.fill(
                matrixStack,
                x - MANA_BAR_BORDER,
                y - MANA_BAR_BORDER,
                x + MANA_BAR_WIDTH + MANA_BAR_BORDER,
                y + MANA_BAR_HEIGHT + MANA_BAR_BORDER,
                MANA_BAR_BORDER_COLOR);

        DrawableHelper.fill(
                matrixStack,
                x,
                y,
                x + MANA_BAR_WIDTH,
                y + MANA_BAR_HEIGHT,
                MANA_BAR_BACKGROUND_COLOR);

        // Draw filled portion of mana bar (change color when low)
        int fillWidth = (int)(MANA_BAR_WIDTH * manaPercent);
        if (fillWidth > 0) {
            // Use red color for low mana instead of blue
            int fillColor = manaPercent <= LOW_MANA_THRESHOLD ? LOW_MANA_FILL_COLOR : MANA_BAR_FILL_COLOR;

            DrawableHelper.fill(
                    matrixStack,
                    x,
                    y,
                    x + fillWidth,
                    y + MANA_BAR_HEIGHT,
                    fillColor);
        }


    }

    /**
     * Renders the active spell icon on the HUD
     */
    private void renderActiveSpellIcon(MatrixStack matrixStack, MinecraftClient client, ItemStack staffStack) {
        NbtCompound nbt = staffStack.getNbt();
        if (nbt == null) return;

        int activeSlot = nbt.getInt(LivingStaffItem.ACTIVE_SLOT_KEY);
        String slotKey = LivingStaffItem.SLOT_PREFIX + activeSlot;

        // Check if the active slot has a spell
        if (!nbt.contains(slotKey) || nbt.getString(slotKey).isEmpty()) {
            return;
        }

        // Get the ID of the spell essence in this slot
        String spellEssenceId = nbt.getString(slotKey);
        Spell spell = SpellRegistry.getSpellFromEssenceId(spellEssenceId);

        if (spell == null) return;

        // Get the spell essence item to display its texture
        Identifier essenceId = new Identifier(spellEssenceId);
        Item essenceItem = Registry.ITEM.get(essenceId);

        if (essenceItem == null) return;

        // Position the spell icon in the bottom right corner
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = screenWidth - SPELL_ICON_SIZE - SPELL_ICON_PADDING;
        int y = screenHeight - SPELL_ICON_SIZE - SPELL_ICON_PADDING;

        // Render spell essence icon
        client.getItemRenderer().renderInGuiWithOverrides(new ItemStack(essenceItem), x, y);

        // Add a visual indicator for the active spell slot (1, 2, or 3)
        TextRenderer textRenderer = client.textRenderer;
        String slotNumberText = String.valueOf(activeSlot + 1);
        int slotIndicatorX = x + SPELL_ICON_SIZE - textRenderer.getWidth(slotNumberText) - 2;
        int slotIndicatorY = y + SPELL_ICON_SIZE - textRenderer.fontHeight - 1;

        // Draw a small background for better visibility
        DrawableHelper.fill(
                matrixStack,
                slotIndicatorX - 1,
                slotIndicatorY - 1,
                slotIndicatorX + textRenderer.getWidth(slotNumberText) + 1,
                slotIndicatorY + textRenderer.fontHeight,
                0x80000000 // Semi-transparent black
        );

        // Draw the slot number
        textRenderer.drawWithShadow(
                matrixStack,
                slotNumberText,
                slotIndicatorX,
                slotIndicatorY,
                0xFFFFFF // White
        );

        // Render spell name
        String spellName = spell.getType().substring(0, 1).toUpperCase() + spell.getType().substring(1);
        int textWidth = textRenderer.getWidth(spellName);
        textRenderer.drawWithShadow(matrixStack, spellName, x + (SPELL_ICON_SIZE - textWidth) / 2,
                y - textRenderer.fontHeight - 2, 0xFFFFFF);
    }

    /**
     * Renders spell cooldown on the HUD
     */
    private void renderSpellCooldown(MatrixStack matrixStack, MinecraftClient client, PlayerEntity player, Item spellItem) {
        // Get remaining cooldown percentage
        float cooldownPercent = player.getItemCooldownManager().getCooldownProgress(spellItem, 0f);
        int remainingTicks = Math.round(cooldownPercent * 20);

        // Only show cooldown if it's actually cooling down
        if (cooldownPercent > 0f) {
            // Prepare the text to display
            Text cooldownText = Text.literal("Spell Cooldown: " + remainingTicks)
                    .formatted(Formatting.RED);

            // Calculate position (centered horizontally, near the crosshair)
            TextRenderer textRenderer = client.textRenderer;
            int textWidth = textRenderer.getWidth(cooldownText);
            int x = (client.getWindow().getScaledWidth() - textWidth) / 2;
            int y = client.getWindow().getScaledHeight() / 2 + 20;

            // Draw the text
            textRenderer.drawWithShadow(matrixStack, cooldownText, x, y, 0xFFFFFF);
        }
    }

    /**
     * Check if the player is holding a magical item
     */
    private boolean isPlayerHoldingMagicalItem(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof SpellEssenceItem ||
                player.getOffHandStack().getItem() instanceof SpellEssenceItem ||
                player.getMainHandStack().getItem() instanceof LivingStaffItem ||
                player.getOffHandStack().getItem() instanceof LivingStaffItem;
    }

    /**
     * Check if the player has any magical items in their inventory or hotbar
     */
    private boolean playerHasMagicalItems(PlayerEntity player) {
        // Check all slots in inventory
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() instanceof SpellEssenceItem ||
                    stack.getItem() instanceof LivingStaffItem) {
                return true;
            }
        }

        // Check offhand slot
        if (player.getOffHandStack().getItem() instanceof SpellEssenceItem ||
                player.getOffHandStack().getItem() instanceof LivingStaffItem) {
            return true;
        }

        return false;
    }

    public static void register() {
        HudRenderCallback.EVENT.register(new ManaHudRenderer());
    }
}