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
}