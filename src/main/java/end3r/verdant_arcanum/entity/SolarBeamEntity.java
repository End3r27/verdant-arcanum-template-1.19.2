package end3r.verdant_arcanum.entity;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import end3r.verdant_arcanum.network.EntitySpawnPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SolarBeamEntity extends Entity {
    private static final Logger LOGGER = LoggerFactory.getLogger(SolarBeamEntity.class);

    // For Fabric 1.19.2, use direct field access
    private static final TrackedData<Float> START_X = DataTracker.registerData(SolarBeamEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> START_Y = DataTracker.registerData(SolarBeamEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> START_Z = DataTracker.registerData(SolarBeamEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_X = DataTracker.registerData(SolarBeamEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Y = DataTracker.registerData(SolarBeamEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> END_Z = DataTracker.registerData(SolarBeamEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> BEAM_WIDTH = DataTracker.registerData(SolarBeamEntity.class, TrackedDataHandlerRegistry.FLOAT);

    // Cache for convenience
    private Vec3d startPosition;
    private Vec3d endPosition;
    public double beamWidth;

    private UUID casterUUID;


    public SolarBeamEntity(EntityType<?> entityType, World world) {
        super(entityType, world);


        // Store positions exactly as provided
        this.startPosition = Vec3d.ZERO;
        this.endPosition = Vec3d.ZERO;
        this.beamWidth = 1.0;
        this.noClip = true; // Pass through blocks

        // Set entity position to start position (no offset)
        this.setPosition(startPosition.x, startPosition.y, startPosition.z);

        // Initialize data tracker with these exact values
        this.dataTracker.set(START_X, (float) startPosition.x);
        this.dataTracker.set(START_Y, (float) startPosition.y);
        this.dataTracker.set(START_Z, (float) startPosition.z);
        this.dataTracker.set(END_X, (float) endPosition.x);
        this.dataTracker.set(END_Y, (float) endPosition.y);
        this.dataTracker.set(END_Z, (float) endPosition.z);
        this.dataTracker.set(BEAM_WIDTH, (float) beamWidth);

        // Update bounding box based on these positions
        this.updateBoundingBox();
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(START_X, 0.0f);
        this.dataTracker.startTracking(START_Y, 0.0f);
        this.dataTracker.startTracking(START_Z, 0.0f);
        this.dataTracker.startTracking(END_X, 0.0f);
        this.dataTracker.startTracking(END_Y, 0.0f);
        this.dataTracker.startTracking(END_Z, 0.0f);
        this.dataTracker.startTracking(BEAM_WIDTH, 0.5f);
    }



    @Override
    public void tick() {
        super.tick();

        // Log positions for debugging
        LOGGER.info("SolarBeam ticking at {}, {}, {} (age: {})", this.getX(), this.getY(), this.getZ(), this.age);
        LOGGER.info("Start position: {}, End position: {}", this.getStartPos(), this.getEndPos());

        if (world.isClient) {
            // On client side, ensure position matches the start position
            Vec3d start = this.getStartPos();

            // Important: Check if entity position doesn't match start position and fix it
            if (Math.abs(this.getX() - start.x) > 0.01 ||
                    Math.abs(this.getY() - start.y) > 0.01 ||
                    Math.abs(this.getZ() - start.z) > 0.01) {
                LOGGER.info("Client-side position correction: Entity at {},{},{}, Start at {},{},{}",
                        this.getX(), this.getY(), this.getZ(), start.x, start.y, start.z);
                this.setPosition(start.x, start.y, start.z);
            }

            // Client-side rendering helper
            LOGGER.info("Client-side tick, attempting manual render");
            renderManually();
        }
    }




    /**
     * Enhanced update beam that also updates entity position
     */
    public void updateBeam(Vec3d start, Vec3d end) {
        // Update data tracker
        this.dataTracker.set(START_X, (float) start.x);
        this.dataTracker.set(START_Y, (float) start.y);
        this.dataTracker.set(START_Z, (float) start.z);
        this.dataTracker.set(END_X, (float) end.x);
        this.dataTracker.set(END_Y, (float) end.y);
        this.dataTracker.set(END_Z, (float) end.z);

        // Update cached values
        this.startPosition = start;
        this.endPosition = end;
        this.beamWidth = this.dataTracker.get(BEAM_WIDTH);

        // Critical: Update entity position to match start position
        // This is the key line that ensures synchronization
        this.setPosition(start.x, start.y, start.z);

        // Update bounding box
        updateBoundingBox();

        LOGGER.info("Beam updated - Start: {}, End: {}, Entity position now: {},{},{}",
                start, end, this.getX(), this.getY(), this.getZ());
    }





    // Calculate and update the entity's bounding box based on start and end positions
    private void updateBoundingBox() {
        double minX = Math.min(startPosition.x, endPosition.x) - beamWidth / 2;
        double minY = Math.min(startPosition.y, endPosition.y) - beamWidth / 2;
        double minZ = Math.min(startPosition.z, endPosition.z) - beamWidth / 2;
        double maxX = Math.max(startPosition.x, endPosition.x) + beamWidth / 2;
        double maxY = Math.max(startPosition.y, endPosition.y) + beamWidth / 2;
        double maxZ = Math.max(startPosition.z, endPosition.z) + beamWidth / 2;

        this.setBoundingBox(new Box(minX, minY, minZ, maxX, maxY, maxZ));
    }


    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
        if (nbt.contains("StartX")) {
            Vec3d start = new Vec3d(
                    nbt.getDouble("StartX"),
                    nbt.getDouble("StartY"),
                    nbt.getDouble("StartZ")
            );

            Vec3d end = new Vec3d(
                    nbt.getDouble("EndX"),
                    nbt.getDouble("EndY"),
                    nbt.getDouble("EndZ")
            );

            if (nbt.containsUuid("CasterUUID")) {
                this.casterUUID = nbt.getUuid("CasterUUID");
            }

                double width = nbt.getDouble("BeamWidth");

            // Set through the updateBeam method to ensure everything is updated
            updateBeam(start, end);
            this.dataTracker.set(BEAM_WIDTH, (float) width);
            this.beamWidth = width;
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
        // Write current values
        nbt.putDouble("StartX", this.startPosition.x);
        nbt.putDouble("StartY", this.startPosition.y);
        nbt.putDouble("StartZ", this.startPosition.z);

        nbt.putDouble("EndX", this.endPosition.x);
        nbt.putDouble("EndY", this.endPosition.y);
        nbt.putDouble("EndZ", this.endPosition.z);

        nbt.putDouble("BeamWidth", this.beamWidth);
        if (casterUUID != null) {
            nbt.putUuid("CasterUUID", casterUUID);

        }
    }

    @Override
    public Packet<?> createSpawnPacket() {
        // Use your custom packet system instead of the default Minecraft one
        return EntitySpawnPacket.create(this);
    }

    // Getter methods that use cached values for performance
    public Vec3d getStartPos() {
        return this.startPosition;
    }

    public Vec3d getEndPos() {
        return this.endPosition;
    }

    public double getBeamWidth() {
        return this.beamWidth;
    }



    @Override
    public void setPosition(double x, double y, double z) {
        // Call super implementation first
        super.setPosition(x, y, z);
        LOGGER.info("SolarBeamEntity position set to {},{},{}", x, y, z);

        // Update bounding box when position changes
        updateBoundingBox();
    }



    // This will only work on client side
    private void renderManually() {
        if (!world.isClient) return;

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

            // Try to get the registered renderer (may return null if not registered)
            EntityRenderer<? super SolarBeamEntity> renderer = dispatcher.getRenderer(this);

            if (renderer != null) {
                LOGGER.info("Found renderer: {}", renderer.getClass().getName());

                client.execute(() -> {
                    MatrixStack matrixStack = new MatrixStack();
                    float tickDelta = client.getTickDelta();

                    // Set up camera-relative position
                    Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
                    double x = this.getX() - cameraPos.x;
                    double y = this.getY() - cameraPos.y;
                    double z = this.getZ() - cameraPos.z;

                    matrixStack.push();
                    matrixStack.translate(x, y, z);

                    // Call render directly
                    renderer.render(this,
                            this.getYaw(tickDelta),
                            tickDelta,
                            matrixStack,
                            client.getBufferBuilders().getEntityVertexConsumers(),
                            15728880); // Full brightness

                    matrixStack.pop();
                    LOGGER.info("Manually rendered SolarBeam at {}, {}, {}", this.getX(), this.getY(), this.getZ());
                });
            } else {
                LOGGER.error("No renderer found for SolarBeamEntity");
            }
        } catch (Exception e) {
            LOGGER.error("Error during manual rendering", e);
        }
    }
    public void setCaster(PlayerEntity player) {
        if (player != null) {
            this.casterUUID = player.getUuid();
        }
    }

}