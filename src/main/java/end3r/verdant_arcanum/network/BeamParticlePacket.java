package end3r.verdant_arcanum.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Vec3d;

public class BeamParticlePacket {
    private final Vec3d startPos;
    private final Vec3d direction;

    public BeamParticlePacket(Vec3d startPos, Vec3d direction) {
        this.startPos = startPos;
        this.direction = direction;
    }

    // Write packet data
    public void write(PacketByteBuf buf) {
        buf.writeDouble(startPos.x);
        buf.writeDouble(startPos.y);
        buf.writeDouble(startPos.z);

        buf.writeDouble(direction.x);
        buf.writeDouble(direction.y);
        buf.writeDouble(direction.z);
    }

    // Read packet data
    public static BeamParticlePacket read(PacketByteBuf buf) {
        Vec3d pos = new Vec3d(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );

        Vec3d dir = new Vec3d(
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble()
        );

        return new BeamParticlePacket(pos, dir);
    }

    public Vec3d getStartPos() {
        return startPos;
    }

    public Vec3d getDirection() {
        return direction;
    }
}