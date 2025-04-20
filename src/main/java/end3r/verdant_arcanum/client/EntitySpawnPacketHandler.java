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
        int entityTypeId = buf.readVarInt();
        UUID uuid = buf.readUuid();
        int entityId = buf.readVarInt();
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();

        EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityTypeId);

        // Log basic entity data
        LOGGER.info("Received spawn packet for entity type {} at position {},{},{}",
                entityType.toString(), x, y, z);

        client.execute(() -> {
            if (client.world == null) return;

            // Check if entity is SolarBeamEntity
            if (entityType == ModEntities.SOLAR_BEAM_ENTITY) {
                // Read beam-specific data
                double startX = buf.readDouble();
                double startY = buf.readDouble();
                double startZ = buf.readDouble();
                double endX = buf.readDouble();
                double endY = buf.readDouble();
                double endZ = buf.readDouble();
                double width = buf.readDouble();

                // Create beam entity
                SolarBeamEntity beam = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, client.world);
                beam.setId(entityId);
                beam.setUuid(uuid);

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
                client.world.addEntity(entityId, beam);

                LOGGER.info("Added beam entity to world, final position: {},{},{}",
                        beam.getX(), beam.getY(), beam.getZ());
            } else {
                // Handle other entity types
                Entity entity = entityType.create(client.world);
                if (entity != null) {
                    entity.setId(entityId);
                    entity.setUuid(uuid);
                    entity.setPosition(x, y, z);
                    client.world.addEntity(entityId, entity);
                }
            }
        });
    }
}