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
    public static Packet<?> create(Entity entity) {
        PacketByteBuf buf = PacketByteBufs.create();

        // Write entity type ID, UUID, and entity ID
        buf.writeVarInt(Registry.ENTITY_TYPE.getRawId(entity.getType()));
        buf.writeUuid(entity.getUuid());
        buf.writeVarInt(entity.getId());

        // Write position
        buf.writeDouble(entity.getX());
        buf.writeDouble(entity.getY());
        buf.writeDouble(entity.getZ());

        // Write beam-specific data if applicable
        if (entity instanceof SolarBeamEntity beam) {
            Vec3d start = beam.getStartPos();
            Vec3d end = beam.getEndPos();

            buf.writeDouble(start.x);
            buf.writeDouble(start.y);
            buf.writeDouble(start.z);
            buf.writeDouble(end.x);
            buf.writeDouble(end.y);
            buf.writeDouble(end.z);
            buf.writeDouble(beam.getBeamWidth()); // Assuming this method exists
        }

        // Create and return the packet
        return ServerPlayNetworking.createS2CPacket(
                new Identifier("verdant_arcanum", "spawn_entity"),
                buf
        );
    }
}
