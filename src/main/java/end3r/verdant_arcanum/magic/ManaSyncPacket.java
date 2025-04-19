package end3r.verdant_arcanum.magic;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;

import static end3r.verdant_arcanum.magic.ManaEventHandler.MANA_SYNC_ID;

public class ManaSyncPacket {

    private final float currentMana;
    private final int maxMana;

    public ManaSyncPacket(float currentMana, int maxMana) {
        this.currentMana = currentMana;
        this.maxMana = maxMana;
    }

    public static void send(ServerPlayerEntity player, ManaSyncPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeFloat(packet.currentMana);
        buf.writeInt(packet.maxMana);
        ServerPlayNetworking.send(player, MANA_SYNC_ID, buf);
    }

    // Register this client-side in your client mod initializer
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(MANA_SYNC_ID, (client, handler, buf, responseSender) -> {
            float currentMana = buf.readFloat();
            int maxMana = buf.readInt();

            // Update client-side mana display
            client.execute(() -> {
                if (client.player != null) {
                    ClientManaData.setMana(currentMana, maxMana);
                }
            });
        });
    }
}