// Updated src/main/java/end3r/verdant_arcanum/registry/ModItems.java
package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.item.SpellEssenceItem;
import end3r.verdant_arcanum.util.TooltipUtils;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.AliasedBlockItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import java.util.List;

public class ModItems {
    // First we'll use the default Minecraft item group temporarily
    private static final ItemGroup DEFAULT_GROUP = ItemGroup.MISC;

    // Block items
    public static final Item GROVE_SOIL = new BlockItem(ModBlocks.GROVE_SOIL,
            new FabricItemSettings().group(DEFAULT_GROUP));

    // Seeds for the flame flower
    public static final Item FLAME_FLOWER_SEEDS = new FlameFlowerSeedsItem(
            ModBlocks.FLAME_FLOWER,
            new FabricItemSettings().group(DEFAULT_GROUP));

    // Harvested flame flower
    public static final Item FLAME_FLOWER_BLOOM = new FlameFlowerBloomItem(
            new FabricItemSettings().group(DEFAULT_GROUP));

    // Spell essences
    public static final Item SPELL_ESSENCE_FLAME = new SpellEssenceItem(
            "Flame",
            new FabricItemSettings().group(DEFAULT_GROUP).fireproof().maxCount(16));

    // This method will be called from ModRegistry
    public static void register() {
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "grove_soil"), GROVE_SOIL);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "flame_flower_seeds"), FLAME_FLOWER_SEEDS);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "flame_flower_bloom"), FLAME_FLOWER_BLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_flame"), SPELL_ESSENCE_FLAME);
    }

    // Custom item class for Flame Flower Seeds with tooltip
    public static class FlameFlowerSeedsItem extends AliasedBlockItem {
        public FlameFlowerSeedsItem(net.minecraft.block.Block block, Settings settings) {
            super(block, settings);
        }

        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flame_flower_seeds", Formatting.GOLD)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flame_flower_seeds.detailed.1", Formatting.YELLOW),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flame_flower_seeds.detailed.2", Formatting.YELLOW),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flame_flower_seeds.detailed.3", Formatting.RED)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }

    // Custom item class for Flame Flower Bloom with tooltip
    public static class FlameFlowerBloomItem extends Item {
        public FlameFlowerBloomItem(Settings settings) {
            super(settings);
        }

        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flame_flower_bloom", Formatting.GOLD)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flame_flower_bloom.detailed.1", Formatting.YELLOW),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flame_flower_bloom.detailed.2", Formatting.RED, Formatting.ITALIC)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }
}