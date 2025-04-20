package end3r.verdant_arcanum.entity;

import end3r.verdant_arcanum.network.EntitySpawnPacket;
import end3r.verdant_arcanum.registry.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SolarBeamEntity extends Entity {
    private Vec3d startPosition;
    private Vec3d endPosition;
    private double beamWidth;

    public SolarBeamEntity(EntityType<?> type, World world) {
        super(type, world);

        // Default initial values
        this.startPosition = Vec3d.ZERO;
        this.endPosition = Vec3d.ZERO;
        this.beamWidth = 2.5; // Default width of the beam
    }

    public SolarBeamEntity(World world, Vec3d startPosition, Vec3d endPosition, double beamWidth) {
        this(ModEntities.SOLAR_BEAM_ENTITY, world); // Use our registered entity type
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.beamWidth = beamWidth;

        // Update bounding box based on positions
        updateBeamBoundingBox();
    }

    /**
     * Initialize the DataTracker for this entity.
     * This implementation doesn't need to track any additional data, so it's currently empty.
     */
    @Override
    protected void initDataTracker() {
        // No synchronized data to track for the SolarBeamEntity
    }

    /**
     * Update the SolarBeamEntity's positions and bounding box.
     *
     * @param start The new starting position of the beam.
     * @param end   The new ending position of the beam.
     */
    public void updateBeam(Vec3d start, Vec3d end) {
        this.startPosition = start;
        this.endPosition = end;

        // Update bounding box based on beam dimensions
        updateBeamBoundingBox();
    }

    /**
     * Update the bounding box of the entity based on the start and end positions.
     */
    private void updateBeamBoundingBox() {
        this.setBoundingBox(new Box(
                startPosition.x - beamWidth, startPosition.y - beamWidth, startPosition.z - beamWidth,
                endPosition.x + beamWidth, endPosition.y + beamWidth, endPosition.z + beamWidth
        ));
    }

    /**
     * Getter for the start position of the beam.
     *
     * @return The starting position of the beam.
     */
    public Vec3d getStartPos() {
        return startPosition;
    }

    /**
     * Getter for the end position of the beam.
     *
     * @return The ending position of the beam.
     */
    public Vec3d getEndPos() {
        return endPosition;
    }

    /**
     * Getter for the width of the beam.
     *
     * @return The width of the beam.
     */
    public double getBeamWidth() {
        return beamWidth;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        double startX = nbt.getDouble("startX");
        double startY = nbt.getDouble("startY");
        double startZ = nbt.getDouble("startZ");
        this.startPosition = new Vec3d(startX, startY, startZ);

        double endX = nbt.getDouble("endX");
        double endY = nbt.getDouble("endY");
        double endZ = nbt.getDouble("endZ");
        this.endPosition = new Vec3d(endX, endY, endZ);

        this.beamWidth = nbt.getDouble("beamWidth");
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putDouble("startX", this.startPosition.x);
        nbt.putDouble("startY", this.startPosition.y);
        nbt.putDouble("startZ", this.startPosition.z);

        nbt.putDouble("endX", this.endPosition.x);
        nbt.putDouble("endY", this.endPosition.y);
        nbt.putDouble("endZ", this.endPosition.z);

        nbt.putDouble("beamWidth", this.beamWidth);
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return EntitySpawnPacket.create(this, ModEntities.SOLAR_BEAM_ENTITY);
    }



}