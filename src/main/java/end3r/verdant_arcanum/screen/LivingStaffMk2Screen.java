package end3r.verdant_arcanum.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.item.LivingStaffMk2Item;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

public class LivingStaffMk2Screen extends HandledScreen<LivingStaffMk2ScreenHandler> {
    // Texture for the GUI - we'll need a new texture for the Mk2 version
    private static final Identifier TEXTURE = new Identifier(VerdantArcanum.MOD_ID, "textures/gui/living_staff_mk2.png");

    // Store the last Y position of the mouse to detect scroll direction
    private double lastMouseY = 0;

    public LivingStaffMk2Screen(LivingStaffMk2ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // Set background height and adjust Y position of title
        this.backgroundHeight = 166;
        this.backgroundWidth = 176;
        this.titleY = 6;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Save initial mouse Y position
        lastMouseY = MinecraftClient.getInstance().mouse.getY();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Draw the background
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);

        // Highlight the active slot
        int activeSlot = handler.getActiveSlot();
        if (activeSlot >= 0 && activeSlot < LivingStaffMk2Item.MAX_SLOTS) {
            int slotX = x + getSlotX(activeSlot);
            int slotY = y + getSlotY();

            // Draw the highlight rectangle
            drawTexture(matrices, slotX - 1, slotY - 1, 176, 0, 18, 18);
        }
    }

    // Helper method to calculate slot X position for 5 slots
    private int getSlotX(int slot) {
        // Distribute 5 slots evenly across the width
        return 44 + (slot * 18); // Slot position with proper spacing
    }

    // Helper method to get slot Y position
    private int getSlotY() {
        // Match the Y position set in the screen handler
        return 20;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Handle scrolling if player is sneaking
        if (client != null && client.player != null && client.player.isSneaking()) {
            handler.handleScroll(amount);
            // Play a click sound for feedback
            client.getSoundManager().play(
                    PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F + client.world.random.nextFloat() * 0.2F)
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Get slot positions
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;

        // Check if clicked on a spell slot (for direct selection)
        for (int i = 0; i < LivingStaffMk2Item.MAX_SLOTS; i++) {
            int slotX = x + getSlotX(i);
            int slotY = y + getSlotY();

            if (mouseX >= slotX && mouseX < slotX + 16 &&
                    mouseY >= slotY && mouseY < slotY + 16) {

                // If this slot has a spell, make it active
                ItemStack stack = this.handler.getSlot(i).getStack();
                if (!stack.isEmpty()) {
                    this.handler.changeActiveSlot(i);
                    // Play a click sound
                    client.getSoundManager().play(
                            PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F)
                    );
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        // Only draw the title, skip drawing the player inventory text
        this.textRenderer.draw(matrices, this.title, this.titleX, this.titleY, 4210752);
    }
}