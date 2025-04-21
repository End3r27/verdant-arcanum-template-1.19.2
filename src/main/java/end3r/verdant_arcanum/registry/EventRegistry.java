package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.event.CustomWorldEvent;
import net.minecraft.util.Identifier;


import java.util.HashMap;
import java.util.Map;

public class EventRegistry {
    private static final Map<Identifier, CustomWorldEvent> REGISTERED_EVENTS = new HashMap<>();

    public static final Identifier STRONG_WINDS_ID = new Identifier("verdant_arcanum", "strong_winds");

    public static void registerAll() {
        register(STRONG_WINDS_ID, new end3r.verdant_arcanum.event.StrongWindsEvent());

        // Add more as you create them:
        // register(new Identifier("verdant_arcanum", "fire_rain"), new FireRainEvent());
        // register(new Identifier("verdant_arcanum", "end_veil"), new EndVeilEvent());
        // register(new Identifier("verdant_arcanum", "nature_overgrowth"), new NatureOvergrowthEvent());
    }

    private static void register(Identifier id, CustomWorldEvent event) {
        REGISTERED_EVENTS.put(id, event);
    }

    public static CustomWorldEvent get(Identifier id) {
        return REGISTERED_EVENTS.get(id);
    }

    public static Map<Identifier, CustomWorldEvent> getAll() {
        return REGISTERED_EVENTS;
    }
}