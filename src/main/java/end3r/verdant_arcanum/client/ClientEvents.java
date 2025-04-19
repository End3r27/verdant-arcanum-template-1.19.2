package end3r.verdant_arcanum.client;

import end3r.verdant_arcanum.magic.ManaParticleSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

public class ClientEvents {
    public static void registerClientEvents() {
        // Register a tick handler to update mana particles
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.world != null && !client.isPaused()) {
                // Update mana particles for the client player
                ManaParticleSystem.getInstance().updateManaParticles(client.player);
            }
        });
    }
}