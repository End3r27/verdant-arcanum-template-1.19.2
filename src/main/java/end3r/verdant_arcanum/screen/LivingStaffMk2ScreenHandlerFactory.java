package end3r.verdant_arcanum.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class LivingStaffMk2ScreenHandlerFactory implements ExtendedScreenHandlerFactory {
    private final ItemStack staffStack;

    public LivingStaffMk2ScreenHandlerFactory(ItemStack staffStack) {
        this.staffStack = staffStack;
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeItemStack(staffStack);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.verdant_arcanum.living_staff_mk2");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LivingStaffMk2ScreenHandler(syncId, inv, staffStack);
    }
}

