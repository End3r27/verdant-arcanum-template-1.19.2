package end3r.verdant_arcanum.client.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.Vec3d;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class WindParticleHandler {
    private static Vec3d currentWindDirection = Vec3d.ZERO;
    private static float windStrength = 0;
    private static boolean windActive = false;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (windActive) {
                moveExistingParticles(client);
            }
        });
    }

    public static void setWindDirection(Vec3d direction, float strength) {
        currentWindDirection = direction;
        windStrength = strength;
        windActive = direction != Vec3d.ZERO && strength > 0;
    }

    @SuppressWarnings("unchecked")
    private static void moveExistingParticles(MinecraftClient client) {
        try {
            // Access the client's particle manager using reflection
            // This is necessary because the particle collection isn't normally accessible
            Field particlesField = client.particleManager.getClass().getDeclaredField("particles");
            particlesField.setAccessible(true);

            Map<Object, Collection<Particle>> particleMap =
                    (Map<Object, Collection<Particle>>) particlesField.get(client.particleManager);

            // Iterate through all particle collections
            for (Collection<Particle> particles : particleMap.values()) {
                for (Particle particle : particles) {
                    // Modify particle velocities based on wind
                    double speedFactor = 0.05 * windStrength;

                    // Access individual particle fields through reflection
                    Field velocityXField = getParticleField(particle, "velocityX", "field_21507");
                    Field velocityZField = getParticleField(particle, "velocityZ", "field_21509");

                    if (velocityXField != null && velocityZField != null) {
                        velocityXField.setAccessible(true);
                        velocityZField.setAccessible(true);

                        double currentVX = velocityXField.getDouble(particle);
                        double currentVZ = velocityZField.getDouble(particle);

                        // Add wind influence to existing velocity
                        velocityXField.setDouble(particle, currentVX + currentWindDirection.x * speedFactor);
                        velocityZField.setDouble(particle, currentVZ + currentWindDirection.z * speedFactor);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error while modifying existing particles: " + e.getMessage());
        }
    }

    private static Field getParticleField(Particle particle, String name, String obfName) {
        try {
            try {
                return particle.getClass().getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // Try superclass
                return particle.getClass().getSuperclass().getDeclaredField(name);
            }
        } catch (NoSuchFieldException e) {
            try {
                // Try obfuscated name
                return particle.getClass().getDeclaredField(obfName);
            } catch (NoSuchFieldException e2) {
                try {
                    // Try obfuscated name in superclass
                    return particle.getClass().getSuperclass().getDeclaredField(obfName);
                } catch (NoSuchFieldException e3) {
                    return null;
                }
            }
        }
    }
}