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
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Box;
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
        
        // Update the bounding box each tick to match the beam's position and size
        this.updateBoundingBox();
    }

    private void spawnParticles() {
        // Add ambient particles along the beam
        if (start != null && end != null) {
            Vec3d direction = end.subtract(start).normalize();
            double length = end.distanceTo(start);

            // Create a beam using end rod particles
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
            
            // Explosion effect at the end of the beam
            for (int i = 0; i < 50; i++) {  // Adjust number of particles for explosion density
                Vec3d explosionParticlePos = end.add(
                        (world.random.nextDouble() - 0.5) * getBeamWidth(),
                        (world.random.nextDouble() - 0.5) * getBeamWidth(),
                        (world.random.nextDouble() - 0.5) * getBeamWidth()
                );

                world.addParticle(
                        ParticleTypes.END_ROD,  // Explosion at the end
                        explosionParticlePos.x, explosionParticlePos.y, explosionParticlePos.z,
                        (world.random.nextDouble() - 0.5) * 0.5,
                        (world.random.nextDouble() - 0.5) * 0.5,
                        (world.random.nextDouble() - 0.5) * 0.5
                );
            }
        }
    }
    
    /**
     * Updates the bounding box to match the beam's dimensions
     */
    protected void updateBoundingBox() {
        if (start != null && end != null) {
            double minX = Math.min(start.x, end.x) - this.getBeamWidth() / 2;
            double minY = Math.min(start.y, end.y) - this.getBeamWidth() / 2;
            double minZ = Math.min(start.z, end.z) - this.getBeamWidth() / 2;
            double maxX = Math.max(start.x, end.x) + this.getBeamWidth() / 2;
            double maxY = Math.max(start.y, end.y) + this.getBeamWidth() / 2;
            double maxZ = Math.max(start.z, end.z) + this.getBeamWidth() / 2;

            this.setBoundingBox(new Box(minX, minY, minZ, maxX, maxY, maxZ));
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
