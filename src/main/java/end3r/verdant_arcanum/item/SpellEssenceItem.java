
package end3r.verdant_arcanum.item;

import end3r.verdant_arcanum.util.TooltipUtils;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;

public class SpellEssenceItem extends Item {
    private final String essenceType;

    public SpellEssenceItem(String essenceType, Settings settings) {
        super(settings);
        this.essenceType = essenceType;
    }

    public String getEssenceType() {
        return this.essenceType;
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        TooltipUtils.addTooltipWithShift(
                stack, world, tooltip, context,
                // Basic info supplier
                () -> new Text[] {
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence", Formatting.AQUA),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence." + essenceType.toLowerCase(),
                                getFormattingForType(essenceType))
                },
                // Detailed info supplier (shown when shift is pressed)
                () -> new Text[] {
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence.detailed.1", Formatting.GOLD),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence.detailed.2", Formatting.GOLD)
                }
        );
        super.appendTooltip(stack, world, tooltip, context);
    }

    private Formatting getFormattingForType(String type) {
        return switch (type.toLowerCase()) {
            case "flame" -> Formatting.RED;
            case "water" -> Formatting.BLUE;
            case "earth" -> Formatting.DARK_GREEN;
            case "air" -> Formatting.WHITE;
            default -> Formatting.GRAY;
        };
    }
}