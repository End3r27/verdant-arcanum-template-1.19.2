package end3r.verdant_arcanum.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import end3r.verdant_arcanum.entity.SolarBeamEntity;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ModCommands {





    // Add this to a command handler class
    private static int executeSetBeamCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();

        // Create start and end positions
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVec(1.0F).normalize();
        Vec3d end = start.add(direction.multiply(10.0));  // 10-block beam

        // Find nearest beam entity or spawn new one
        SolarBeamEntity beam = null;

        List<SolarBeamEntity> beams = player.world.getEntitiesByClass(
                SolarBeamEntity.class,
                player.getBoundingBox().expand(5.0),
                e -> true
        );

        if (!beams.isEmpty()) {
            beam = beams.get(0);
        } else {
            beam = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, player.world);
            beam.setPosition(start);
            player.world.spawnEntity(beam);
        }

        // Update beam positions
        beam.updateBeam(start, end);

        source.sendFeedback(Text.of("Updated beam positions to: " + start + " -> " + end), false);
        return 1;
    }

    public static void registerCommands() {
        // Register the command using Fabric API's CommandRegistrationCallback
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(
                        CommandManager.literal("set_beam")
                                .requires(source -> source.hasPermissionLevel(2))
                                .executes(ModCommands::executeSetBeamCommand)
                )
        );

        // In your command registration
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("spawnbeam")
                    .executes(context -> {
                        ServerWorld world = context.getSource().getWorld();
                        ServerPlayerEntity player = context.getSource().getPlayer();

                        Vec3d startPos = player.getPos().add(0, 1, 0); // Start at eye level
                        Vec3d direction = player.getRotationVector();
                        Vec3d endPos = startPos.add(direction.multiply(10)); // 10 blocks in look direction

                        SolarBeamEntity beam = new SolarBeamEntity(ModEntities.SOLAR_BEAM_ENTITY, world);
                        beam.setPosition(startPos.x, startPos.y, startPos.z);
                        beam.updateBeam(startPos, endPos);
                        world.spawnEntity(beam);

                        context.getSource().sendFeedback(Text.literal("Spawned beam!"), false);
                        return 1;
                    }));
        });
    }
}

