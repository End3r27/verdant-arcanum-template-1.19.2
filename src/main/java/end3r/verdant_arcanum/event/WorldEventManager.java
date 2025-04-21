package end3r.verdant_arcanum.event;

import end3r.verdant_arcanum.registry.EventRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

import java.util.Random;

public class WorldEventManager {
    private static final int CHECK_INTERVAL = 20 * 30; // Check every 30 seconds
    private int tickCounter = 0;

    private CustomWorldEvent currentEvent;
    private static final Random RANDOM = new Random();

    private static final WorldEventManager INSTANCE = new WorldEventManager();

    public static WorldEventManager getInstance() {
        return INSTANCE;
    }

    public void tick(ServerWorld world) {

        if (currentEvent != null && currentEvent.isComplete()) {
            System.out.println("Removing completed event: " + currentEvent.getId());
            currentEvent = null;
        }

        tickCounter++;

        if (currentEvent != null) {
            currentEvent.tick(world);
            if (currentEvent.isComplete()) {
                currentEvent = null;
            }
            return;
        }

        // Try triggering a new event
        if (tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;

            if (RANDOM.nextFloat() < 0.25f) {
                startStrongWinds(world);
            }
        }
    }

    private void startStrongWinds(ServerWorld world) {
        CustomWorldEvent event = EventRegistry.get(EventRegistry.STRONG_WINDS_ID);
        if (event != null) {
            currentEvent = event;
            currentEvent.start(world);
        }
    }

    public CustomWorldEvent getCurrentEvent() {
        return currentEvent;
    }

    /**
     * Start a custom world event
     * @param world The server world
     * @param event The event to start
     */
    public void startEvent(ServerWorld world, CustomWorldEvent event) {
        // If there's already an event running, stop it first
        if (currentEvent != null) {
            stopCurrentEvent(world);
        }

        this.currentEvent = event;
        event.start(world);
    }

    /**
     * Stop an event by its identifier
     * @param world The server world
     * @param eventId The identifier of the event to stop
     * @return true if an event was stopped, false otherwise
     */
    public boolean stopEvent(ServerWorld world, Identifier eventId) {
        if (currentEvent != null && currentEvent.getId().equals(eventId)) {
            stopCurrentEvent(world);
            return true;
        }
        return false;
    }

    /**
     * Stops the currently running event
     * @param world The server world
     */
    private void stopCurrentEvent(ServerWorld world) {
        if (currentEvent != null) {
            Identifier eventId = currentEvent.getId();

            // Notify players that the event is ending
            if (world != null) {
                // Create a user-friendly notification based on the event type
                String eventName = formatEventName(eventId.getPath());
                world.getPlayers().forEach(player ->
                        player.sendMessage(Text.literal("The " + eventName + " has been stopped."), true)
                );
            }

            // Clean up event
            currentEvent = null;
            tickCounter = 0; // Reset tick counter
        }
    }

    /**
     * Stop all active events
     * @param world The server world
     * @return true if an event was stopped, false otherwise
     */
    public boolean stopAllEvents(ServerWorld world) {
        if (currentEvent != null) {
            stopCurrentEvent(world);
            return true;
        }
        return false;
    }

    /**
     * Format event ID path into human-readable text
     * For example: "strong_winds" becomes "Strong Winds"
     */
    private String formatEventName(String eventPath) {
        String[] words = eventPath.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return result.toString().trim();
    }
}