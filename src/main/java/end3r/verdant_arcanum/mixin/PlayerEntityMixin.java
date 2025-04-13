package end3r.verdant_arcanum.mixin;

import end3r.verdant_arcanum.magic.ManaSystem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo info) {
        // Get the current player instance
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Update mana regeneration
        ManaSystem.getInstance().updateManaRegen(player);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfo info) {
        // Get the current player instance
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Create a compound for our mod data
        NbtCompound modData = new NbtCompound();

        // Save mana data
        ManaSystem.getInstance().savePlayerMana(player, modData);

        // Add our mod data to the player NBT
        nbt.put("VerdantArcanum", modData);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo info) {
        // Get the current player instance
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Check if our mod data exists
        if (nbt.contains("VerdantArcanum")) {
            NbtCompound modData = nbt.getCompound("VerdantArcanum");

            // Load mana data
            ManaSystem.getInstance().loadPlayerMana(player, modData);
        }
    }
}