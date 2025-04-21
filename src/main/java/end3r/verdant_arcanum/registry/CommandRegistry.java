package end3r.verdant_arcanum.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import end3r.verdant_arcanum.event.StrongWindsEvent;
import end3r.verdant_arcanum.event.OvergrowthEvent;
import end3r.verdant_arcanum.event.WorldEventManager;

public class CommandRegistry {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Basic command (original functionality)
        dispatcher.register(CommandManager.literal("startwind")
                .requires(source -> source.hasPermissionLevel(2)) // OP level 2+
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerWorld world = source.getWorld();

                    StrongWindsEvent event = new StrongWindsEvent();
                    WorldEventManager.getInstance().startEvent(world, event);

                    source.sendFeedback(Text.literal("🌬️ Manually started Strong Winds event."), true);
                    return 1;
                })

                // Add duration parameter (in seconds)
                .then(CommandManager.literal("duration")
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 120))
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerWorld world = source.getWorld();
                                    int seconds = IntegerArgumentType.getInteger(context, "seconds");

                                    StrongWindsEvent event = new StrongWindsEvent();
                                    event.setDuration(seconds * 20); // Convert to ticks
                                    WorldEventManager.getInstance().startEvent(world, event);

                                    source.sendFeedback(Text.literal("🌬️ Started Strong Winds event with " + seconds + " seconds duration."), true);
                                    return 1;
                                })
                        )
                )

                // Add direction parameter
                .then(CommandManager.literal("direction")
                        .then(CommandManager.argument("direction", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    builder.suggest("north");
                                    builder.suggest("south");
                                    builder.suggest("east");
                                    builder.suggest("west");
                                    builder.suggest("northeast");
                                    builder.suggest("northwest");
                                    builder.suggest("southeast");
                                    builder.suggest("southwest");
                                    builder.suggest("random");
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerWorld world = source.getWorld();
                                    String direction = StringArgumentType.getString(context, "direction");

                                    StrongWindsEvent event = new StrongWindsEvent();
                                    setWindDirection(event, direction);
                                    WorldEventManager.getInstance().startEvent(world, event);

                                    source.sendFeedback(Text.literal("🌬️ Started Strong Winds event blowing from the " + direction + "."), true);
                                    return 1;
                                })

                                // Combined direction and duration
                                .then(CommandManager.literal("duration")
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 120))
                                                .executes(context -> {
                                                    ServerCommandSource source = context.getSource();
                                                    ServerWorld world = source.getWorld();
                                                    String direction = StringArgumentType.getString(context, "direction");
                                                    int seconds = IntegerArgumentType.getInteger(context, "seconds");

                                                    StrongWindsEvent event = new StrongWindsEvent();
                                                    setWindDirection(event, direction);
                                                    event.setDuration(seconds * 20); // Convert to ticks
                                                    WorldEventManager.getInstance().startEvent(world, event);

                                                    source.sendFeedback(Text.literal("🌬️ Started Strong Winds event blowing from the " + direction + " for " + seconds + " seconds."), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )

                // Add strength parameter
                .then(CommandManager.literal("strength")
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 3))
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerWorld world = source.getWorld();
                                    int strength = IntegerArgumentType.getInteger(context, "level");

                                    StrongWindsEvent event = new StrongWindsEvent();
                                    event.setStrength(strength);
                                    WorldEventManager.getInstance().startEvent(world, event);

                                    String strengthText = strength == 1 ? "mild" : strength == 2 ? "moderate" : "severe";
                                    source.sendFeedback(Text.literal("🌬️ Started " + strengthText + " Strong Winds event."), true);
                                    return 1;
                                })

                                // Combined strength and duration
                                .then(CommandManager.literal("duration")
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 120))
                                                .executes(context -> {
                                                    ServerCommandSource source = context.getSource();
                                                    ServerWorld world = source.getWorld();
                                                    int strength = IntegerArgumentType.getInteger(context, "level");
                                                    int seconds = IntegerArgumentType.getInteger(context, "seconds");

                                                    StrongWindsEvent event = new StrongWindsEvent();
                                                    event.setStrength(strength);
                                                    event.setDuration(seconds * 20); // Convert to ticks
                                                    WorldEventManager.getInstance().startEvent(world, event);

                                                    String strengthText = strength == 1 ? "mild" : strength == 2 ? "moderate" : "severe";
                                                    source.sendFeedback(Text.literal("🌬️ Started " + strengthText + " Strong Winds event for " + seconds + " seconds."), true);
                                                    return 1;
                                                })
                                        )
                                )

                                // Combined strength and direction
                                .then(CommandManager.literal("direction")
                                        .then(CommandManager.argument("direction", StringArgumentType.word())
                                                .suggests((context, builder) -> {
                                                    builder.suggest("north");
                                                    builder.suggest("south");
                                                    builder.suggest("east");
                                                    builder.suggest("west");
                                                    builder.suggest("northeast");
                                                    builder.suggest("northwest");
                                                    builder.suggest("southeast");
                                                    builder.suggest("southwest");
                                                    builder.suggest("random");
                                                    return builder.buildFuture();
                                                })
                                                .executes(context -> {
                                                    ServerCommandSource source = context.getSource();
                                                    ServerWorld world = source.getWorld();
                                                    int strength = IntegerArgumentType.getInteger(context, "level");
                                                    String direction = StringArgumentType.getString(context, "direction");

                                                    StrongWindsEvent event = new StrongWindsEvent();
                                                    event.setStrength(strength);
                                                    setWindDirection(event, direction);
                                                    WorldEventManager.getInstance().startEvent(world, event);

                                                    String strengthText = strength == 1 ? "mild" : strength == 2 ? "moderate" : "severe";
                                                    source.sendFeedback(Text.literal("🌬️ Started " + strengthText + " Strong Winds event blowing from the " + direction + "."), true);
                                                    return 1;
                                                })

                                                // Combined all parameters: strength, direction, and duration
                                                .then(CommandManager.literal("duration")
                                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 120))
                                                                .executes(context -> {
                                                                    ServerCommandSource source = context.getSource();
                                                                    ServerWorld world = source.getWorld();
                                                                    int strength = IntegerArgumentType.getInteger(context, "level");
                                                                    String direction = StringArgumentType.getString(context, "direction");
                                                                    int seconds = IntegerArgumentType.getInteger(context, "seconds");

                                                                    StrongWindsEvent event = new StrongWindsEvent();
                                                                    event.setStrength(strength);
                                                                    setWindDirection(event, direction);
                                                                    event.setDuration(seconds * 20); // Convert to ticks
                                                                    WorldEventManager.getInstance().startEvent(world, event);

                                                                    String strengthText = strength == 1 ? "mild" : strength == 2 ? "moderate" : "severe";
                                                                    source.sendFeedback(Text.literal("🌬️ Started " + strengthText + " Strong Winds event blowing from the " + direction + " for " + seconds + " seconds."), true);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );

        // Overgrowth command
        dispatcher.register(CommandManager.literal("startovergrowth")
                .requires(source -> source.hasPermissionLevel(2)) // OP level 2+
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerWorld world = source.getWorld();

                    OvergrowthEvent event = new OvergrowthEvent();
                    WorldEventManager.getInstance().startEvent(world, event);

                    source.sendFeedback(Text.literal("🌱 Manually started Overgrowth event."), true);
                    return 1;
                })

                // Add duration parameter (in seconds)
                .then(CommandManager.literal("duration")
                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 600))
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerWorld world = source.getWorld();
                                    int seconds = IntegerArgumentType.getInteger(context, "seconds");

                                    OvergrowthEvent event = new OvergrowthEvent();
                                    event.setDuration(seconds * 20); // Convert to ticks
                                    WorldEventManager.getInstance().startEvent(world, event);

                                    source.sendFeedback(Text.literal("🌱 Started Overgrowth event with " + seconds + " seconds duration."), true);
                                    return 1;
                                })
                        )
                )

                // Add intensity parameter
                .then(CommandManager.literal("intensity")
                        .then(CommandManager.argument("level", IntegerArgumentType.integer(1, 3))
                                .executes(context -> {
                                    ServerCommandSource source = context.getSource();
                                    ServerWorld world = source.getWorld();
                                    int intensity = IntegerArgumentType.getInteger(context, "level");

                                    OvergrowthEvent event = new OvergrowthEvent();
                                    event.setIntensity(intensity);
                                    WorldEventManager.getInstance().startEvent(world, event);

                                    String intensityText = intensity == 1 ? "mild" : intensity == 2 ? "moderate" : "intense";
                                    source.sendFeedback(Text.literal("🌱 Started " + intensityText + " Overgrowth event."), true);
                                    return 1;
                                })

                                // Combined intensity and duration
                                .then(CommandManager.literal("duration")
                                        .then(CommandManager.argument("seconds", IntegerArgumentType.integer(5, 600))
                                                .executes(context -> {
                                                    ServerCommandSource source = context.getSource();
                                                    ServerWorld world = source.getWorld();
                                                    int intensity = IntegerArgumentType.getInteger(context, "level");
                                                    int seconds = IntegerArgumentType.getInteger(context, "seconds");

                                                    OvergrowthEvent event = new OvergrowthEvent();
                                                    event.setIntensity(intensity);
                                                    event.setDuration(seconds * 20); // Convert to ticks
                                                    WorldEventManager.getInstance().startEvent(world, event);

                                                    String intensityText = intensity == 1 ? "mild" : intensity == 2 ? "moderate" : "intense";
                                                    source.sendFeedback(Text.literal("🌱 Started " + intensityText + " Overgrowth event for " + seconds + " seconds."), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
        );

        // Add a command to stop the wind
        dispatcher.register(CommandManager.literal("stopwind")
                .requires(source -> source.hasPermissionLevel(2)) // OP level 2+
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerWorld world = source.getWorld();

                    WorldEventManager.getInstance().stopEvent(world, StrongWindsEvent.ID);

                    source.sendFeedback(Text.literal("🌬️ Manually stopped Strong Winds event."), true);
                    return 1;
                })
        );
        
        // Add a command to stop the overgrowth
        dispatcher.register(CommandManager.literal("stopovergrowth")
                .requires(source -> source.hasPermissionLevel(2)) // OP level 2+
                .executes(context -> {
                    ServerCommandSource source = context.getSource();
                    ServerWorld world = source.getWorld();

                    WorldEventManager.getInstance().stopEvent(world, OvergrowthEvent.ID);

                    source.sendFeedback(Text.literal("🌱 Manually stopped Overgrowth event."), true);
                    return 1;
                })
        );
    }

    /**
     * Helper method to set wind direction from string input
     */
    private static void setWindDirection(StrongWindsEvent event, String direction) {
        Vec3d dirVector;

        switch (direction.toLowerCase()) {
            case "north":
                dirVector = new Vec3d(0, 0, -1);
                break;
            case "south":
                dirVector = new Vec3d(0, 0, 1);
                break;
            case "east":
                dirVector = new Vec3d(1, 0, 0);
                break;
            case "west":
                dirVector = new Vec3d(-1, 0, 0);
                break;
            case "northeast":
                dirVector = new Vec3d(0.707, 0, -0.707);
                break;
            case "northwest":
                dirVector = new Vec3d(-0.707, 0, -0.707);
                break;
            case "southeast":
                dirVector = new Vec3d(0.707, 0, 0.707);
                break;
            case "southwest":
                dirVector = new Vec3d(-0.707, 0, 0.707);
                break;
            case "random":
            default:
                // Use the original random direction logic
                return;
        }

        event.setWindDirection(dirVector);
    }
}
