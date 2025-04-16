package end3r.verdant_arcanum.mixin;

import end3r.verdant_arcanum.entity.MagicInfusedBee;
import end3r.verdant_arcanum.registry.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeeEntity.class)
public class BeeFlowerEventMixin {
    @Inject(method = "isFlowers", at = @At("HEAD"), cancellable = true)
    private void checkMagicalFlowers(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        BeeEntity bee = (BeeEntity)(Object)this;

        if (bee instanceof MagicInfusedBee) {
            BlockState state = bee.world.getBlockState(pos);

            // Check if it's any of our magical flowers
            if (state.isIn(ModTags.Blocks.MAGIC_FLOWERS_IN_BLOOM)) {
                // Set return value to true so the bee recognizes this as a valid flower
                cir.setReturnValue(true);
            }
        }
    }
}