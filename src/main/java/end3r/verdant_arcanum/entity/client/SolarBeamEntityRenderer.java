package end3r.verdant_arcanum.entity.client;

import end3r.verdant_arcanum.entity.SolarBeamEntity;
import end3r.verdant_arcanum.VerdantArcanum;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;

public class SolarBeamEntityRenderer extends EntityRenderer<SolarBeamEntity> {

// Removed duplicate implementation as it was already defined in the class.
    private static final Identifier BEAM_TEXTURE =
            new Identifier(VerdantArcanum.MOD_ID, "textures/entity/solar_beam.png");

    public SolarBeamEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(SolarBeamEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // Log rendering logic
        VerdantArcanum.LOGGER.info("Rendering SolarBeamEntity at start: {}, end: {}",
                entity.getStartPos(), entity.getEndPos());

        // Get start and end positions of the beam
        Vec3d startPos = entity.getStartPos();
        Vec3d endPos = entity.getEndPos();

        if (startPos == null || endPos == null) {
            return;
        }

        // Calculate beam vector
        Vec3d beamVec = endPos.subtract(startPos);
        double beamLength = beamVec.length();

        if (beamLength < 0.1) {
            return; // Beam too short to render
        }

        // Get camera position for relative coordinates
        Vec3d cameraPos = this.dispatcher.camera.getPos();

        // Calculate camera-relative positions
        Vec3d relStart = startPos.subtract(cameraPos);

        // Calculate the beam direction (normalized)
        Vec3d beamDir = beamVec.normalize();

        // Set up the render
        float beamWidth = (float) (entity.getBeamWidth() / 2.0f);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucentEmissive(getTexture(entity))
        );

        // Save the current matrix state
        matrices.push();

        // Move to beam start position (relative to camera)
        matrices.translate(relStart.x, relStart.y, relStart.z);

        // Rotate to align with beam direction
        alignToVector(matrices, beamDir);

        // Create a pulsating glow effect
        float time = (entity.age + tickDelta) * 0.1f;
        float alpha = 0.7f + 0.3f * (float)Math.sin(time);
        int r = 255;  // Red component
        int g = 220;  // Green component
        int b = 100;  // Blue component
        int a = (int)(alpha * 255);

        // Get the position matrix
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        // Draw the beam - essentially a flat ribbon that extends in the Z direction
        // We'll use a number of segments for smoother appearance
        int segments = Math.max(1, (int)(beamLength / 2.0));
        float segmentLength = (float)beamLength / segments;

        for (int i = 0; i < segments; i++) {
            float z0 = i * segmentLength;
            float z1 = (i + 1) * segmentLength;
            float u0 = (float)i / segments;
            float u1 = (float)(i + 1) / segments;

            // First face - front side
            // Top left
            vertexConsumer.vertex(posMatrix, -beamWidth, beamWidth, z0)
                    .color(r, g, b, a)
                    .texture(u0, 0.0f)
                    .light(15728880) // Full brightness
                    .normal(0, 1, 0)
                    .next();

            // Bottom left
            vertexConsumer.vertex(posMatrix, -beamWidth, -beamWidth, z0)
                    .color(r, g, b, a)
                    .texture(u0, 1.0f)
                    .light(15728880)
                    .normal(0, 1, 0)
                    .next();

            // Bottom right
            vertexConsumer.vertex(posMatrix, beamWidth, -beamWidth, z1)
                    .color(r, g, b, a)
                    .texture(u1, 1.0f)
                    .light(15728880)
                    .normal(0, 1, 0)
                    .next();

            // Top right
            vertexConsumer.vertex(posMatrix, beamWidth, beamWidth, z1)
                    .color(r, g, b, a)
                    .texture(u1, 0.0f)
                    .light(15728880)
                    .normal(0, 1, 0)
                    .next();

            // Second face - back side (reversed winding order)
            // Top right
            vertexConsumer.vertex(posMatrix, beamWidth, beamWidth, z1)
                    .color(r, g, b, a)
                    .texture(u1, 0.0f)
                    .light(15728880)
                    .normal(0, -1, 0)
                    .next();

            // Bottom right
            vertexConsumer.vertex(posMatrix, beamWidth, -beamWidth, z1)
                    .color(r, g, b, a)
                    .texture(u1, 1.0f)
                    .light(15728880)
                    .normal(0, -1, 0)
                    .next();

            // Bottom left
            vertexConsumer.vertex(posMatrix, -beamWidth, -beamWidth, z0)
                    .color(r, g, b, a)
                    .texture(u0, 1.0f)
                    .light(15728880)
                    .normal(0, -1, 0)
                    .next();

            // Top left
            vertexConsumer.vertex(posMatrix, -beamWidth, beamWidth, z0)
                    .color(r, g, b, a)
                    .texture(u0, 0.0f)
                    .light(15728880)
                    .normal(0, -1, 0)
                    .next();

            // Draw perpendicular faces to make beam visible from all angles

            // Top face
            vertexConsumer.vertex(posMatrix, -beamWidth, beamWidth, z0)
                    .color(r, g, b, a)
                    .texture(u0, 0.0f)
                    .light(15728880)
                    .normal(0, 0, 1)
                    .next();

            vertexConsumer.vertex(posMatrix, beamWidth, beamWidth, z1)
                    .color(r, g, b, a)
                    .texture(u1, 0.0f)
                    .light(15728880)
                    .normal(0, 0, 1)
                    .next();

            vertexConsumer.vertex(posMatrix, beamWidth, beamWidth, z1)
                    .color(r, g, b, a)
                    .texture(u1, 0.0f)
                    .light(15728880)
                    .normal(0, 0, 1)
                    .next();

            vertexConsumer.vertex(posMatrix, -beamWidth, beamWidth, z0)
                    .color(r, g, b, a)
                    .texture(u0, 0.0f)
                    .light(15728880)
                    .normal(0, 0, 1)
                    .next();

            // Bottom face
            vertexConsumer.vertex(posMatrix, -beamWidth, -beamWidth, z0)
                    .color(r, g, b, a)
                    .texture(u0, 1.0f)
                    .light(15728880)
                    .normal(0, 0, -1)
                    .next();

            vertexConsumer.vertex(posMatrix, beamWidth, -beamWidth, z1)
                    .color(r, g, b, a)
                    .texture(u1, 1.0f)
                    .light(15728880)
                    .normal(0, 0, -1)
                    .next();

            vertexConsumer.vertex(posMatrix, beamWidth, -beamWidth, z1)
                    .color(r, g, b, a)
                    .texture(u1, 1.0f)
                    .light(15728880)
                    .normal(0, 0, -1)
                    .next();

            vertexConsumer.vertex(posMatrix, -beamWidth, -beamWidth, z0)
                    .color(r, g, b, a)
                    .texture(u0, 1.0f)
                    .light(15728880)
                    .normal(0, 0, -1)
                    .next();
        }

        // Restore the matrix
        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    /**
     * Helper method to create rotation matrix that aligns the Z axis with the given direction vector
     */
    private void alignToVector(MatrixStack matrices, Vec3d dir) {
        // Special case for vertical beams
        if (Math.abs(dir.y) > 0.99999) {
            // If pointing straight up or down
            matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(dir.y > 0 ? -90 : 90));
            return;
        }

        // Calculate rotation angles
        double horizontalLength = Math.sqrt(dir.x * dir.x + dir.z * dir.z);
        float yaw = (float)Math.toDegrees(Math.atan2(dir.x, dir.z));
        float pitch = (float)Math.toDegrees(Math.atan2(dir.y, horizontalLength));

        // Apply rotations in correct order
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(yaw));
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(-pitch));
    }

    @Override
    public Identifier getTexture(SolarBeamEntity entity) {
        return BEAM_TEXTURE;
    }
}