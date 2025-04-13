package end3r.verdant_arcanum.spell;

/**
 * Exception thrown when a spell cast fails in a way that
 * should not consume mana.
 */
public class SpellCastException extends RuntimeException {
    public SpellCastException(String message) {
        super(message);
    }
}