package end3r.verdant_arcanum.mixin;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.BeeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BeeEntity.class)
public abstract class BeeEntityMixin {
    // Shadow the BEE_FLAGS tracked data field
    @Shadow
    private static TrackedData<Byte> BEE_FLAGS;

    // Shadow the HAS_NECTAR_FLAG constant
    @Shadow
    private static final int HAS_NECTAR_FLAG = 8;

    // Shadow the necessary method to interact with flags
    @Shadow
    protected abstract boolean getBeeFlag(int flag);

    @Shadow
    protected abstract void setBeeFlag(int flag, boolean value);

    // Getter implementation
    public boolean getHasNectar() {
        return this.getBeeFlag(HAS_NECTAR_FLAG);
    }

    // Setter implementation
    public void setHasNectar(boolean value) {
        this.setBeeFlag(HAS_NECTAR_FLAG, value);
    }
}