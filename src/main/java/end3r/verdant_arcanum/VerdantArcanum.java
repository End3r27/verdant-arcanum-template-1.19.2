// VerdantArcanum.java
package end3r.verdant_arcanum;

import end3r.verdant_arcanum.registry.ModRegistry;
import end3r.verdant_arcanum.spell.SolarBloomSpell;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerdantArcanum implements ModInitializer {
	// Define mod ID constant
	public static final String MOD_ID = "verdant_arcanum";

	// Logger for mod
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Verdant Arcanum...");

		// Register all mod components
		ModRegistry.registerAll();

		ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);



		LOGGER.info("Verdant Arcanum initialization complete!");
	}

	private void onServerTick(MinecraftServer server) {
		for (ServerWorld world : server.getWorlds()) {
			SolarBloomSpell.tickActiveSpells(world, null);
		}
	}

}