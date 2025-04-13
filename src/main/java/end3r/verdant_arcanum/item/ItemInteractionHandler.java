package end3r.verdant_arcanum.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ItemInteractionHandler {

    /**
     * Attempt to graft a spell essence onto a living staff.
     * This should be called from SpellEssenceItem's use method when the player
     * is holding a living staff in their off-hand.
     */
    public static TypedActionResult<ItemStack> tryGraftingOntoStaff(World world, PlayerEntity player, Hand hand) {
        // Get both hands' items
        ItemStack mainHandStack = player.getStackInHand(hand);
        ItemStack offHandStack = player.getStackInHand(hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND);

        // Check if one hand has a spell essence and the other has a living staff
        if (mainHandStack.getItem() instanceof SpellEssenceItem && offHandStack.getItem() instanceof LivingStaffItem) {
            TypedActionResult<ItemStack> result = LivingStaffItem.graftSpellEssence(world, player, offHandStack, mainHandStack);
            if (result.getResult().isAccepted()) {
                // Only return success if the grafting succeeded
                return TypedActionResult.success(mainHandStack);
            }
            return TypedActionResult.fail(mainHandStack);
        }
        else if (offHandStack.getItem() instanceof SpellEssenceItem && mainHandStack.getItem() instanceof LivingStaffItem) {
            TypedActionResult<ItemStack> result = LivingStaffItem.graftSpellEssence(world, player, mainHandStack, offHandStack);
            if (result.getResult().isAccepted()) {
                // Only return success if the grafting succeeded
                return TypedActionResult.success(mainHandStack);
            }
            return TypedActionResult.fail(mainHandStack);
        }

        // No valid combination found
        return TypedActionResult.pass(mainHandStack);
    }
}