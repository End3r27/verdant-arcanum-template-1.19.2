package end3r.verdant_arcanum.registry;

import end3r.verdant_arcanum.spell.FlameSpell;
import end3r.verdant_arcanum.spell.Spell;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for all available spells in the mod.
 */
public class SpellRegistry {
    private static final Map<String, Spell> SPELLS = new HashMap<>();

    // Initialize spells
    static {
        registerSpell(new FlameSpell());
        // Register other spells here as they are developed
    }

    /**
     * Register a spell in the registry.
     * @param spell The spell to register
     */
    public static void registerSpell(Spell spell) {
        SPELLS.put(spell.getType().toLowerCase(), spell);
    }

    /**
     * Get a spell by its type.
     * @param type The spell type
     * @return The spell, or null if not found
     */
    public static Spell getSpell(String type) {
        return SPELLS.get(type.toLowerCase());
    }

    /**
     * Get a spell from an essence ID.
     * @param essenceId The full registry ID of the spell essence
     * @return The spell, or null if not found
     */
    public static Spell getSpellFromEssenceId(String essenceId) {
        // Map essence IDs to spell types
        if (essenceId.equals("verdant_arcanum:spell_essence_flame")) {
            return getSpell("flame");
        }
        // Add more mappings as spells are developed

        return null;
    }
}