package end3r.verdant_arcanum.magic;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.entity.EquipmentSlot;

public class MaxManaEnchantment extends Enchantment {

    public MaxManaEnchantment() {
        super(Rarity.RARE, EnchantmentTarget.ARMOR_CHEST, new EquipmentSlot[]{EquipmentSlot.CHEST});
    }

    @Override
    public int getMaxLevel() {
        return 5; // Maximum level for this enchantment
    }

    @Override
    public boolean isTreasure() {
        return true; // Makes it a rare enchantment
    }

    @Override
    public boolean isAvailableForEnchantedBookOffer() {
        return true; // Allows it to appear in enchanted books
    }

    @Override
    public boolean isAvailableForRandomSelection() {
        return true; // Allows it to appear in random selection
    }
}