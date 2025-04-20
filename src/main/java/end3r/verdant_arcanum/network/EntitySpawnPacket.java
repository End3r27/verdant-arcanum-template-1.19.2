package end3r.verdant_arcanum.network;

import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.registry.ModEntities;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.util.Identifier;

public class EntitySpawnPacket {
    public static Packet<?> create(Entity entity, EntityType<?> entityType) {
        if (!(entity instanceof SolarBeamEntity)) {
            throw new IllegalArgumentException("Entity must be a SolarBeamEntity");
        }

        SolarBeamEntity beamEntity = (SolarBeamEntity) entity;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(entity.getId()); // Write the entity ID
        buf.writeUuid(entity.getUuid()); // Write the entity UUID
        buf.writeDouble(entity.getX());
        buf.writeDouble(entity.getY());
        buf.writeDouble(entity.getZ());
        buf.writeVarInt(ModEntities.getRawId(entityType)); // Entity type identifier

        // Write custom data for the beam entity
        buf.writeDouble(beamEntity.getStartPos().x);
        buf.writeDouble(beamEntity.getStartPos().y);
        buf.writeDouble(beamEntity.getStartPos().z);
        buf.writeDouble(beamEntity.getEndPos().x);
        buf.writeDouble(beamEntity.getEndPos().y);
        buf.writeDouble(beamEntity.getEndPos().z);
        buf.writeDouble(beamEntity.getBeamWidth()); // Beam width

        return ServerPlayNetworking.createS2CPacket(EntitySpawnPacket.ID, buf); // Custom spawn packet ID
    }


    public static final Identifier ID = new Identifier("verdant_arcanum", "entity_spawn");
}