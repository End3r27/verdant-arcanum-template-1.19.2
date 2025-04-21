package end3r.verdant_arcanum.network;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.registry.ModEntities;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class BeamSyncPacket {
    // Identifier for the packet channel
    public static final Identifier BEAM_SYNC_PACKET_ID = new Identifier(VerdantArcanum.MOD_ID, "beam_sync");

    // Register client-side packet receiver
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BEAM_SYNC_PACKET_ID, BeamSyncPacket::receiveBeamSyncPacket);
    }

    // Register server-side packet handler - call this from your mod initializer
    public static void registerServer() {
        // This method is empty as we're only sending from server to client,
        // but you could add server-side handling here if needed
    }

    // Send packet from server to client
    public static void sendToClient(ServerPlayerEntity player, SolarBeamEntity beamEntity) {
        PacketByteBuf buf = PacketByteBufs.create();

        // Write entity ID
        buf.writeInt(beamEntity.getId());

        // Write start position
        Vec3d start = beamEntity.getStart();
        buf.writeDouble(start.x);
        buf.writeDouble(start.y);
        buf.writeDouble(start.z);

        // Write end position
        Vec3d end = beamEntity.getEnd();
        buf.writeDouble(end.x);
        buf.writeDouble(end.y);
        buf.writeDouble(end.z);

        // Write beam width
        buf.writeFloat(beamEntity.getBeamWidth());

        // Send to the player client
        ServerPlayNetworking.send(player, BEAM_SYNC_PACKET_ID, buf);
    }

    // Receive and handle the packet on client
    private static void receiveBeamSyncPacket(MinecraftClient client, ClientPlayNetworkHandler handler,
                                              PacketByteBuf buf, PacketSender sender) {
        // Read data from buffer
        int entityId = buf.readInt();

        double startX = buf.readDouble();
        double startY = buf.readDouble();
        double startZ = buf.readDouble();
        Vec3d start = new Vec3d(startX, startY, startZ);

        double endX = buf.readDouble();
        double endY = buf.readDouble();
        double endZ = buf.readDouble();
        Vec3d end = new Vec3d(endX, endY, endZ);

        float width = buf.readFloat();

        // Execute on main client thread
        client.execute(() -> {
            ClientWorld world = client.world;
            if (world == null) return;

            // Try to find existing entity
            SolarBeamEntity beamEntity = (SolarBeamEntity)world.getEntityById(entityId);

            if (beamEntity == null) {
                // Entity doesn't exist yet, create it
                beamEntity = new SolarBeamEntity(ModEntities.SOLAR_BEAM, world);
                beamEntity.setId(entityId);
                beamEntity.updatePosition(startX, startY, startZ);
                world.spawnEntity(beamEntity);
                world.addEntity(entityId, beamEntity);
                VerdantArcanum.LOGGER.info("Created SolarBeamEntity on client with ID: {}", entityId);
            }

            // Update beam properties
            beamEntity.setStart(start);
            beamEntity.setEnd(end);
            beamEntity.setBeamWidth(width);

            VerdantArcanum.LOGGER.info("Updated beam entity {} at {} -> {} on CLIENT",
                    beamEntity.getId(), start, end);
        });
    }
}