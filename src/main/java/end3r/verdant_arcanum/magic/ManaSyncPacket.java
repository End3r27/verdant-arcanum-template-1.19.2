package end3r.verdant_arcanum.magic;


import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ManaSyncPacket {
    private static final Identifier CHANNEL = new Identifier("verdant_arcanum", "mana_sync");

    // Register the server-side packet sender
    public static void registerServer() {
        // This method would set up how the server sends packets to clients
    }

    // Register the client-side packet receiver
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, responseSender) -> {
            // Only read what the server actually sent
            if (buf.readableBytes() >= 8) {
                int maxMana = buf.readInt();
                float currentMana = buf.readInt(); // You might want to convert to float here

                client.execute(() -> {
                    // Use the combined setter method instead of the individual ones
                    ClientManaData.setMana(currentMana, maxMana, ClientManaData.getRegenMultiplier());
                    // This uses the existing regen multiplier value since it's not being synced in this packet
                });
            }
        });
    }


    // Method used by the server to send mana data to a client
    public static void sendToClient(ServerPlayerEntity player, ManaSystem.PlayerMana manaData) {
        PacketByteBuf buf = PacketByteBufs.create();

        // Write exactly what will be read on the client side
        buf.writeInt(manaData.getMaxMana());
        buf.writeInt((int)manaData.getCurrentMana()); // Cast float to int if needed

        // Send only the data that's expected
        ServerPlayNetworking.send(player, CHANNEL, buf);
    }
}