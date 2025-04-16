package end3r.verdant_arcanum.mixin;

import end3r.verdant_arcanum.entity.MagicInfusedBee;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.entity.passive.BeeEntity$PollinateGoal")
public class BeePollinateGoalMixin {
    @Shadow(remap = false)
    private BeeEntity field_20377; // This is the outer BeeEntity instance

    @Inject(method = "getFlower", at = @At("HEAD"), cancellable = true)
    private void findClosestMagicalFlower(CallbackInfoReturnable<BlockPos> cir) {
        // Access the bee entity using the shadowed field
        BeeEntity bee = this.field_20377;

        if (bee instanceof MagicInfusedBee) {
            // Your custom logic here
            MagicInfusedBee magicBee = (MagicInfusedBee) bee;
            BlockPos flowerPos = magicBee.findNearestMagicalFlower(); // Assuming you add this method

            if (flowerPos != null) {
                cir.setReturnValue(flowerPos);
            }
        }
    }

    @Inject(method = "shouldRunEveryTick", at = @At("HEAD"), cancellable = true)
    private void checkMagicalFlowerPollination(CallbackInfoReturnable<Boolean> cir) {
        BeeEntity bee = this.field_20377;

        if (bee instanceof MagicInfusedBee) {
            cir.setReturnValue(true);
        }
    }
}