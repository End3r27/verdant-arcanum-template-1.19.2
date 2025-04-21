package end3r.verdant_arcanum.event;

import end3r.verdant_arcanum.registry.EventRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
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

    // BossBar for event progress display
    private ServerBossBar eventBossBar;
    // Track event progress
    private int eventElapsedTicks = 0;

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
                        // Debug code was empty in original
                    }
                }

                // Tick the event
                currentEvent.tick(world);

                // Update event progress
                eventElapsedTicks++;

                // Update boss bar progress if it exists
                if (eventBossBar != null) {
                    // Calculate remaining progress (1.0 -> 0.0)
                    int eventDuration = currentEvent.getDuration();
                    float progress = Math.max(0.0f, Math.min(1.0f, (float)(eventDuration - eventElapsedTicks) / eventDuration));
                    eventBossBar.setPercent(progress);
                }

                // Check if it's complete or if time has elapsed
                if (currentEvent.isComplete()) {
                    // Notify players the event is ending
                    for (PlayerEntity player : world.getPlayers()) {
                        String eventName = formatEventName(currentEvent.getId().getPath());
                        player.sendMessage(Text.literal("The " + eventName + " is subsiding..."), true);
                    }

                    // Remove boss bar
                    if (eventBossBar != null) {
                        eventBossBar.clearPlayers();
                        eventBossBar = null;
                    }

                    currentEvent = null;
                    eventElapsedTicks = 0;
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

                // Remove boss bar if there was an error
                if (eventBossBar != null) {
                    eventBossBar.clearPlayers();
                    eventBossBar = null;
                }

                // Prevent buggy events from crashing the server - cancel the event
                currentEvent = null;
                eventElapsedTicks = 0;
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
            eventElapsedTicks = 0;
            currentEvent.start(world);
            createEventBossBar(world, EventRegistry.STRONG_WINDS_ID);
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
            eventElapsedTicks = 0;
            currentEvent.start(world);
            createEventBossBar(world, EventRegistry.OVERGROWTH_ID);
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
            eventElapsedTicks = 0;
            currentEvent.start(world);
            createEventBossBar(world, EventRegistry.FIRE_RAIN_ID);
        } else {
            // Debug message if event couldn't be found
            for (PlayerEntity player : world.getPlayers()) {
                if (player.hasPermissionLevel(2)) {
                    player.sendMessage(Text.literal("[Debug] Failed to start Fire Rain event: not found in registry"), false);
                }
            }
        }
    }

    /**
     * Creates and configures a boss bar for the current event
     * @param world The server world
     * @param eventId The event identifier
     */
    private void createEventBossBar(ServerWorld world, Identifier eventId) {
        // Remove existing boss bar if there is one
        if (eventBossBar != null) {
            eventBossBar.clearPlayers();
        }

        String eventName = formatEventName(eventId.getPath());
        Text bossBarText = Text.literal(eventName); // Event name as text

        // Create a new boss bar with color based on event type
        eventBossBar = new ServerBossBar(
                bossBarText,
                getBossBarColorForEvent(eventId),
                BossBar.Style.PROGRESS
        );

        // Add all players in the dimension to see the boss bar
        for (PlayerEntity player : world.getPlayers()) {
            eventBossBar.addPlayer((net.minecraft.server.network.ServerPlayerEntity) player);
        }
    }

    /**
     * Determines the appropriate boss bar color based on event type
     * @param eventId The event identifier
     * @return BossBar.Color for the event
     */
    private BossBar.Color getBossBarColorForEvent(Identifier eventId) {
        if (eventId.equals(EventRegistry.STRONG_WINDS_ID)) {
            return BossBar.Color.WHITE; // White for wind
        } else if (eventId.equals(EventRegistry.OVERGROWTH_ID)) {
            return BossBar.Color.GREEN; // Green for plants/growth
        } else if (eventId.equals(EventRegistry.FIRE_RAIN_ID)) {
            return BossBar.Color.RED; // Red for fire
        } else {
            return BossBar.Color.PURPLE; // Default purple for unknown magical events
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
        eventElapsedTicks = 0;

        event.start(world);

        // Create boss bar for the event
        createEventBossBar(world, event.getId());

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

            // Clear the boss bar
            if (eventBossBar != null) {
                eventBossBar.clearPlayers();
                eventBossBar = null;
            }

            // Clean up event
            currentEvent = null;
            eventElapsedTicks = 0;
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