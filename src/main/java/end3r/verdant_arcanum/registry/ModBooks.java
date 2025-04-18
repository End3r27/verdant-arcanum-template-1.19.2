package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import vazkii.patchouli.api.PatchouliAPI;

public class ModBooks {
    // Update this to match your actual folder structure
    public static final Identifier GROVE_JOURNAL = new Identifier("verdant_arcanum", "grove_journal");

    public static void registerBooks() {
        VerdantArcanum.LOGGER.info("Registering Verdant Arcanum guidebook: " + GROVE_JOURNAL);
    }

    public static ItemStack getGroveJournalStack() {
        try {
            return PatchouliAPI.get().getBookStack(GROVE_JOURNAL);
        } catch (Exception e) {
            VerdantArcanum.LOGGER.error("Failed to get Grove Journal guidebook: " + e.getMessage());
            return ItemStack.EMPTY;
        }
    }
}