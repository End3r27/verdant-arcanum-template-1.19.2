package end3r.verdant_arcanum.network;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.registry.ModEntities;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

public class EntitySpawnPacket {
    public static Packet<?> create(Entity entity, PacketWriter extraData) {
        PacketByteBuf buf = PacketByteBufs.create();

        // Write entity type ID, UUID, and entity ID
        buf.writeVarInt(Registry.ENTITY_TYPE.getRawId(entity.getType()));
        buf.writeUuid(entity.getUuid());
        buf.writeVarInt(entity.getId());

        // Write position
        buf.writeDouble(entity.getX());
        buf.writeDouble(entity.getY());
        buf.writeDouble(entity.getZ());

        // Write additional data if provided
        if (extraData != null) {
            extraData.write(buf);
        }

        // Create and return the packet
        return ServerPlayNetworking.createS2CPacket(
                new Identifier("verdant_arcanum", "spawn_entity"),
                buf
        );
    }

    // Keep the existing create method for backward compatibility
    public static Packet<?> create(Entity entity) {
        // For SolarBeamEntity, use the special handling
        if (entity instanceof SolarBeamEntity beam) {
            return create(entity, (buffer) -> {
                Vec3d start = beam.getStartPos();
                Vec3d end = beam.getEndPos();

                buffer.writeDouble(start.x);
                buffer.writeDouble(start.y);
                buffer.writeDouble(start.z);
                buffer.writeDouble(end.x);
                buffer.writeDouble(end.y);
                buffer.writeDouble(end.z);
                buffer.writeDouble(beam.getBeamWidth());
            });
        }

        // For other entities, use the basic version
        return create(entity, null);
    }

    // Define a functional interface for writing extra data
    @FunctionalInterface
    public interface PacketWriter {
        void write(PacketByteBuf buffer);
    }
}