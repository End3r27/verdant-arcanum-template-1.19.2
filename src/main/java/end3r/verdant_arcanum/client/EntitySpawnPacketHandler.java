package end3r.verdant_arcanum.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class EntitySpawnPacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntitySpawnPacketHandler.class);

    public static void receiveEntityPacket(MinecraftClient client, ClientPlayNetworkHandler handler,
                                           PacketByteBuf buf, PacketSender responseSender) {
        // Important: Read ALL data from the buffer BEFORE the client.execute call
        // because the buffer is only valid during this method call

        // Read entity type and ID data
        net.minecraft.entity.EntityType<?> entityType = net.minecraft.util.registry.Registry.ENTITY_TYPE.get(buf.readVarInt());
        UUID entityUUID = buf.readUuid();
        int entityID = buf.readVarInt();

        // Read position
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();

        // Pre-read beam-specific data if needed - OUTSIDE the client.execute block
        // Create containers to hold the data we read
        Vec3d start = null;
        Vec3d end = null;
        double width = 0;

        // Try to determine if this is a beam entity by checking entity type
        boolean isBeamEntity = entityType.toString().contains("solar_beam");
        if (isBeamEntity) {
            try {
                // Read beam data immediately
                double startX = buf.readDouble();
                double startY = buf.readDouble();
                double startZ = buf.readDouble();
                double endX = buf.readDouble();
                double endY = buf.readDouble();
                double endZ = buf.readDouble();
                width = buf.readDouble();

                // Store it to use later
                start = new Vec3d(startX, startY, startZ);
                end = new Vec3d(endX, endY, endZ);
            } catch (Exception e) {
                // Handle case where we might not have beam data
                LOGGER.error("Error reading beam data", e);
            }
        }

        // Store final references for lambda
        final Vec3d finalStart = start;
        final Vec3d finalEnd = end;
        final double finalWidth = width;

        // Now we can safely use client.execute - the buffer is no longer accessed inside
        client.execute(() -> {
            if (client.world == null)
                return;

            // Create entity
            net.minecraft.entity.Entity entity = entityType.create(client.world);
            if (entity == null) {
                LOGGER.warn("Failed to create instance of entity type {}", entityType);
                return;
            }

            // Set entity data
            entity.setId(entityID);
            entity.setUuid(entityUUID);

            // Set position
            entity.setPos(x, y, z);

            // For beam entities, set their specific data using the pre-read values
            if (entity instanceof SolarBeamEntity beamEntity && finalStart != null && finalEnd != null) {
                // Use the pre-read data we captured above
                beamEntity.updateBeam(finalStart, finalEnd);

                // Additional beam properties if needed
                // beamEntity.setBeamWidth(finalWidth);

                LOGGER.info("Set beam data: start={}, end={}", finalStart, finalEnd);
            }

            // Add entity to world
            client.world.addEntity(entityID, entity);
            LOGGER.info("Spawned entity {} at {}, {}, {}",
                    entity.getType().toString(), x, y, z);
        });
    }
}