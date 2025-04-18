package end3r.verdant_arcanum;

import end3r.verdant_arcanum.client.gui.MagicHiveScreen;
import end3r.verdant_arcanum.entity.client.MagicInfusedBeeRenderer;
import end3r.verdant_arcanum.registry.*;
import end3r.verdant_arcanum.screen.LivingStaffScreen;
import end3r.verdant_arcanum.item.LivingStaffItem;
import end3r.verdant_arcanum.magic.ManaSystem;
import end3r.verdant_arcanum.magic.ManaParticleSystem;
import end3r.verdant_arcanum.screen.LivingStaffScreenHandler;
import end3r.verdant_arcanum.spell.Spell;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWScrollCallbackI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import end3r.verdant_arcanum.item.SpellEssenceItem;

@Environment(EnvType.CLIENT)
public class VerdantArcanumClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerdantArcanum.MOD_ID);

    // Mouse scroll tracking
    private static boolean mouseScrolled = false;
    private static int scrollDirection = 0;

    // Store the original scroll callback
    private static GLFWScrollCallbackI originalScrollCallback;

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
    public void onInitializeClient() {
        LOGGER.info("Initializing Verdant Arcanum Client...");

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLAME_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BLINK_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.ROOTGRASP_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.GUST_BLOOM, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BREEZEVINE_BLOOM, RenderLayer.getCutout());

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLAME_FLOWER, RenderLayer.getCutout());



        HandledScreens.register(ModScreenHandlers.MAGIC_HIVE_SCREEN_HANDLER, MagicHiveScreen::new);

        EntityRendererRegistry.register(ModEntities.MAGIC_INFUSED_BEE, MagicInfusedBeeRenderer::new);


        ClientTickEvents.END_CLIENT_TICK.register(tickClient -> {
            if (tickClient.player != null && tickClient.player.isSneaking() && mouseScrolled) {
                ItemStack mainHandItem = tickClient.player.getMainHandStack();
                ItemStack offHandItem = tickClient.player.getOffHandStack();

                // Only handle if player has a staff
                if (mainHandItem.getItem() instanceof LivingStaffItem ||
                        offHandItem.getItem() instanceof LivingStaffItem) {

                    // Send packet to server instead of handling locally
                    end3r.verdant_arcanum.network.StaffPacketHandler.sendScrollPacket(scrollDirection);
                }

                // Reset after handling (important to prevent repeated scrolling)
                mouseScrolled = false;
            }
        });



        // Register the screen handler type
        LivingStaffScreenHandler.register();

        // Register the screen handler with the correct screen factory
        ScreenRegistry.register(LivingStaffScreenHandler.HANDLER_TYPE, LivingStaffScreen::new);

        // Ensure ManaParticleSystem is initialized
        ManaParticleSystem.getInstance();

        // Get a reference to the MinecraftClient instance
        MinecraftClient client = MinecraftClient.getInstance();

        // Register client started event for mouse scroll setup
        ClientLifecycleEvents.CLIENT_STARTED.register(startedClient -> {
            // Store the original callback
            long window = client.getWindow().getHandle();
            originalScrollCallback = GLFW.glfwSetScrollCallback(window, null);

            // Set our custom callback
            GLFW.glfwSetScrollCallback(window, (windowHandle, xoffset, yoffset) -> {
                // Check if player is sneaking and has a staff
                boolean shouldHandleStaffScroll = false;

                if (client.player != null && client.player.isSneaking()) {
                    ItemStack mainHandItem = client.player.getMainHandStack();
                    ItemStack offHandItem = client.player.getOffHandStack();

                    if (mainHandItem.getItem() instanceof LivingStaffItem ||
                            offHandItem.getItem() instanceof LivingStaffItem) {
                        shouldHandleStaffScroll = true;
                    }
                }

                // Only capture for staff if relevant
                if (shouldHandleStaffScroll) {
                    mouseScrolled = true;
                    scrollDirection = yoffset > 0 ? 1 : -1;
                } else if (originalScrollCallback != null) {
                    // Call the original callback for default behavior
                    originalScrollCallback.invoke(windowHandle, xoffset, yoffset);
                }
            });
        });

        // Register spell scroll handling in client tick
        ClientTickEvents.END_CLIENT_TICK.register(tickClient -> {
            if (tickClient.player != null && tickClient.player.isSneaking() && mouseScrolled) {
                ItemStack mainHandItem = tickClient.player.getMainHandStack();
                ItemStack offHandItem = tickClient.player.getOffHandStack();

                if (mainHandItem.getItem() instanceof LivingStaffItem) {
                    LivingStaffItem.handleScrollWheel(tickClient.world, tickClient.player, mainHandItem, scrollDirection);
                } else if (offHandItem.getItem() instanceof LivingStaffItem) {
                    LivingStaffItem.handleScrollWheel(tickClient.world, tickClient.player, offHandItem, scrollDirection);
                }

                // Reset after handling
                mouseScrolled = false;
            }
        });

        // Register HUD rendering callback
        HudRenderCallback.EVENT.register((matrixStack, tickDelta) -> {
            MinecraftClient hudClient = MinecraftClient.getInstance();
            PlayerEntity player = hudClient.player;

            if (player != null) {
                // Render mana bar if player has magical items in inventory or hotbar
                if (playerHasMagicalItems(player) || isPlayerHoldingMagicalItem(player)) {
                    renderManaBar(matrixStack, hudClient, player);
                }

                // Render active spell icon if player is holding a staff
                ItemStack mainHandStack = player.getMainHandStack();
                ItemStack offHandStack = player.getOffHandStack();

                if (mainHandStack.getItem() instanceof LivingStaffItem) {
                    renderActiveSpellIcon(matrixStack, hudClient, mainHandStack);
                } else if (offHandStack.getItem() instanceof LivingStaffItem) {
                    renderActiveSpellIcon(matrixStack, hudClient, offHandStack);
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
                    renderSpellCooldown(matrixStack, hudClient, player, spellItem);
                }
            }
        });

        // Register client tick event for particle updates
        ClientTickEvents.END_CLIENT_TICK.register(particleClient -> {
            if (particleClient.player != null && particleClient.world != null) {
                // Particles will be handled in ManaSystem's update method
                ManaSystem.getInstance().updateManaRegen(particleClient.player);
            }
        });

        LOGGER.info("Verdant Arcanum Client initialization complete!");
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

    /**
     * Render the mana bar in the bottom left corner
     */
    private void renderManaBar(MatrixStack matrixStack, MinecraftClient client, PlayerEntity player) {
        // Get player's mana
        ManaSystem.PlayerMana playerMana = ManaSystem.getInstance().getPlayerMana(player);
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

        // Draw filled portion of mana bar (change color when low)
        int fillWidth = (int)(MANA_BAR_WIDTH * manaPercent);
        if (fillWidth > 0) {
            // Use red color for low mana instead of blue
            int fillColor = manaPercent <= LOW_MANA_THRESHOLD ? LOW_MANA_FILL_COLOR : MANA_BAR_FILL_COLOR;

            DrawableHelper.fill(matrixStack,
                    x,
                    y,
                    x + fillWidth,
                    y + MANA_BAR_HEIGHT,
                    fillColor);
        }

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