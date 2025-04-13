// VerdantArcanum.java
package end3r.verdant_arcanum;

import end3r.verdant_arcanum.registry.ModRegistry;
import net.fabricmc.api.ModInitializer;
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

		LOGGER.info("Verdant Arcanum initialization complete!");
	}
}