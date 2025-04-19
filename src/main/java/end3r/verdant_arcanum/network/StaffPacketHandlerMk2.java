package end3r.verdant_arcanum.network;

import end3r.verdant_arcanum.item.LivingStaffItem;
import end3r.verdant_arcanum.item.LivingStaffMk2Item;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class StaffPacketHandlerMk2 {
    // Proper initialization with a new Identifier
    public static final Identifier STAFF_SCROLL_PACKET_ID = new Identifier("verdant_arcanum", "staff_scroll");

    // Register the packet handler on the server
    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(STAFF_SCROLL_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            // Get the scroll direction from the packet
            int direction = buf.readInt();

            // Execute on the server thread
            server.execute(() -> {
                // Check if player is sneaking (important for security)
                if (player.isSneaking()) {
                    // Find the staff in either hand
                    ItemStack mainHandStack = player.getMainHandStack();
                    ItemStack offHandStack = player.getOffHandStack();

                    // Handle MK2 staff
                    if (mainHandStack.getItem() instanceof LivingStaffMk2Item) {
                        LivingStaffMk2Item.handleScrollWheelMk2(player.world, player, mainHandStack, direction);
                    } else if (offHandStack.getItem() instanceof LivingStaffMk2Item) {
                        LivingStaffMk2Item.handleScrollWheelMk2(player.world, player, offHandStack, direction);
                    }
                    // Handle original staff
                    else if (mainHandStack.getItem() instanceof LivingStaffItem) {
                        LivingStaffItem.handleScrollWheel(player.world, player, mainHandStack, direction);
                    } else if (offHandStack.getItem() instanceof LivingStaffItem) {
                        LivingStaffItem.handleScrollWheel(player.world, player, offHandStack, direction);
                    }
                }
            });
        });
    }

    // Client side method to send a scroll packet (same as original)
    public static void sendScrollPacket(int direction) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(direction);
        ClientPlayNetworking.send(STAFF_SCROLL_PACKET_ID, buf);
    }
}