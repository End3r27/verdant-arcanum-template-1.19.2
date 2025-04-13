package end3r.verdant_arcanum.spell;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Interface for all spell implementations.
 */
public interface Spell {
    /**
     * Get the type name of this spell.
     * @return The spell's type as a string
     */
    String getType();

    /**
     * Get the mana cost for casting this spell.
     * @return The mana cost as an integer
     */
    int getManaCost();

    /**
     * Cast the spell (server-side effect).
     * @param world The world
     * @param player The player casting the spell
     * @throws SpellCastException If the spell cast fails in a way that should not consume mana
     */
    void cast(World world, PlayerEntity player);

    /**
     * Play client-side visual effects for the spell.
     * @param world The world
     * @param player The player casting the spell
     */
    void playClientEffects(World world, PlayerEntity player);

    /**
     * Play client-side visual effects when the spell fails.
     * Default implementation does nothing.
     * @param world The world
     * @param player The player casting the spell
     */
    default void playFailureEffects(World world, PlayerEntity player) {
        // Default implementation does nothing
    }
}