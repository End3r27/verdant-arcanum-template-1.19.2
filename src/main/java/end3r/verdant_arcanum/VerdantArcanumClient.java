// VerdantArcanumClient.java
package end3r.verdant_arcanum;

import end3r.verdant_arcanum.magic.ManaSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import end3r.verdant_arcanum.item.SpellEssenceItem;

@Environment(EnvType.CLIENT)
public class VerdantArcanumClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerdantArcanum.MOD_ID);

    // Mana bar dimensions
    private static final int MANA_BAR_WIDTH = 100;
    private static final int MANA_BAR_HEIGHT = 8;
    private static final int MANA_BAR_BORDER = 1;
    private static final int PADDING = 5;

    // Mana bar colors
    private static final int MANA_BAR_BORDER_COLOR = 0xFF000080; // Dark blue
    private static final int MANA_BAR_BACKGROUND_COLOR = 0x80000040; // Semi-transparent dark blue
    private static final int MANA_BAR_FILL_COLOR = 0xFF0080FF; // Light blue
    private static final int MANA_TEXT_COLOR = 0xFFFFFFFF; // White

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Verdant Arcanum Client...");

        // Register HUD rendering callback
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            PlayerEntity player = client.player;

            if (player != null) {
                // Render mana bar if player has magical items in inventory or hotbar
                if (playerHasMagicalItems(player) || isPlayerHoldingMagicalItem(player)) {
                    renderManaBar(matrixStack, client, player);
                }

                // Get the item the player is holding
                Item mainHandItem = player.getMainHandStack().getItem();
                Item offHandItem = player.getOffHandStack().getItem();

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
        });

        LOGGER.info("Verdant Arcanum Client initialization complete!");
    }

    /**
     * Check if the player is holding a magical item
     */
    private boolean isPlayerHoldingMagicalItem(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof SpellEssenceItem ||
                player.getOffHandStack().getItem() instanceof SpellEssenceItem;
    }

    /**
     * Check if the player has any magical items in their inventory or hotbar
     */
    private boolean playerHasMagicalItems(PlayerEntity player) {
        // Check all slots in inventory
        for (ItemStack stack : player.getInventory().main) {
            if (stack.getItem() instanceof SpellEssenceItem) {
                return true;
            }
        }

        // Check offhand slot
        if (player.getOffHandStack().getItem() instanceof SpellEssenceItem) {
            return true;
        }

        return false;
    }

    /**
     * Render the mana bar in the bottom left corner
     */
    private void renderManaBar(MatrixStack matrixStack, MinecraftClient client, PlayerEntity player) {
        // Get player's mana
        ManaSystem.ManaSystem.PlayerMana playerMana = ManaSystem.getInstance().getPlayerMana(player);
        float manaPercent = playerMana.getManaPercentage();

        // Calculate positions
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int x = PADDING;
        int y = screenHeight - PADDING - MANA_BAR_HEIGHT;

        // Draw mana bar background and border
        DrawableHelper.fill(matrixStack,
                x - MANA_BAR_BORDER,
                y - MANA_BAR_BORDER,
                x + MANA_BAR_WIDTH + MANA_BAR_BORDER,
                y + MANA_BAR_HEIGHT + MANA_BAR_BORDER,
                MANA_BAR_BORDER_COLOR);

        DrawableHelper.fill(matrixStack,
                x,
                y,
                x + MANA_BAR_WIDTH,
                y + MANA_BAR_HEIGHT,
                MANA_BAR_BACKGROUND_COLOR);

        // Draw filled portion of mana bar
        int fillWidth = (int)(MANA_BAR_WIDTH * manaPercent);
        if (fillWidth > 0) {
            DrawableHelper.fill(matrixStack,
                    x,
                    y,
                    x + fillWidth,
                    y + MANA_BAR_HEIGHT,
                    MANA_BAR_FILL_COLOR);
        }

        // Draw mana text
        TextRenderer textRenderer = client.textRenderer;
        String manaText = String.format("Mana: %.0f/%.0f", playerMana.getCurrentMana(), (float)playerMana.getMaxMana());
        int textWidth = textRenderer.getWidth(manaText);

        // Center text in mana bar
        int textX = x + (MANA_BAR_WIDTH - textWidth) / 2;
        int textY = y + (MANA_BAR_HEIGHT - textRenderer.fontHeight) / 2;

        // Draw with shadow for better visibility
        textRenderer.drawWithShadow(matrixStack, manaText, textX, textY, MANA_TEXT_COLOR);
    }

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
}