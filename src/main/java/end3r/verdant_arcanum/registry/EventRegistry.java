package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.event.CustomWorldEvent;
import end3r.verdant_arcanum.event.EndVeilEvent;
import end3r.verdant_arcanum.event.FireRainEvent;
import end3r.verdant_arcanum.event.OvergrowthEvent;
import net.minecraft.util.Identifier;


import java.util.HashMap;
import java.util.Map;

public class EventRegistry {
    private static final Map<Identifier, CustomWorldEvent> REGISTERED_EVENTS = new HashMap<>();

    public static final Identifier STRONG_WINDS_ID = new Identifier("verdant_arcanum", "strong_winds");
    public static final Identifier OVERGROWTH_ID = new Identifier("verdant_arcanum", "overgrowth");
    public static final Identifier FIRE_RAIN_ID = new Identifier("verdant_arcanum", "fire_rain");
    public static final Identifier END_VEIL_ID = new Identifier("verdant_arcanum", "end_veil");

    public static void registerAll() {
        register(STRONG_WINDS_ID, new end3r.verdant_arcanum.event.StrongWindsEvent());
        register(OVERGROWTH_ID, new end3r.verdant_arcanum.event.OvergrowthEvent());
        register(FIRE_RAIN_ID, new end3r.verdant_arcanum.event.FireRainEvent());
        register(END_VEIL_ID, new end3r.verdant_arcanum.event.EndVeilEvent());
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
