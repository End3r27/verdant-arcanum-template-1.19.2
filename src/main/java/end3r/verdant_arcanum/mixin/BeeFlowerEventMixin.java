package end3r.verdant_arcanum.mixin;

import end3r.verdant_arcanum.entity.MagicInfusedBee;
import end3r.verdant_arcanum.registry.ModItems;
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
            MagicInfusedBee magicBee = (MagicInfusedBee)bee;
            BlockState state = bee.world.getBlockState(pos);

            if (state.isIn(ModTags.Blocks.MAGIC_FLOWERS_IN_BLOOM)) {
                // If it's one of our magical flowers, return true
                cir.setReturnValue(true);

                // Track which type of flower when we find one
                if (state.isIn(ModTags.Blocks.FLAME_FLOWERS_IN_BLOOM)) {
                    magicBee.setCurrentPollenType(ModItems.FLAME_FLOWER_BLOOM);
                } else if (state.isIn(ModTags.Blocks.BLINK_FLOWERS_IN_BLOOM)) {
                    magicBee.setCurrentPollenType(ModItems.BLINK_FLOWER_BLOOM);
                } else if (state.isIn(ModTags.Blocks.ROOTGRASP_FLOWERS_IN_BLOOM)) {
                    magicBee.setCurrentPollenType(ModItems.ROOTGRASP_FLOWER_BLOOM);
                } else if (state.isIn(ModTags.Blocks.GUST_FLOWERS_IN_BLOOM)) {
                    magicBee.setCurrentPollenType(ModItems.GUST_FLOWER_BLOOM);
                }
            }
        }
    }
}