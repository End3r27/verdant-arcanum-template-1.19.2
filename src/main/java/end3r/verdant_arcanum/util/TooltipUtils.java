// TooltipUtils.java
package end3r.verdant_arcanum.util;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Supplier;

public class TooltipUtils {
    /**
     * Adds a basic tooltip to an item with shift functionality for detailed info
     *
     * @param stack The ItemStack
     * @param world The World
     * @param tooltip The tooltip list to add to
     * @param context The tooltip context
     * @param basicInfo Supplier for basic tooltip info
     * @param detailedInfo Supplier for detailed (shift) tooltip info
     */
    public static void addTooltipWithShift(
            ItemStack stack,
            World world,
            List<Text> tooltip,
            TooltipContext context,
            Supplier<Text[]> basicInfo,
            Supplier<Text[]> detailedInfo) {

        // Add basic tooltip information
        for (Text line : basicInfo.get()) {
            tooltip.add(line);
        }

        // Add shift prompt and detailed info if shift is pressed
        if (Screen.hasShiftDown()) {
            // Add detailed tooltip information
            for (Text line : detailedInfo.get()) {
                tooltip.add(line);
            }
        } else {
            // Prompt to hold shift for more info
            tooltip.add(Text.translatable("tooltip.verdant_arcanum.hold_shift")
                    .formatted(Formatting.GRAY, Formatting.ITALIC));
        }
    }

    /**
     * Creates a formatted tooltip line
     *
     * @param translationKey The translation key
     * @param formatting The formatting to apply
     * @return The formatted Text
     */
// Update in TooltipUtils.java
    public static Text createTooltip(String translationKey, Formatting formatting, Object... args) {
        return Text.translatable(translationKey, args).formatted(formatting);
    }

    // For backward compatibility, keep the original method
    public static Text createTooltip(String translationKey, Formatting... formatting) {
        return Text.translatable(translationKey).formatted(formatting);
    }
}