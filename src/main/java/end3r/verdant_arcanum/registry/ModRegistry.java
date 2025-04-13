// ModRegistry.java
package end3r.verdant_arcanum.registry;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

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

                // When you add more items to your mod in the future,
                // add them to the creative tab here
            })
            .build();

    public static void registerAll() {
        // Register all blocks first
        end3r.verdant_arcanum.registry.ModBlocks.register();

        // Then register items (which may depend on blocks)
        end3r.verdant_arcanum.registry.ModItems.register();

        // Register other components if needed
        // ModEntities.register();
        // ModSounds.register();
        // etc.
    }
}