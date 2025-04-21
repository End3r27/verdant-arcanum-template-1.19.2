package end3r.verdant_arcanum.registry;


import net.minecraft.util.registry.Registry;

import static end3r.verdant_arcanum.event.StrongWindsEvent.SOUND_ID;
import static end3r.verdant_arcanum.event.StrongWindsEvent.STRONG_WIND_SOUND;


public class ModSoundRegistry {
    public static void registerSounds() {
        net.minecraft.util.registry.Registry.register(Registry.SOUND_EVENT, SOUND_ID, STRONG_WIND_SOUND);
    }
}
