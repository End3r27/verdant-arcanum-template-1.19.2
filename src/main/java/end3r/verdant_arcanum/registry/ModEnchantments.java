package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.magic.MaxManaEnchantment;
import end3r.verdant_arcanum.magic.ManaRegenEnchantment;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.util.Identifier;

public class ModEnchantments {
    public static final Enchantment MAX_MANA = new MaxManaEnchantment();
    public static final Enchantment MANA_REGEN = new ManaRegenEnchantment();

    public static void registerEnchantments() {
        net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.ENCHANTMENT, new Identifier("verdant_arcanum", "max_mana"), MAX_MANA);
        net.minecraft.util.registry.Registry.register(net.minecraft.util.registry.Registry.ENCHANTMENT, new Identifier("verdant_arcanum", "mana_regen"), MANA_REGEN);
    }
}