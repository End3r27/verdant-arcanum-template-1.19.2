package end3r.verdant_arcanum.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.registry.ModEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Environment(EnvType.CLIENT)
public class EntitySpawnPacketHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntitySpawnPacketHandler.class);

    public static void receiveEntityPacket(MinecraftClient client, ClientPlayNetworkHandler handler,
                                           PacketByteBuf buf, PacketSender responseSender) {
        // Copy packet buffer for safe access within controlled scope
        PacketByteBuf dataBuf = new PacketByteBuf(buf.copy());

        int entityTypeId = -1;
        UUID uuid = null;
        int entityId = -1;
        double x = 0, y = 0, z = 0;

        try {
            entityTypeId = dataBuf.readVarInt();
            uuid = dataBuf.readUuid();
            entityId = dataBuf.readVarInt();
            x = dataBuf.readDouble();
            y = dataBuf.readDouble();
            z = dataBuf.readDouble();
        } catch (Exception e) {
            LOGGER.error("Error reading base entity data from spawn packet: {}", e.getMessage());
            dataBuf.release();  // Ensure buffer release on early read errors
            return;
        }

        EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityTypeId);

        // Create final copies of variables to use in lambda
        final int finalEntityId = entityId;
        final UUID finalUuid = uuid;
        final double finalX = x, finalY = y, finalZ = z;

        LOGGER.info("Received spawn packet for entity type {} at position {},{},{}", entityType, x, y, z);

        client.execute(() -> {
            if (client.world == null) {
                dataBuf.release(); // Cleanup after execution if world is null
                return;
            }

            if (entityType == ModEntities.SOLAR_BEAM_ENTITY) {
                try {
                    // Read beam-specific data
                    double startX = dataBuf.readDouble();
                    double startY = dataBuf.readDouble();
                    double startZ = dataBuf.readDouble();
                    double endX = dataBuf.readDouble();
                    double endY = dataBuf.readDouble();
                    double endZ = dataBuf.readDouble();
                    double width = dataBuf.readDouble();

                    // Create beam entity
                    SolarBeamEntity beam = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, client.world);
                    beam.setId(finalEntityId);
                    beam.setUuid(finalUuid);

                    // Update beam with received data
                    Vec3d start = new Vec3d(startX, startY, startZ);
                    Vec3d end = new Vec3d(endX, endY, endZ);

                    LOGGER.info("Setting up beam entity with start={}, end={}, width={}",
                            start, end, width);

                    // Set beam width before updating beam
                    beam.beamWidth = width;
                    beam.updateBeam(start, end);

                    // Additional safety check - ensure position matches start
                    if (Math.abs(beam.getX() - startX) > 0.01) {
                        LOGGER.warn("Position mismatch after creation, forcing correction");
                        beam.setPosition(startX, startY, startZ);
                    }

                    // Add to world
                    client.world.addEntity(finalEntityId, beam);

                    LOGGER.info("Added beam entity to world, final position: {},{},{}",
                            beam.getX(), beam.getY(), beam.getZ());
                } catch (Exception e) {
                    LOGGER.error("Error reading beam-specific data from spawn packet: {}", e.getMessage());
                } finally {
                    dataBuf.release();  // Ensure buffer is released regardless of read outcome
                }
            } else {
                try {
                    // Handle other entity types
                    Entity entity = entityType.create(client.world);
                    if (entity != null) {
                        entity.setId(finalEntityId);
                        entity.setUuid(finalUuid);
                        entity.setPosition(finalX, finalY, finalZ);
                        client.world.addEntity(finalEntityId, entity);
                    }
                } finally {
                    dataBuf.release(); // Release buffer after entity handling
                }
            }
        });
    }
}

