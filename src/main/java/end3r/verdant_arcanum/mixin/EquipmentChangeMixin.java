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

    @Inject(method = "equipStack", at = @At("RETURN"))
    private void onEquipStack(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // We don't need to pass the previous stack - we'll do a full recalculation
        // The previously equipped stack is already gone by this point
        EnchantmentEquipHandler.onEquipmentChange(player, slot, ItemStack.EMPTY, stack);
    }
}