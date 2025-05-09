package end3r.verdant_arcanum.magic;

import end3r.verdant_arcanum.magic.ManaSystem;

public class ClientManaData {
    public static float currentMana = 0;
    public static int maxMana = 100;
    private static float regenMultiplier = 1.0f; // Add this field

    public static void setMana(float current, int max, float multiplier) {
        currentMana = current;
        maxMana = max;
        regenMultiplier = multiplier; // Store the multiplier
    }

    public static float getCurrentMana() {
        return currentMana;
    }

    public static int getMaxMana() {
        return maxMana;
    }

    public static float getRegenMultiplier() {
        return regenMultiplier;
    }

    public static float getManaPercentage() {
        if (maxMana <= 0) return 0; // Avoid division by zero
        // Calculate percentage and clamp between 0 and 1
        return Math.max(0, Math.min(1, currentMana / (float)maxMana));
    }


    public static void resetOrMarkInactive() {
        // Don't reset current mana value when items are removed
        // Just make sure max mana returns to the default value
        maxMana = ManaSystem.DEFAULT_MAX_MANA;
        regenMultiplier = 1.0f; // Reset multiplier too
        // Cap current mana at the new max if needed
        if (currentMana > maxMana) {
            currentMana = maxMana;
        }
    }
}