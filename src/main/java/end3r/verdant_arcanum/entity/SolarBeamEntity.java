package end3r.verdant_arcanum.entity;

import end3r.verdant_arcanum.VerdantArcanum;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SolarBeamEntity extends Entity {

    private static final TrackedData<Float> BEAM_WIDTH = DataTracker.registerData(SolarBeamEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private Vec3d start = Vec3d.ZERO;
    private Vec3d end = Vec3d.ZERO;
    private Entity owner;

    public SolarBeamEntity(EntityType<? extends SolarBeamEntity> entityType, World world) {
        super(entityType, world);
        this.noClip = true;
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(BEAM_WIDTH, 2.5F);
    }

    @Override
    public void tick() {
        super.tick();

        // The entity doesn't need much tick logic as it's controlled by the spell
        if (this.world.isClient) {
            // Client-side particle effects can be added here
            spawnParticles();
        }
    }

    private void spawnParticles() {
        // Add ambient particles along the beam
        if (start != null && end != null) {
            Vec3d direction = end.subtract(start).normalize();
            double length = end.distanceTo(start);

            for (int i = 0; i < Math.min(length * 2, 40); i++) {
                double progress = world.random.nextDouble() * length;
                Vec3d particlePos = start.add(direction.multiply(progress));

                // Randomize position slightly
                particlePos = particlePos.add(
                        (world.random.nextDouble() - 0.5) * getBeamWidth(),
                        (world.random.nextDouble() - 0.5) * getBeamWidth(),
                        (world.random.nextDouble() - 0.5) * getBeamWidth()
                );

                world.addParticle(
                        net.minecraft.particle.ParticleTypes.END_ROD,
                        particlePos.x, particlePos.y, particlePos.z,
                        (world.random.nextDouble() - 0.5) * 0.02,
                        (world.random.nextDouble() - 0.5) * 0.02,
                        (world.random.nextDouble() - 0.5) * 0.02
                );
            }
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("BeamWidth")) {
            this.dataTracker.set(BEAM_WIDTH, nbt.getFloat("BeamWidth"));
        }

        if (nbt.contains("StartX") && nbt.contains("StartY") && nbt.contains("StartZ")) {
            this.start = new Vec3d(
                    nbt.getDouble("StartX"),
                    nbt.getDouble("StartY"),
                    nbt.getDouble("StartZ")
            );
        }

        if (nbt.contains("EndX") && nbt.contains("EndY") && nbt.contains("EndZ")) {
            this.end = new Vec3d(
                    nbt.getDouble("EndX"),
                    nbt.getDouble("EndY"),
                    nbt.getDouble("EndZ")
            );
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putFloat("BeamWidth", this.getBeamWidth());

        if (start != null) {
            nbt.putDouble("StartX", start.x);
            nbt.putDouble("StartY", start.y);
            nbt.putDouble("StartZ", start.z);
        }

        if (end != null) {
            nbt.putDouble("EndX", end.x);
            nbt.putDouble("EndY", end.y);
            nbt.putDouble("EndZ", end.z);
        }
    }

    public float getBeamWidth() {
        return this.dataTracker.get(BEAM_WIDTH);
    }

    public void setBeamWidth(float width) {
        this.dataTracker.set(BEAM_WIDTH, width);
    }

    public Vec3d getStart() {
        return start;
    }

    public void setStart(Vec3d start) {
        this.start = start;
    }

    public Vec3d getEnd() {
        return end;
    }

    public void setEnd(Vec3d end) {
        this.end = end;
    }

    public Entity getOwner() {
        return owner;
    }

    public void setOwner(Entity owner) {
        this.owner = owner;
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }

    @Override
    public boolean shouldRender(double distance) {
        // Always render the beam regardless of distance
        return true;
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        VerdantArcanum.LOGGER.info("SolarBeamEntity received spawn packet on CLIENT side with ID: {}", this.getId());
    }

}