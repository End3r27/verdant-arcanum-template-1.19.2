package end3r.verdant_arcanum.event;

import end3r.verdant_arcanum.registry.EventRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import java.util.Random;

public class WorldEventManager {
    private static final int CHECK_INTERVAL = 20 * 30 * 20; // Check every 10 minutes (20 ticks/sec * 60 sec * 10 min)
    private static final int NETHER_CHECK_INTERVAL = 20 * 30 * 20; // Also 10 minutes for Nether
    private int tickCounter = 0;
    private int netherTickCounter = 0;

    private CustomWorldEvent currentEvent;
    private static final Random RANDOM = new Random();

    private static final WorldEventManager INSTANCE = new WorldEventManager();

    public static WorldEventManager getInstance() {
        return INSTANCE;
    }

    public void tick(ServerWorld world) {
        if (currentEvent != null) {
            try {
                // Debug event ticking
                if (world.getTime() % 100 == 0) {
                    for (PlayerEntity player : world.getPlayers()) {

                        }
                    }

                
                // Tick the event
                currentEvent.tick(world);
                
                // Check if it's complete
                if (currentEvent.isComplete()) {
                    // Notify players the event is ending
                    for (PlayerEntity player : world.getPlayers()) {
                        String eventName = formatEventName(currentEvent.getId().getPath());
                        player.sendMessage(Text.literal("The " + eventName + " is subsiding..."), true);
                    }
                    
                    currentEvent = null;
                }
                
                // Skip the rest of the method since we have an active event
                return;
            } catch (Exception e) {
                // Log any errors in the event ticking
                for (PlayerEntity player : world.getPlayers()) {
                    if (player.hasPermissionLevel(2)) { // Op level 2+
                        player.sendMessage(Text.literal("[Debug] Error in event tick: " + e.getMessage()), false);
                    }
                }
                
                // Prevent buggy events from crashing the server - cancel the event
                currentEvent = null;
            }
        }

        tickCounter++;
        
        // Separate counter for nether events
        if (world.getRegistryKey() == World.NETHER) {
            netherTickCounter++;
        }

        // Only trigger Overworld events in the Overworld
        if (world.getRegistryKey() == World.OVERWORLD && tickCounter >= CHECK_INTERVAL) {
            tickCounter = 0;

            float eventChance = RANDOM.nextFloat();
            if (eventChance < 0.25f) {
                // 50% chance for strong winds, 50% chance for overgrowth when an event triggers
                String eventType = RANDOM.nextBoolean() ? "Strong Winds" : "Overgrowth";
                
                // Notify players about the new event
                for (PlayerEntity player : world.getPlayers()) {
                    player.sendMessage(Text.literal("A magical " + eventType.toLowerCase() + " begins to manifest..."), true);
                }
                if (RANDOM.nextBoolean()) {
                    startStrongWinds(world);
                } else {
                    startOvergrowth(world);
                }
            }
        }
        
        // Only trigger Nether events in the Nether
        if (world.getRegistryKey() == World.NETHER && netherTickCounter >= NETHER_CHECK_INTERVAL) {
            netherTickCounter = 0;
            
            float eventChance = RANDOM.nextFloat();
            if (eventChance < 0.25f) {
                // Notify players about the new event
                for (PlayerEntity player : world.getPlayers()) {
                    player.sendMessage(Text.literal("A magical fire rain begins to manifest..."), true);
                }
                startFireRain(world);
            }
        }
    }

    private void startStrongWinds(ServerWorld world) {
        CustomWorldEvent event = EventRegistry.get(EventRegistry.STRONG_WINDS_ID);
        if (event != null) {
            currentEvent = event;
            currentEvent.start(world);
        } else {
            // Debug message if event couldn't be found
            for (PlayerEntity player : world.getPlayers()) {
                if (player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("[Debug] Failed to start Strong Winds event: not found in registry"), false);
                }
            }
        }
    }
    
    private void startOvergrowth(ServerWorld world) {
        CustomWorldEvent event = EventRegistry.get(EventRegistry.OVERGROWTH_ID);
        if (event != null) {
            currentEvent = event;
            currentEvent.start(world);
        } else {
            // Debug message if event couldn't be found
            for (PlayerEntity player : world.getPlayers()) {
                if (player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("[Debug] Failed to start Overgrowth event: not found in registry"), false);
                }
            }
        }
    }
    
    private void startFireRain(ServerWorld world) {
        // Only start fire rain in the Nether
        if (world.getRegistryKey() != World.NETHER) {
            return;
        }
        
        CustomWorldEvent event = EventRegistry.get(EventRegistry.FIRE_RAIN_ID);
        if (event != null) {
            currentEvent = event;
            currentEvent.start(world);
        } else {
            // Debug message if event couldn't be found
            for (PlayerEntity player : world.getPlayers()) {
                if (player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("[Debug] Failed to start Fire Rain event: not found in registry"), false);
                }
            }
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
        
        // Notify players about the new event
        String eventName = formatEventName(event.getId().getPath());
        for (PlayerEntity player : world.getPlayers()) {
            player.sendMessage(Text.literal("A magical " + eventName.toLowerCase() + " begins to manifest..."), true);
        }
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
            
            // Reset Nether tick counter if it was a Nether event
            if (eventId.equals(EventRegistry.FIRE_RAIN_ID)) {
                netherTickCounter = 0;
            }
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
