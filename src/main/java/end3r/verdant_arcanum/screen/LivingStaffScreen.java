package end3r.verdant_arcanum.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import end3r.verdant_arcanum.VerdantArcanum;
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
import org.lwjgl.glfw.GLFW;

public class LivingStaffScreen extends HandledScreen<LivingStaffScreenHandler> {
    // Texture for the GUI
    private static final Identifier TEXTURE = new Identifier(VerdantArcanum.MOD_ID, "textures/gui/living_staff.png");

    // Store the last Y position of the mouse to detect scroll direction
    private double lastMouseY = 0;

    public LivingStaffScreen(LivingStaffScreenHandler handler, PlayerInventory inventory, Text title) {
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
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Draw the background
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);

        // Highlight the active slot
        int activeSlot = handler.getActiveSlot();
        if (activeSlot >= 0 && activeSlot < 3) {
            int slotX = x + 62 + activeSlot * 26;
            int slotY = y + 35;

            // Draw the highlight rectangle
            drawTexture(matrices, slotX - 1, slotY - 1, 176, 0, 18, 18);
        }
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
        for (int i = 0; i < 3; i++) {
            int slotX = x + 62 + i * 26;
            int slotY = y + 35;

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
}