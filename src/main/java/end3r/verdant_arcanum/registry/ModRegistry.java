package end3r.verdant_arcanum.registry;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class ModRegistry {
    // Custom item group (creative tab) with all mod items
    public static final ItemGroup VERDANT_GROUP = FabricItemGroupBuilder.create(
                    new Identifier("verdant_arcanum", "main"))
            .icon(() -> new ItemStack(end3r.verdant_arcanum.registry.ModItems.FLAME_FLOWER_BLOOM))
            .appendItems(stacks -> {
                // Add blocks
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModBlocks.GROVE_SOIL));

                // Add plants and related items
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.FLAME_FLOWER_SEEDS));
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.FLAME_FLOWER_BLOOM));

                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.ROOTGRASP_FLOWER_SEEDS));
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.ROOTGRASP_FLOWER_BLOOM));

                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.BLINK_FLOWER_SEEDS));
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.BLINK_FLOWER_BLOOM));

                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.GUST_FLOWER_SEEDS));
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.GUST_FLOWER_BLOOM));

                // Add magical essences
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.SPELL_ESSENCE_FLAME));
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.SPELL_ESSENCE_BLINK));
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.SPELL_ESSENCE_ROOTGRASP));
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.SPELL_ESSENCE_GUST));

                // Add magical tools
                stacks.add(new ItemStack(ModItems.LIVING_STAFF));

                stacks.add(new ItemStack(ModItems.MAGIC_INFUSED_BEE_SPAWN_EGG));
                stacks.add(new ItemStack(ModItems.MAGIC_HIVE));

            })
            .build();

    public static void registerAll() {
        // Register all blocks first
        end3r.verdant_arcanum.registry.ModBlocks.register();

        // Then register items (which may depend on blocks)
        end3r.verdant_arcanum.registry.ModItems.register();

        ModEntities.registerModEntities();

        ModBlockEntities.register();
        ModScreenHandlers.register();



    }
}