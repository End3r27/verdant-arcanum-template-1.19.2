// GroveSoilBlock.java
package end3r.verdant_arcanum.block;

import end3r.verdant_arcanum.util.TooltipUtils;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.BlockView;

import java.util.List;

public class GroveSoilBlock extends Block {
    public GroveSoilBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
        TooltipUtils.addTooltipWithShift(
                stack, null, tooltip, options,
                // Basic info supplier
                () -> new Text[] {
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.grove_soil", Formatting.GREEN)
                },
                // Detailed info supplier (shown when shift is pressed)
                () -> new Text[] {
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.grove_soil.detailed.1", Formatting.AQUA),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.grove_soil.detailed.2", Formatting.GOLD)
                }
        );
        super.appendTooltip(stack, world, tooltip, options);
    }
}