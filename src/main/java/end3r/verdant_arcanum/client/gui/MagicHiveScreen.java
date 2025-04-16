package end3r.verdant_arcanum.client.gui;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.screen.MagicHiveScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class MagicHiveScreen extends HandledScreen<MagicHiveScreenHandler> {
    // The texture for the GUI
    private static final Identifier TEXTURE = new Identifier(VerdantArcanum.MOD_ID, "textures/gui/magic_hive.png");

    public MagicHiveScreen(MagicHiveScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        // Adjust the size of the GUI
        this.backgroundWidth = 176;
        this.backgroundHeight = 176;

        // Inventory label position is set but we'll override the drawing of the text
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Draw the main background
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        // Call the parent's render method but override drawForeground to prevent text rendering
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    // Override to prevent drawing the title text
    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        // Do not call super.drawForeground() to avoid rendering any text
        // This overrides the default behavior which would draw the title and inventory text
    }

    @Override
    protected void init() {
        super.init();
        // Title position is set but we've overridden drawForeground so it won't be displayed
        titleX = (backgroundWidth - textRenderer.getWidth(title)) / 2;
    }
}