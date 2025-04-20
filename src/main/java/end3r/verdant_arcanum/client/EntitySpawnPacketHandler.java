package end3r.verdant_arcanum.client;

import end3r.verdant_arcanum.network.EntitySpawnPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.registry.Registry;

import java.util.UUID;

public class EntitySpawnPacketHandler {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(EntitySpawnPacket.ID, (client, handler, buf, responseSender) -> {
            int entityId = buf.readVarInt(); // Read entity ID
            UUID entityUuid = buf.readUuid(); // Read entity UUID
            double x = buf.readDouble(); // Read X position
            double y = buf.readDouble(); // Read Y position
            double z = buf.readDouble(); // Read Z position
            int entityTypeRawId = buf.readVarInt(); // Read entity type ID

            client.execute(() -> {
                // Recreate the entity on the client
                EntityType<?> entityType = Registry.ENTITY_TYPE.get(entityTypeRawId);
                if (entityType != null) {
                    Entity entity = entityType.create(client.world);
                    if (entity != null) {
                        entity.setPos(x, y, z);
                        entity.updateTrackedPosition(x, y, z);
                        entity.setId(entityId);
                        entity.setUuid(entityUuid);

                        client.world.addEntity(entityId, entity); // Add entity to the client world
                    }
                }
            });
        });
    }
}