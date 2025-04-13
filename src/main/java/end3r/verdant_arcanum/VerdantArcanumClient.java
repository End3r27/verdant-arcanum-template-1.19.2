// VerdantArcanumClient.java
package end3r.verdant_arcanum;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import end3r.verdant_arcanum.VerdantArcanum;

@Environment(EnvType.CLIENT)
public class VerdantArcanumClient implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerdantArcanum.MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Verdant Arcanum Client...");

        // Register client-specific features here if needed
        // For example: EntityRenderers, Block/Item model registration, etc.

        LOGGER.info("Verdant Arcanum Client initialization complete!");
    }
}