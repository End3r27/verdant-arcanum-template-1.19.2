package end3r.verdant_arcanum.mixin;

import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BeeEntity.class)
public interface BeeEntityAccessor {
    @Accessor("flowerPos")
    BlockPos getFlowerPosition();

    @Accessor("flowerPos")
    void setFlowerPosition(BlockPos pos);
}