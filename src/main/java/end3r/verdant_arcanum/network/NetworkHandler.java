package end3r.verdant_arcanum.network;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.spell.tier2.SolarBloomSpell;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class NetworkHandler {
    // Register packet identifiers
    public static final Identifier BEAM_PARTICLE_PACKET_ID =
            new Identifier(VerdantArcanum.MOD_ID, "beam_particle");

    // Initialize network handling
    public static void init() {
        // Register packet handlers on client side
        registerClientReceivers();
    }

    // Register client packet receivers
    private static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(BEAM_PARTICLE_PACKET_ID, (client, handler, buf, responseSender) -> {
            BeamParticlePacket packet = BeamParticlePacket.read(buf);

            // Execute on main client thread
            client.execute(() -> {
                // Call the static method to spawn particles
                SolarBloomSpell.createBeamParticles(
                        client.world,
                        packet.getStartPos(),
                        packet.getDirection()
                );
            });
        });
    }

    // Send packet to a specific client
    public static void sendToClient(BeamParticlePacket packet, ServerPlayerEntity player) {
        var buf = PacketByteBufs.create();
        packet.write(buf);
        ServerPlayNetworking.send(player, BEAM_PARTICLE_PACKET_ID, buf);
    }
}