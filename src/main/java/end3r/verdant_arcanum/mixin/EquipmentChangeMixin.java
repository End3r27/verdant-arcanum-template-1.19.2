package end3r.verdant_arcanum.mixin;

import end3r.verdant_arcanum.magic.EnchantmentEquipHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class EquipmentChangeMixin {

    @Inject(method = "equipStack", at = @At("TAIL"))
    private void onEquipStack(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Get the previous stack in this slot
        ItemStack previousStack = player.getEquippedStack(slot);

        // Handle the equipment change
        EnchantmentEquipHandler.onEquipmentChange(player, slot, previousStack, stack);
    }
}