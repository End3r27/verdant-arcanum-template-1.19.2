package end3r.verdant_arcanum.magic;

/**
 * Client-side storage for mana data received from server
 */
public class ClientManaData {
    private static float currentMana = 0;
    private static int maxMana = 100;

    public static void setMana(float current, int max) {
        currentMana = current;
        maxMana = max;
    }

    public static float getCurrentMana() {
        return currentMana;
    }

    public static int getMaxMana() {
        return maxMana;
    }

    public static float getManaPercentage() {
        return currentMana / maxMana;
    }
    public static void resetOrMarkInactive() {
        // Don't reset current mana value when items are removed
        // Just make sure max mana returns to the default value
        maxMana = ManaSystem.DEFAULT_MAX_MANA;
        // Cap current mana at the new max if needed
        if (currentMana > maxMana) {
            currentMana = maxMana;
        }
    }
}