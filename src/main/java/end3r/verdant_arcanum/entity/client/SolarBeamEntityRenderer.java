package end3r.verdant_arcanum.entity.client;

import end3r.verdant_arcanum.entity.SolarBeamEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class SolarBeamEntityRenderer extends EntityRenderer<SolarBeamEntity> {
    private static final Identifier BEAM_TEXTURE = new Identifier("verdant_arcanum", "textures/entity/solar_beam.png");
    private static final RenderLayer BEAM_LAYER = RenderLayer.getEntityTranslucent(BEAM_TEXTURE);
    private static final Logger LOGGER = LogManager.getLogger(SolarBeamEntityRenderer.class);
    private static final int FULL_BRIGHTNESS = 15728880;

    public SolarBeamEntityRenderer(EntityRendererFactory.Context context) {
        super(context);

        LOGGER.info("SolarBeamEntityRenderer constructor called!");

    }

    @Override
    public void render(SolarBeamEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // Log rendering attempt at debug level
        LOGGER.debug("Attempting to render beam at {} to {}", entity.getStartPos(), entity.getEndPos());

        // Skip if no valid positions
        if (entity.getStartPos() == null || entity.getEndPos() == null) {
            LOGGER.warn("Skipping render - null positions");
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        matrices.push();

        // Get camera position for relative rendering
        if (this.dispatcher == null || this.dispatcher.camera == null) {
            LOGGER.warn("Camera or dispatcher is null, cannot render beam");
            matrices.pop();
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        Vec3d cameraPos = this.dispatcher.camera.getPos();

        // Calculate beam positions relative to camera
        Vec3d startPos = entity.getStartPos().subtract(cameraPos);
        Vec3d endPos = entity.getEndPos().subtract(cameraPos);

        // Calculate beam direction and length
        Vec3d beamVec = endPos.subtract(startPos);
        double beamLength = beamVec.length();

        if (beamLength < 0.1) {
            LOGGER.debug("Beam too short to render: {}", beamLength);
            matrices.pop();
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }

        // Calculate rotation to align with beam direction
        float yawAngle = (float) Math.toDegrees(Math.atan2(beamVec.z, beamVec.x)) - 90;
        float pitchAngle = (float) -Math.toDegrees(Math.atan2(beamVec.y,
                Math.sqrt(beamVec.x * beamVec.x + beamVec.z * beamVec.z)));

        // Move to start position
        matrices.translate(startPos.x, startPos.y, startPos.z);

        // Rotate to align with beam direction - using Vec3f.POSITIVE_Y/X instead of RotationAxis
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(yawAngle));
        matrices.multiply(Vec3f.POSITIVE_X.getDegreesQuaternion(pitchAngle));

        // Set up the beam dimensions
        float width = (float) entity.getBeamWidth();

        // Get vertex consumer for the beam layer
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(BEAM_LAYER);

        // Draw the beam as a quad
        // Front face
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), -width/2, 0, 0)
                .color(255, 255, 150, 200)  // Yellow-ish color
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHTNESS)
                .normal(matrices.peek().getNormalMatrix(), 0, 1, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), width/2, 0, 0)
                .color(255, 255, 150, 200)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHTNESS)
                .normal(matrices.peek().getNormalMatrix(), 0, 1, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), width/2, (float)beamLength, 0)
                .color(255, 255, 150, 200)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHTNESS)
                .normal(matrices.peek().getNormalMatrix(), 0, 1, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), -width/2, (float)beamLength, 0)
                .color(255, 255, 150, 200)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHTNESS)
                .normal(matrices.peek().getNormalMatrix(), 0, 1, 0)
                .next();

        // Add rotated versions to make the beam visible from all angles
        // Rotate 90 degrees and draw another face
        matrices.multiply(Vec3f.POSITIVE_Y.getDegreesQuaternion(90));
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), -width/2, 0, 0)
                .color(255, 255, 150, 200)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHTNESS)
                .normal(matrices.peek().getNormalMatrix(), 0, 1, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), width/2, 0, 0)
                .color(255, 255, 150, 200)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHTNESS)
                .normal(matrices.peek().getNormalMatrix(), 0, 1, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), width/2, (float)beamLength, 0)
                .color(255, 255, 150, 200)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHTNESS)
                .normal(matrices.peek().getNormalMatrix(), 0, 1, 0)
                .next();
        vertexConsumer.vertex(matrices.peek().getPositionMatrix(), -width/2, (float)beamLength, 0)
                .color(255, 255, 150, 200)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHTNESS)
                .normal(matrices.peek().getNormalMatrix(), 0, 1, 0)
                .next();

        LOGGER.debug("Beam rendered successfully from {} to {}", startPos, endPos);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(SolarBeamEntity entity) {
        return BEAM_TEXTURE;
    }
}