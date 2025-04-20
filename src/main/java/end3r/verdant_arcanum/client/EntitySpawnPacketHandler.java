package end3r.verdant_arcanum.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.network.EntitySpawnPacket;
import end3r.verdant_arcanum.registry.ModEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class EntitySpawnPacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntitySpawnPacketHandler.class);

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EntitySpawnPacket.ID, EntitySpawnPacketHandler::handleEntitySpawn);
    }

    public static void handleEntitySpawn(MinecraftClient client, ClientPlayNetworkHandler handler,
                                         PacketByteBuf buf, PacketSender sender) {
        // Read basic entity data
        int entityId = buf.readVarInt();
        UUID uuid = buf.readUuid();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();

        // Read beam-specific data
        double startX = buf.readDouble();
        double startY = buf.readDouble();
        double startZ = buf.readDouble();

        double endX = buf.readDouble();
        double endY = buf.readDouble();
        double endZ = buf.readDouble();

        float beamWidth = buf.readFloat();

        // Execute on main client thread
        client.execute(() -> {
            if (client.world == null)
                return;

            // Create a new SolarBeamEntity in the client world
            SolarBeamEntity solarBeamEntity = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, client.world);

            // Set entity ID and UUID
            solarBeamEntity.setId(entityId);
            solarBeamEntity.setUuid(uuid);

            // Position the entity
            solarBeamEntity.setPos(x, y, z);

            // Set beam properties
            Vec3d start = new Vec3d(startX, startY, startZ);
            Vec3d end = new Vec3d(endX, endY, endZ);
            solarBeamEntity.updateBeam(start, end);

            // Add the entity to the client world
            client.world.addEntity(entityId, solarBeamEntity);

            // Log to confirm entity creation
            LOGGER.info("Client created SolarBeamEntity from packet: id={}, pos=[{},{},{}], start={}, end={}",
                    entityId, x, y, z, start, end);
        });
    }
}