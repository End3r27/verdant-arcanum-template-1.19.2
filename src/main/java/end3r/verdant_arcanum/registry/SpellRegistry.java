package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.spell.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class SpellRegistry {
    // Map to store spell types by their ID
    private static final Map<String, Spell> SPELLS = new HashMap<>();

    // Initialize spells
    static {
        // Register the spells
        register("flame", new FlameSpell());
        register("blink", new BlinkSpell());
        register("rootgrasp", new RootgraspSpell());
        register("gust", new GustSpell());
        register("breezevine", new BreezevineSpell());
        register("solarbloom", new SolarBloomSpell());
        register("flamespiral", new FlameSpiralSpell());


    }

    private static void register(String type, Spell spell) {
        SPELLS.put(type, spell);
    }

    public static Spell getSpell(String type) {
        return SPELLS.get(type);
    }

    /**
     * Get a spell based on the essence item ID.
     * @param essenceId The item ID of the spell essence
     * @return The corresponding spell, or null if not found
     */
    public static Spell getSpellFromEssenceId(String essenceId) {
        // Extract the spell type from the essence ID
        // Format expected: "verdant_arcanum:spell_essence_flame"
        if (essenceId != null && essenceId.contains("spell_essence_")) {
            String[] parts = essenceId.split("spell_essence_");
            if (parts.length > 1) {
                // Get the spell type from the end of the ID
                return getSpell(parts[1]);
            }
        }
        return null;
    }
}