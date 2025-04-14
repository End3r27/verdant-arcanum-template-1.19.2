package end3r.verdant_arcanum.network;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.item.LivingStaffItem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class StaffPacketHandler {
    public static final Identifier STAFF_SCROLL_PACKET_ID = new Identifier(VerdantArcanum.MOD_ID, "staff_scroll");

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

                    if (mainHandStack.getItem() instanceof LivingStaffItem) {
                        LivingStaffItem.handleScrollWheel(player.world, player, mainHandStack, direction);
                    } else if (offHandStack.getItem() instanceof LivingStaffItem) {
                        LivingStaffItem.handleScrollWheel(player.world, player, offHandStack, direction);
                    }
                }
            });
        });
    }

    // Client side method to send a scroll packet
    public static void sendScrollPacket(int direction) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(direction);
        ClientPlayNetworking.send(STAFF_SCROLL_PACKET_ID, buf);
    }
}