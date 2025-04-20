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
import net.minecraft.util.math.Matrix3f;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolarBeamEntityRenderer extends EntityRenderer<SolarBeamEntity> {
    private static final Identifier BEAM_TEXTURE = new Identifier("verdant_arcanum", "textures/entity/solar_beam.png");
    private static final RenderLayer BEAM_LAYER = RenderLayer.getEntityTranslucent(BEAM_TEXTURE);
    private static final Logger LOGGER = LoggerFactory.getLogger(SolarBeamEntityRenderer.class);
    private static final int FULL_BRIGHTNESS = 15728880;

    public SolarBeamEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(SolarBeamEntity entity, float entityYaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {

        LOGGER.info("SolarBeamEntityRenderer.render STARTING for entity ID: {}", entity.getId());

        float width = entity.getBeamWidth() * 5.0f;  // Make it 5x wider

        int color = 0xFF00FF;  // Bright magenta



        if (entity == null) {
            LOGGER.error("Entity is null in render method");
            return;
        }

        LOGGER.info("SolarBeamEntityRenderer.render called for entity ID: {}", entity.getId());


        // Extract position information from the entity
        Vec3d startPos = entity.getStart();
        Vec3d endPos = entity.getEnd();

        // Ensure positions are not null and log them
        if (startPos == null || endPos == null) {
            LOGGER.warn("Unexpected null positions: startPos={}, endPos={}", startPos, endPos);
            return;
        }

        // Log information about the entity being rendered
        LOGGER.debug("Rendering SolarBeamEntity: id={}, start={}, end={}", entity.getId(), startPos, endPos);

        // Calculate beam vector
        Vec3d beamVec = endPos.subtract(startPos);

        // Check if the beam vector has appropriate length
        if (beamVec.lengthSquared() < 0.001) {
            LOGGER.warn("Invalid beam vector with length={}, cannot render", beamVec.length());
            return;
        }

        // Normalize the beam vector for calculations
        beamVec = beamVec.normalize();
        LOGGER.trace("Normalized beam vector: {}", beamVec);

        // Get render position adjusted by camera
        Vec3d cameraPos = this.dispatcher.camera.getPos();
        Vec3d renderStartPos = startPos.subtract(cameraPos);
        Vec3d renderEndPos = endPos.subtract(cameraPos);
        LOGGER.trace("Camera-relative positions: start={}, end={}", renderStartPos, renderEndPos);

        // Calculate the beam width
        width = entity.getBeamWidth();
        LOGGER.trace("Beam width: {}", width);

        // Begin rendering process
        matrices.push();
        
        try {
            // Set up vertex consumer for the beam
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(BEAM_LAYER);
            if (vertexConsumer == null) {
                LOGGER.error("Failed to get vertex consumer for beam layer");
                return;
            }

            // Calculate perpendicular vectors for beam quad rendering
            Vec3d perpendicular1 = calculatePerpendicular(beamVec).multiply(width / 2.0);
            Vec3d perpendicular2 = beamVec.crossProduct(perpendicular1).multiply(width / 2.0);
            LOGGER.trace("Perpendicular vectors: p1={}, p2={}", perpendicular1, perpendicular2);

            // Get matrix transformations
            Matrix4f posMatrix = matrices.peek().getPositionMatrix();
            Matrix3f normalMatrix = matrices.peek().getNormalMatrix();

            // Render the beam as two perpendicular quads to make it visible from all angles
            LOGGER.trace("Rendering first beam quad");
            renderBeamQuad(vertexConsumer, posMatrix, normalMatrix, renderStartPos, renderEndPos,
                    perpendicular1, perpendicular2, 0, 1, 0, 1, FULL_BRIGHTNESS);

            LOGGER.trace("Rendering second beam quad");
            renderBeamQuad(vertexConsumer, posMatrix, normalMatrix, renderStartPos, renderEndPos,
                    perpendicular2, perpendicular1.negate(), 0, 1, 0, 1, FULL_BRIGHTNESS);
            
            LOGGER.debug("Successfully rendered beam quads");
        } catch (Exception e) {
            LOGGER.error("Exception during beam rendering: {}", e.getMessage(), e);
        } finally {
            matrices.pop();
        }

        super.render(entity, entityYaw, tickDelta, matrices, vertexConsumers, light);
    }

    private Vec3d calculatePerpendicular(Vec3d vec) {
        // Find a vector perpendicular to the beam direction
        Vec3d perpendicular;
        if (Math.abs(vec.x) < 0.1 && Math.abs(vec.z) < 0.1) {
            // Special case for mostly vertical beams
            LOGGER.trace("Using fallback perpendicular for vertical beam");
            perpendicular = new Vec3d(1, 0, 0);
        } else {
            perpendicular = new Vec3d(-vec.z, 0, vec.x).normalize();
        }
        
        // Validate the perpendicular vector
        if (perpendicular.lengthSquared() < 0.9 || perpendicular.lengthSquared() > 1.1) {
            LOGGER.warn("Calculated perpendicular vector has abnormal length: {}", perpendicular.length());
        }
        
        return perpendicular;
    }

    private void renderBeamQuad(VertexConsumer vertexConsumer, Matrix4f posMatrix, Matrix3f normalMatrix,
                                Vec3d start, Vec3d end, Vec3d perpendicular1, Vec3d perpendicular2,
                                float minU, float maxU, float minV, float maxV, int light) {
        try {
            // Calculate vertex positions
            Vec3d v1 = start.add(perpendicular1);
            Vec3d v2 = start.subtract(perpendicular1);
            Vec3d v3 = end.subtract(perpendicular1);
            Vec3d v4 = end.add(perpendicular1);
            
            // Render a quad from start to end with the given perpendicular vectors
            LOGGER.trace("Quad vertices: v1={}, v2={}, v3={}, v4={}", v1, v2, v3, v4);
            
            addVertex(vertexConsumer, posMatrix, normalMatrix, v1, minU, minV, light);
            addVertex(vertexConsumer, posMatrix, normalMatrix, v2, maxU, minV, light);
            addVertex(vertexConsumer, posMatrix, normalMatrix, v3, maxU, maxV, light);
            addVertex(vertexConsumer, posMatrix, normalMatrix, v4, minU, maxV, light);
        } catch (Exception e) {
            LOGGER.error("Failed to render beam quad: {}", e.getMessage());
        }
    }

    private void addVertex(VertexConsumer vertexConsumer, Matrix4f posMatrix, Matrix3f normalMatrix,
                           Vec3d pos, float u, float v, int light) {
        if (vertexConsumer == null) {
            LOGGER.error("Null vertex consumer in addVertex");
            return;
        }
        
        if (Float.isNaN((float)pos.x) || Float.isNaN((float)pos.y) || Float.isNaN((float)pos.z)) {
            LOGGER.error("NaN position in vertex: {}", pos);
            return;
        }
        
        try {
            vertexConsumer
                    .vertex(posMatrix, (float)pos.x, (float)pos.y, (float)pos.z)
                    .color(255, 255, 255, 230) // Slightly transparent white
                    .texture(u, v)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(light)
                    .normal(normalMatrix, 0.0F, 1.0F, 0.0F)
                    .next();
        } catch (Exception e) {
            LOGGER.error("Exception adding vertex: {}", e.getMessage());
        }
    }

    @Override
    public Identifier getTexture(SolarBeamEntity entity) {
        return BEAM_TEXTURE;
    }
}
