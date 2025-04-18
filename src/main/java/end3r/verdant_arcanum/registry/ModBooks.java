package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import net.minecraft.item.Item;

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import vazkii.patchouli.api.PatchouliAPI;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.common.item.ItemModBook;


public class ModBooks {
    public static final String MOD_ID = VerdantArcanum.MOD_ID;

    // Register Grove Journal as a Patchouli Book
    public static final Item GROVE_JOURNAL = registerBook("grove_journal",
            new ItemModBook(
                    new Item.Settings().maxCount(1).group(),
                    new Identifier(MOD_ID, "grove_journal")
            )
    );

    /**
     * Register a Patchouli book
     * @param name The registry name for the book item
     * @param bookItem The book item instance
     * @return The registered book item
     */
    private static Item registerBook(String name, Item bookItem) {
        return Registry.register(Registry.REGISTRIES.ITEM, new Identifier(MOD_ID, name), bookItem);
    }

    /**
     * Initialize all books
     * This should be called in your mod's initialization
     */
    public static void registerBooks() {
        VerdantArcanum.LOGGER.info("Registering Verdant Arcanum books");
    }

    /**
     * Check if Patchouli is loaded and initialize any Patchouli-specific functionality
     * Call this after mod initialization
     */
    public static void setupPatchouliBooks() {
        // If we have Patchouli, we can do some additional setup
        if (isPatchouliLoaded()) {
            VerdantArcanum.LOGGER.info("Patchouli detected, setting up books integration");
            // Any additional Patchouli API setup can go here
        }
    }

    /**
     * Helper method to check if Patchouli is loaded
     */
    private static boolean isPatchouliLoaded() {
        try {
            Class.forName("vazkii.patchouli.api.PatchouliAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}