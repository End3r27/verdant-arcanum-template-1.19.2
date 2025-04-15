package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.screen.MagicHiveScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    // Magic Hive screen handler
    public static ScreenHandlerType<MagicHiveScreenHandler> MAGIC_HIVE_SCREEN_HANDLER;

    public static void register() {
        // Register the Magic Hive screen handler
        MAGIC_HIVE_SCREEN_HANDLER = ScreenHandlerRegistry.registerSimple(
                new Identifier(VerdantArcanum.MOD_ID, "magic_hive"),
                MagicHiveScreenHandler::new
        );
    }
}