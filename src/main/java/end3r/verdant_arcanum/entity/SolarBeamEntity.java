package end3r.verdant_arcanum.entity;

import end3r.verdant_arcanum.entity.client.SolarBeamEntityRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import end3r.verdant_arcanum.network.EntitySpawnPacket;
import end3r.verdant_arcanum.registry.ModEntities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public SolarBeamEntity(EntityType<?> type, World world) {
        super(type, world);

        this.startPosition = Vec3d.ZERO;
        this.endPosition = Vec3d.ZERO;

        LOGGER.info("Created SolarBeamEntity with default constructor");


    }

    public SolarBeamEntity(World world, Vec3d startPosition, Vec3d endPosition, double beamWidth) {
        super(ModEntities.SOLAR_BEAM_ENTITY, world);
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.beamWidth = beamWidth;
        this.noClip = true; // Pass through blocks
    }

    @Override
    protected void initDataTracker() {
        this.dataTracker.startTracking(START_X, 0.0f);
        this.dataTracker.startTracking(START_Y, 0.0f);
        this.dataTracker.startTracking(START_Z, 0.0f);
        this.dataTracker.startTracking(END_X, 0.0f);
        this.dataTracker.startTracking(END_Y, 0.0f);
        this.dataTracker.startTracking(END_Z, 0.0f);
        this.dataTracker.startTracking(BEAM_WIDTH, 1.0f);
    }


    @Override
    public void tick() {
        super.tick();

        if (world.isClient) {
            renderManually();
        }


        if (this.age == 1) {
            if (this.startPosition == null || (this.startPosition.x == 0 && this.startPosition.y == 0 && this.startPosition.z == 0)) {
                // Initialize positions using the entity's CURRENT position
                this.startPosition = new Vec3d(this.getX(), this.getY(), this.getZ());
                this.endPosition = new Vec3d(this.getX(), this.getY() + 5, this.getZ()); // Default 5 blocks up

                // Update the data tracker with these values
                this.updateBeam(this.startPosition, this.endPosition);

                LOGGER.info("Initialized beam positions on first tick: {} to {}", this.startPosition, this.endPosition);
            }
        }


        if (this.age <= 5) {
            LOGGER.info("SolarBeam ticking at {}, {}, {} (age: {})",
                    this.getX(), this.getY(), this.getZ(), this.age);
            LOGGER.info("Start position: {}, End position: {}", this.startPosition, this.endPosition);
        }


        // Update cached values from dataTracker
        this.startPosition = new Vec3d(
                this.dataTracker.get(START_X),
                this.dataTracker.get(START_Y),
                this.dataTracker.get(START_Z)
        );

        this.endPosition = new Vec3d(
                this.dataTracker.get(END_X),
                this.dataTracker.get(END_Y),
                this.dataTracker.get(END_Z)
        );

        this.beamWidth = this.dataTracker.get(BEAM_WIDTH);

        // Update bounding box regularly
        updateBoundingBox();

        // Add this debug log
        LOGGER.info("SolarBeam ticking at {}, {}, {} (age: {})",
                this.getX(), this.getY(), this.getZ(), this.age);
        LOGGER.info("Start position: {}, End position: {}",
                this.getStartPos(), this.getEndPos());

        // Add this client-side rendering code
        if (world.isClient) {
            LOGGER.info("Client-side tick, attempting manual render");
            MinecraftClient client = MinecraftClient.getInstance();

            // This line will only run on the next frame after the world tick
            client.execute(() -> {
                EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();
                LOGGER.info("Looking for renderer for {}", this.getType());

                try {
                    // Force rendering
                    MatrixStack matrixStack = new MatrixStack();
                    float tickDelta = client.getTickDelta();

                    // Get our current renderer
                    var renderer = dispatcher.getRenderer(this);
                    if (renderer != null) {
                        LOGGER.info("Found renderer: {}", renderer.getClass().getName());

                        // Set up matrices for rendering at entity position
                        matrixStack.push();
                        Vec3d cameraPos = client.gameRenderer.getCamera().getPos();
                        matrixStack.translate(getX() - cameraPos.x, getY() - cameraPos.y, getZ() - cameraPos.z);

                        // Manually render
                        renderer.render(this,
                                getYaw(tickDelta),
                                tickDelta,
                                matrixStack,
                                client.getBufferBuilders().getEntityVertexConsumers(),
                                15728880); // Full brightness

                        matrixStack.pop();
                        LOGGER.info("Manual render attempt completed");
                    } else {
                        LOGGER.error("No renderer found for SolarBeamEntity");
                    }
                } catch (Exception e) {
                    LOGGER.error("Error during manual rendering", e);
                }
            });
        }

    }




    public void updateBeam(Vec3d start, Vec3d end) {
        // Update DataTracker (which synchronizes to client)
        this.dataTracker.set(START_X, (float) start.x);
        this.dataTracker.set(START_Y, (float) start.y);
        this.dataTracker.set(START_Z, (float) start.z);
        this.dataTracker.set(END_X, (float) end.x);
        this.dataTracker.set(END_Y, (float) end.y);
        this.dataTracker.set(END_Z, (float) end.z);

        // Also update local fields
        this.startPosition = start;
        this.endPosition = end;

        // Update bounding box
        updateBoundingBox();
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
    public Packet<?> createSpawnPacket() {
        // Create custom spawn packet that includes our position data
        PacketByteBuf buf = PacketByteBufs.create();

        // Entity ID and UUID (required)
        buf.writeVarInt(this.getId());
        buf.writeUuid(this.getUuid());

        // Entity position (required)
        buf.writeDouble(this.getX());
        buf.writeDouble(this.getY());
        buf.writeDouble(this.getZ());

        // Additional data specific to SolarBeamEntity
        Vec3d start = this.getStartPos();
        Vec3d end = this.getEndPos();

        buf.writeDouble(start.x);
        buf.writeDouble(start.y);
        buf.writeDouble(start.z);

        buf.writeDouble(end.x);
        buf.writeDouble(end.y);
        buf.writeDouble(end.z);

        buf.writeFloat((float) this.getBeamWidth());

        return ServerPlayNetworking.createS2CPacket(EntitySpawnPacket.ID, buf);
    }

    @Override
    public void setPosition(double x, double y, double z) {
        super.setPosition(x, y, z);

        // If this is a fresh entity (no positions set), update the beam
        if ((this.startPosition == null || (this.startPosition.x == 0 && this.startPosition.y == 0 && this.startPosition.z == 0))) {
            this.startPosition = new Vec3d(x, y, z);
            this.endPosition = new Vec3d(x, y + 5, z); // Default 5 blocks up

            // Only update the tracker if we're not in constructor (world might be null)
            if (this.world != null) {
                this.updateBeam(this.startPosition, this.endPosition);
                LOGGER.info("Updated beam positions on setPosition: {} to {}", this.startPosition, this.endPosition);
            }
        }
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
}