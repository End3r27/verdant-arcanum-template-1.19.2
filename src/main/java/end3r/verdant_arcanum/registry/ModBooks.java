package end3r.verdant_arcanum.registry;

import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import vazkii.patchouli.api.PatchouliAPI;

public class ModBooks {
    // Update this to match your actual folder structure
    public static final Identifier GROVE_JOURNAL = new Identifier("verdant_arcanum", "grove_journal");

    public static void registerBooks() {
        System.out.println("Registering Verdant Arcanum guidebook: " + GROVE_JOURNAL);
    }

}