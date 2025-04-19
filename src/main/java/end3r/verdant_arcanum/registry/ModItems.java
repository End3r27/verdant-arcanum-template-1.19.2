package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;

import end3r.verdant_arcanum.item.LivingStaffItem;
import end3r.verdant_arcanum.item.LivingStaffMk2Item;
import end3r.verdant_arcanum.item.SpellEssenceItem;
import end3r.verdant_arcanum.magic.ManaRegenEnchantment;
import end3r.verdant_arcanum.magic.MaxManaEnchantment;
import end3r.verdant_arcanum.util.TooltipUtils;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.List;

public class ModItems {
    // First we'll use the default Minecraft item group temporarily
    private static final ItemGroup DEFAULT_GROUP = ModRegistry.VERDANT_GROUP;

    // Block items
    public static final Item GROVE_SOIL = new BlockItem(ModBlocks.GROVE_SOIL, new FabricItemSettings().group(DEFAULT_GROUP));

    // Seeds for magical flowers
    public static final Item FLAME_FLOWER_SEEDS = new FlameFlowerSeedsItem(ModBlocks.FLAME_FLOWER, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item BLINK_FLOWER_SEEDS = new BlinkFlowerSeedsItem(ModBlocks.BLINK_FLOWER, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item ROOTGRASP_FLOWER_SEEDS = new RootgraspFlowerSeedsItem(ModBlocks.ROOTGRASP_FLOWER, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item GUST_FLOWER_SEEDS = new GustFlowerSeedsItem(ModBlocks.GUST_FLOWER, new FabricItemSettings().group(DEFAULT_GROUP));

    // Harvested magical flower blooms
    public static final Item FLAME_FLOWER_BLOOM = new FlameFlowerBloomItem(ModBlocks.FLAME_BLOOM, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item BLINK_FLOWER_BLOOM = new BlinkFlowerBloomItem(ModBlocks.BLINK_BLOOM, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item ROOTGRASP_FLOWER_BLOOM = new RootgraspFlowerBloomItem(ModBlocks.ROOTGRASP_BLOOM, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item GUST_FLOWER_BLOOM = new GustFlowerBloomItem(ModBlocks.GUST_BLOOM, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item BREEZEVINE_FLOWER_BLOOM = new BreezevineFlowerBloomItem(ModBlocks.BREEZEVINE_BLOOM, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item SOLARBLOOM_FLOWER_BLOOM = new SolarbloomFlowerBloomItem(ModBlocks.SOLARBLOOM_BLOOM, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item FLAMESPIRAL_FLOWER_BLOOM = new FlamespiralFlowerBloomItem(ModBlocks.FLAMESPIRAL_BLOOM, new FabricItemSettings().group(DEFAULT_GROUP));
    public static final Item PHANTOMSTEP_FLOWER_BLOOM = new PhantomstepFlowerBloomItem(ModBlocks.PHANTOMSTEP_BLOOM, new FabricItemSettings().group(DEFAULT_GROUP));


    // Spell essences
    public static final Item SPELL_ESSENCE_FLAME = new SpellEssenceItem("flame", new FabricItemSettings().group(DEFAULT_GROUP).fireproof().maxCount(16));
    public static final Item SPELL_ESSENCE_BLINK = new SpellEssenceItem("blink", new Item.Settings().group(DEFAULT_GROUP).maxCount(16));
    public static final Item SPELL_ESSENCE_ROOTGRASP = new SpellEssenceItem("rootgrasp", new Item.Settings().group(DEFAULT_GROUP).maxCount(16));
    public static final Item SPELL_ESSENCE_GUST = new SpellEssenceItem("gust", new Item.Settings().group(DEFAULT_GROUP).maxCount(16));
    public static final Item SPELL_ESSENCE_BREEZEVINE = new SpellEssenceItem("breezevine", new Item.Settings().group(DEFAULT_GROUP).maxCount(16));
    public static final Item SPELL_ESSENCE_SOLARBLOOM = new SpellEssenceItem("solarbloom", new Item.Settings().group(DEFAULT_GROUP).fireproof().maxCount(4));
    public static final Item SPELL_ESSENCE_FLAMESPIRAL = new SpellEssenceItem("flamespiral", new FabricItemSettings().group(DEFAULT_GROUP).fireproof().maxCount(4));
    public static final Item SPELL_ESSENCE_PHANTOMSTEP = new SpellEssenceItem("phantomstep", new FabricItemSettings().group(DEFAULT_GROUP).maxCount(4));


    public static final MaxManaEnchantment MAX_MANA_ENCHANTMENT = new MaxManaEnchantment();
    public static final ManaRegenEnchantment MANA_REGEN_ENCHANTMENT = new ManaRegenEnchantment();


    public static final Item MAGIC_HIVE = new BlockItem(ModBlocks.MAGIC_HIVE, new FabricItemSettings().group(DEFAULT_GROUP));

    public static final Item MAGIC_BEE_SPAWNER = new BlockItem(ModBlocks.MAGIC_BEE_SPAWNER, new FabricItemSettings().group(DEFAULT_GROUP));

    // Grove Journal book
    public static final Item GROVE_JOURNAL = new Item(new FabricItemSettings().group(DEFAULT_GROUP).maxCount(1)) {
        @Override
        public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
            if (world.isClient) {
                PatchouliAPI.get().openBookGUI(ModBooks.GROVE_JOURNAL);
            }
            return TypedActionResult.success(user.getStackInHand(hand));
        }
    };


    // Magic Infused Bee Spawn Egg (Purple and Yellow)
    public static final Item MAGIC_INFUSED_BEE_SPAWN_EGG = new SpawnEggItem(
            end3r.verdant_arcanum.registry.ModEntities.MAGIC_INFUSED_BEE,
            0x9B30FF, // Purple primary color
            0xFFD700, // Yellow secondary color
            new FabricItemSettings().group(DEFAULT_GROUP)
    );

    public static final Item LIVING_STAFF = new LivingStaffItem(new FabricItemSettings().group(DEFAULT_GROUP).maxCount(1));

    public static final Item LIVING_STAFF_MK2 = new LivingStaffMk2Item(
            new FabricItemSettings().group(DEFAULT_GROUP).maxCount(1).fireproof());

    // This method will be called from ModRegistry
    public static void register() {
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "grove_soil"), GROVE_SOIL);

        // Register seeds
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "flame_flower_seeds"), FLAME_FLOWER_SEEDS);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "blink_flower_seeds"), BLINK_FLOWER_SEEDS);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "rootgrasp_flower_seeds"), ROOTGRASP_FLOWER_SEEDS);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "gust_flower_seeds"), GUST_FLOWER_SEEDS);

        // Register blooms
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "flame_flower_bloom"), FLAME_FLOWER_BLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "blink_flower_bloom"), BLINK_FLOWER_BLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "rootgrasp_flower_bloom"), ROOTGRASP_FLOWER_BLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "gust_flower_bloom"), GUST_FLOWER_BLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "breezevine_flower_bloom"), BREEZEVINE_FLOWER_BLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "solarbloom_flower_bloom"), SOLARBLOOM_FLOWER_BLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "flamespiral_flower_bloom"), FLAMESPIRAL_FLOWER_BLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "phantomstep_flower_bloom"), PHANTOMSTEP_FLOWER_BLOOM);


        // Register spell essences
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_blink"), SPELL_ESSENCE_BLINK);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_rootgrasp"), SPELL_ESSENCE_ROOTGRASP);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_gust"), SPELL_ESSENCE_GUST);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_flame"), SPELL_ESSENCE_FLAME);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_breezevine"), SPELL_ESSENCE_BREEZEVINE);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_solarbloom"), SPELL_ESSENCE_SOLARBLOOM);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_flamespiral"), SPELL_ESSENCE_FLAMESPIRAL);
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "spell_essence_phantomstep"), SPELL_ESSENCE_PHANTOMSTEP);


        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "magic_hive"), MAGIC_HIVE);

        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "magic_bee_spawner"), MAGIC_BEE_SPAWNER);

        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "grove_journal"), GROVE_JOURNAL);

        Registry.register(Registry.ENCHANTMENT, new Identifier("verdant_arcanum", "max_mana"), MAX_MANA_ENCHANTMENT);
        Registry.register(Registry.ENCHANTMENT, new Identifier("verdant_arcanum", "mana_regen"), MANA_REGEN_ENCHANTMENT);



        // Register the Magic Infused Bee Spawn Egg
        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "magic_infused_bee_spawn_egg"), MAGIC_INFUSED_BEE_SPAWN_EGG);

        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "living_staff"), LIVING_STAFF);

        Registry.register(Registry.ITEM, new Identifier(VerdantArcanum.MOD_ID, "living_staff_mk2"), LIVING_STAFF_MK2);

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

    // Custom item class for Blink Flower Seeds with tooltip
    public static class BlinkFlowerSeedsItem extends AliasedBlockItem {
        public BlinkFlowerSeedsItem(net.minecraft.block.Block block, Settings settings) {
            super(block, settings);
        }

        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.blink_flower_seeds", Formatting.AQUA)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.blink_flower_seeds.detailed.1", Formatting.BLUE),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.blink_flower_seeds.detailed.2", Formatting.BLUE),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.blink_flower_seeds.detailed.3", Formatting.DARK_PURPLE)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }

    // Custom item class for Rootgrasp Flower Seeds with tooltip
    public static class RootgraspFlowerSeedsItem extends AliasedBlockItem {
        public RootgraspFlowerSeedsItem(net.minecraft.block.Block block, Settings settings) {
            super(block, settings);
        }

        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.rootgrasp_flower_seeds", Formatting.DARK_GREEN)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.rootgrasp_flower_seeds.detailed.1", Formatting.GREEN),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.rootgrasp_flower_seeds.detailed.2", Formatting.GREEN),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.rootgrasp_flower_seeds.detailed.3", Formatting.DARK_GREEN)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }

    // Custom item class for Gust Flower Seeds with tooltip
    public static class GustFlowerSeedsItem extends AliasedBlockItem {
        public GustFlowerSeedsItem(net.minecraft.block.Block block, Settings settings) {
            super(block, settings);
        }

        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.gust_flower_seeds", Formatting.WHITE)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.gust_flower_seeds.detailed.1", Formatting.GRAY),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.gust_flower_seeds.detailed.2", Formatting.GRAY),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.gust_flower_seeds.detailed.3", Formatting.WHITE)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }

    // Custom item class for Flame Flower Bloom with tooltip
    public static class FlameFlowerBloomItem extends BlockItem {
        public FlameFlowerBloomItem(Block flameBloom, Settings settings) {
            super(flameBloom, settings);
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

    // Custom item class for Blink Flower Bloom with tooltip
    public static class BlinkFlowerBloomItem extends BlockItem {
        public BlinkFlowerBloomItem(Block blinkBloom, Settings settings) {
            super(blinkBloom, settings);
        }

        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.blink_flower_bloom", Formatting.AQUA)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.blink_flower_bloom.detailed.1", Formatting.BLUE),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.blink_flower_bloom.detailed.2", Formatting.DARK_PURPLE, Formatting.ITALIC)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }

    // Custom item class for Rootgrasp Flower Bloom with tooltip
    public static class RootgraspFlowerBloomItem extends BlockItem {
        public RootgraspFlowerBloomItem(Block rootgraspBloom, Settings settings) {
            super(rootgraspBloom, settings);
        }

        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.rootgrasp_flower_bloom", Formatting.DARK_GREEN)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.rootgrasp_flower_bloom.detailed.1", Formatting.GREEN),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.rootgrasp_flower_bloom.detailed.2", Formatting.DARK_GREEN, Formatting.ITALIC)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }

    // Custom item class for Gust Flower Bloom with tooltip
    public static class GustFlowerBloomItem extends BlockItem {
        public GustFlowerBloomItem(Block gustBloom, Settings settings) {
            super(gustBloom, settings);
        }




        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.gust_flower_bloom", Formatting.WHITE)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.gust_flower_bloom.detailed.1", Formatting.GRAY),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.gust_flower_bloom.detailed.2", Formatting.WHITE, Formatting.ITALIC)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }
    public static class BreezevineFlowerBloomItem extends BlockItem {
        public BreezevineFlowerBloomItem(Block gustBloom, Settings settings) {
            super(gustBloom, settings);
        }




        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.breezevine_flower_bloom", Formatting.WHITE)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.breezevine_flower_bloom.detailed.1", Formatting.GRAY),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.breezevine_flower_bloom.detailed.2", Formatting.WHITE, Formatting.ITALIC)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }
    public static class SolarbloomFlowerBloomItem extends BlockItem {
        public SolarbloomFlowerBloomItem(Block gustBloom, Settings settings) {
            super(gustBloom, settings);
        }




        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.solarbloom_flower_bloom", Formatting.WHITE)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.solarbloom_flower_bloom.detailed.1", Formatting.GRAY),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.solarbloom_flower_bloom.detailed.2", Formatting.WHITE, Formatting.ITALIC)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }
    public static class FlamespiralFlowerBloomItem extends BlockItem {
        public FlamespiralFlowerBloomItem(Block gustBloom, Settings settings) {
            super(gustBloom, settings);
        }




        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flamespiral_flower_bloom", Formatting.WHITE)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flamespiral_flower_bloom.detailed.1", Formatting.GRAY),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.flamespiral_flower_bloom.detailed.2", Formatting.WHITE, Formatting.ITALIC)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }
    public static class PhantomstepFlowerBloomItem extends BlockItem {
        public PhantomstepFlowerBloomItem(Block gustBloom, Settings settings) {
            super(gustBloom, settings);
        }




        @Override
        public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
            TooltipUtils.addTooltipWithShift(
                    stack, world, tooltip, context,
                    // Basic info supplier
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.phantomstep_flower_bloom", Formatting.WHITE)
                    },
                    // Detailed info supplier (shown when shift is pressed)
                    () -> new Text[] {
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.phantomstep_flower_bloom.detailed.1", Formatting.GRAY),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.phantomstep_flower_bloom.detailed.2", Formatting.WHITE, Formatting.ITALIC)
                    }
            );
            super.appendTooltip(stack, world, tooltip, context);
        }
    }
}