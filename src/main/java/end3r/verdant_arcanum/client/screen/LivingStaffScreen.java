package end3r.verdant_arcanum.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.item.LivingStaffItem;
import end3r.verdant_arcanum.screen.LivingStaffScreenHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class LivingStaffScreen extends HandledScreen<LivingStaffScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(VerdantArcanum.MOD_ID, "textures/gui/living_staff.png");

    private int activeSlotHighlightX = 0;

    public LivingStaffScreen(LivingStaffScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Center the title
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(matrices, mouseX, mouseY);

        // Update the position of the active slot indicator
        int activeSlot = handler.getActiveSlot();
        activeSlotHighlightX = 62 + activeSlot * 26;
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

        // Draw active slot highlight
        drawTexture(matrices, x + activeSlotHighlightX - 1, y + 34, 176, 0, 18, 18);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // Delegate scroll handling to the screen handler
        if (this.client.player.isSneaking()) {
            handler.handleScroll(amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}