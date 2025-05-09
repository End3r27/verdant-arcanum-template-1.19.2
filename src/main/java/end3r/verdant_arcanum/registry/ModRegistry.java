package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.magic.ManaEventHandler;
import end3r.verdant_arcanum.magic.ManaSyncPacket;
import end3r.verdant_arcanum.spell.tier2.SolarBloomSpell;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import static end3r.verdant_arcanum.registry.ModItems.MANA_REGEN_ENCHANTMENT;
import static end3r.verdant_arcanum.registry.ModItems.MAX_MANA_ENCHANTMENT;


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

                stacks.add(new ItemStack(ModItems.BREEZEVINE_FLOWER_BLOOM));
                stacks.add(new ItemStack(end3r.verdant_arcanum.registry.ModItems.SPELL_ESSENCE_BREEZEVINE));

                stacks.add(new ItemStack(ModItems.SOLARBLOOM_FLOWER_BLOOM));
                stacks.add(new ItemStack(ModItems.SPELL_ESSENCE_SOLARBLOOM));

                stacks.add(new ItemStack(ModItems.FLAMESPIRAL_FLOWER_BLOOM));
                stacks.add(new ItemStack(ModItems.SPELL_ESSENCE_FLAMESPIRAL));

                stacks.add(new ItemStack(ModItems.PHANTOMSTEP_FLOWER_BLOOM));
                stacks.add(new ItemStack(ModItems.SPELL_ESSENCE_PHANTOMSTEP));


                // Add magical tools
                stacks.add(new ItemStack(ModItems.LIVING_STAFF));
                stacks.add(new ItemStack(ModItems.LIVING_STAFF_MK2));

                stacks.add(new ItemStack(ModItems.MAGIC_INFUSED_BEE_SPAWN_EGG));
                stacks.add(new ItemStack(ModBlocks.MAGIC_BEE_SPAWNER));
                stacks.add(new ItemStack(ModItems.MAGIC_HIVE));

                // Add the Grove Journal
                ItemStack groveJournal = ModBooks.getGroveJournalStack();
                if (!groveJournal.isEmpty()) {
                    stacks.add(groveJournal);
                } else {
                    stacks.add(new ItemStack(ModItems.GROVE_JOURNAL));
                }



            })
            .build();

    public static void registerModTags() {
        // This method ensures that the mod loads the tag files properly
        // You don't actually need to put code here - simply calling this method
        // during initialization will force the class to load, which registers the tags
        VerdantArcanum.LOGGER.info("Registering mod tags");
    }

    public static void registerAll() {
        // Register all blocks first
        end3r.verdant_arcanum.registry.ModBlocks.register();

        // Then register items (which may depend on blocks)
        end3r.verdant_arcanum.registry.ModItems.register();

        ModEntities.registerModEntities();

        ModBlockEntities.register();
        ModScreenHandlers.register();


        registerModTags();

        ModBooks.registerBooks();

        ManaEventHandler.initialize(MAX_MANA_ENCHANTMENT, MANA_REGEN_ENCHANTMENT);

        ManaSyncPacket.registerServer();

        SolarBloomSpell.registerSounds();

        EventRegistry.registerAll();

        ModSoundRegistry.registerSounds();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            CommandRegistry.register(dispatcher);
        });







    }

}