package end3r.verdant_arcanum.event;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.GameRules;

import java.util.Random;

public class FireRainEvent implements CustomWorldEvent {
    public static final Identifier ID = new Identifier("verdant_arcanum", "fire_rain");
    private static final int DEFAULT_DURATION = 20 * 60 * 2; // 2 minutes (in ticks)
    private static final int CHECK_INTERVAL = 10; // Check for player damage every 10 ticks (0.5 seconds)
    private static final float DAMAGE_AMOUNT = 1.0f; // 0.5 hearts of damage
    private static final DamageSource DAMAGE_SOURCE = new DamageSource("fire_rain").setFire();

    private int ticksRemaining = DEFAULT_DURATION;
    private int intensityLevel = 2; // 1=mild, 2=moderate, 3=severe
    private int tickCounter = 0;
    private Random random = new Random();

    // Store previous weather state to restore after event
    private boolean wasRaining = false;
    private boolean wasThundering = false;
    private int prevRainTime = 0;
    private int prevThunderTime = 0;

    /**
     * Set the duration of the fire rain event
     * @param ticks Duration in ticks (20 ticks = 1 second)
     */
    public void setDuration(int ticks) {
        this.ticksRemaining = Math.max(200, ticks); // Minimum 10 seconds
    }

    /**
     * Set the intensity of the fire rain
     * @param intensity 1=mild, 2=moderate, 3=severe
     */
    public void setIntensity(int intensity) {
        this.intensityLevel = Math.max(1, Math.min(3, intensity)); // Clamp between 1-3
    }

    @Override
    public void start(ServerWorld world) {
        // Ensure we're only in the Nether
        if (world.getRegistryKey() != World.NETHER) {
            return;
        }

        // Reset tick counter
        tickCounter = 0;

        // Store current weather state
        wasRaining = world.isRaining();
        wasThundering = world.isThundering();
        prevRainTime = world.getRainTime();
        prevThunderTime = world.getThunderTime();

        // Force rain to start - NOTE: This is a server-side instruction to start the rain weather effect
        // The visual rain effect will be determined by client-side resources or resource packs
        // A resource pack can be used to make the rain appear red/orange in the Nether
        world.setWeather(0, ticksRemaining, true, intensityLevel > 1);

        // Determine description based on intensity
        String intensityDesc;
        switch (intensityLevel) {
            case 1:
                intensityDesc = "Mild";
                break;
            case 3:
                intensityDesc = "Deadly";
                break;
            default:
                intensityDesc = "Fierce";
                break;
        }

        // Notify all players in the Nether
        world.getPlayers().forEach(player ->
                player.sendMessage(Text.literal("🔥 A " + intensityDesc + " rain of fire begins to fall from above!")
                        .formatted(Formatting.RED, Formatting.BOLD), true)
        );

        // Play a dramatic sound for all players
        world.getPlayers().forEach(player ->
                player.playSound(SoundEvents.ENTITY_BLAZE_AMBIENT, SoundCategory.AMBIENT, 1.0f, 0.5f)
        );

        // Initial dramatic thunder and fire sounds
        if (intensityLevel > 1) {
            world.getPlayers().forEach(player -> {
                player.playSound(SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 1.0f, 0.8f);
                player.playSound(SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.AMBIENT, 1.0f, 0.7f);
            });
        }

        // Store fire spread rule value and possibly enable fire spreading based on intensity
        if (intensityLevel > 1) {
            // Only enable fire spread for higher intensities
            boolean originalFireSpreadValue = world.getGameRules().getBoolean(GameRules.DO_FIRE_TICK);
            if (!originalFireSpreadValue) {
                // Don't actually modify the game rule as it affects the whole server
                // But we'll handle fire creation separately
            }
        }
    }

    @Override
    public void tick(ServerWorld world) {
        // Ensure we're only in the Nether
        if (world.getRegistryKey() != World.NETHER) {
            return;
        }

        // Decrement remaining time
        ticksRemaining--;
        tickCounter++;

        // Ensure it's still raining
        if (!world.isRaining()) {
            world.setWeather(0, ticksRemaining, true, intensityLevel > 1);
        }

        // For maximum intensity, occasionally create fires on exposed blocks
        if (intensityLevel == 3 && random.nextFloat() < 0.05f) {
            createRandomFires(world);
        }

        // Check for player damage
        if (tickCounter % CHECK_INTERVAL == 0) {
            checkPlayerExposure(world);
        }

        // Play ambient fire sounds
        if (tickCounter % 40 == 0) {
            world.getPlayers().forEach(player ->
                    player.playSound(
                            SoundEvents.BLOCK_FIRE_AMBIENT,
                            SoundCategory.AMBIENT,
                            0.3f,
                            0.7f + random.nextFloat() * 0.3f
                    )
            );
        }

        // Stronger thunder sounds occasionally for higher intensities
        if (intensityLevel > 1 && tickCounter % 100 == 0) {
            world.getPlayers().forEach(player ->
                    player.playSound(
                            SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                            SoundCategory.WEATHER,
                            0.4f,
                            0.6f + random.nextFloat() * 0.2f
                    )
            );
        }

        // Notify players when event is ending
        if (ticksRemaining == 600) { // 30 seconds left
            world.getPlayers().forEach(player ->
                    player.sendMessage(Text.literal("The fire rain is beginning to subside...")
                            .formatted(Formatting.RED), true)
            );
        } else if (ticksRemaining == 100) { // 5 seconds left
            world.getPlayers().forEach(player ->
                    player.sendMessage(Text.literal("The fire rain stops...")
                            .formatted(Formatting.GOLD), true)
            );
        }
    }

    /**
     * Create random fires on the ground for maximum intensity
     */
    private void createRandomFires(ServerWorld world) {
        for (PlayerEntity player : world.getPlayers()) {
            // Skip if too few players online
            if (world.getPlayers().size() < 1) {
                return;
            }

            // Pick a random player and place fires around them
            if (random.nextFloat() < 0.3f) { // 30% chance per player
                int fires = 1 + random.nextInt(3); // 1-3 fires

                for (int i = 0; i < fires; i++) {
                    int range = 10 + (intensityLevel * 5); // 15-25 block range
                    int x = player.getBlockPos().getX() + (random.nextInt(range * 2) - range);
                    int z = player.getBlockPos().getZ() + (random.nextInt(range * 2) - range);

                    // Find the top block
                    BlockPos pos = new BlockPos(x, player.getBlockPos().getY(), z);
                    BlockPos topBlock = world.getTopPosition(net.minecraft.world.Heightmap.Type.WORLD_SURFACE, pos);

                    // Only place fire if the player is exposed to the sky
                    if (world.isSkyVisible(topBlock)) {
                        // Check if we can place fire here (air block with solid block beneath)
                        BlockPos firePos = topBlock.up();
                        if (world.isAir(firePos) && world.getBlockState(topBlock).isSolidBlock(world, topBlock)) {
                            world.setBlockState(firePos, net.minecraft.block.Blocks.FIRE.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if players are exposed to the fire rain and apply damage
     */
    private void checkPlayerExposure(ServerWorld world) {
        for (PlayerEntity player : world.getPlayers()) {
            // Skip players in creative or spectator mode
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }

            // Check if the player is exposed to the sky (not under cover)
            if (isExposedToSky(world, player)) {
                // Scale damage with intensity
                float damage = DAMAGE_AMOUNT * intensityLevel;

                // Apply fire damage
                player.damage(DAMAGE_SOURCE, damage);

                // Apply fire effect for longer duration with higher intensity
                int fireDuration = 20 + (intensityLevel * 20); // 1-3 seconds of fire
                player.setFireTicks(player.getFireTicks() + fireDuration);

                // Notify player (only sometimes to avoid spam)
                if (random.nextFloat() < 0.3f) {
                    player.sendMessage(Text.literal("You're being burned by the fire rain!")
                            .formatted(Formatting.RED), true);
                }
            }
        }
    }

    /**
     * Check if a player is exposed to the sky
     */
    private boolean isExposedToSky(ServerWorld world, PlayerEntity player) {
        BlockPos playerPos = player.getBlockPos();

        // The height to check above the player
        int checkHeight = 10;

        // Check if there are any blocks above the player
        for (int y = 1; y <= checkHeight; y++) {
            BlockPos checkPos = playerPos.up(y);
            if (!world.isAir(checkPos)) {
                return false; // Player is covered
            }
        }

        return true; // Player is exposed
    }

    @Override
    public void onComplete(ServerWorld world) {
        // Restore previous weather state
        if (wasRaining) {
            world.setWeather(0, prevRainTime, true, wasThundering);
        } else {
            world.setWeather(prevRainTime, 0, false, false);
        }

        // Notify players that the event has ended
        world.getPlayers().forEach(player ->
                player.sendMessage(Text.literal("The fire rain has ended.")
                        .formatted(Formatting.GOLD), true)
        );
    }

    @Override
    public boolean isComplete() {
        return ticksRemaining <= 0;
    }

    @Override
    public Identifier getId() {
        return ID;
    }
}