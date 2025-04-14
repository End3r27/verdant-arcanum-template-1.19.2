package end3r.verdant_arcanum.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

// Made the factory class public to fix the access issue
public class LivingStaffScreenHandlerFactory implements ExtendedScreenHandlerFactory {
    private final ItemStack staffStack;

    public LivingStaffScreenHandlerFactory(ItemStack staffStack) {
        this.staffStack = staffStack;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeItemStack(staffStack);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.verdant_arcanum.living_staff");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LivingStaffScreenHandler(syncId, inv, staffStack);
    }
}
