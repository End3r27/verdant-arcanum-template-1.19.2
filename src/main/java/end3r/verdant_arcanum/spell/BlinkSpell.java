package end3r.verdant_arcanum.spell;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;

/**
 * Blink spell implementation that teleports the player in their look direction.
 */
public class BlinkSpell implements Spell {
    // Mana cost for casting this spell
    private static final int MANA_COST = 30;
    // Maximum teleport distance
    private static final int MAX_DISTANCE = 8;

    @Override
    public String getType() {
        return "Blink";
    }

    @Override
    public int getManaCost() {
        return MANA_COST;
    }

    @Override
    public void cast(World world, PlayerEntity player) {
        // Calculate destination based on where the player is looking
        Vec3d lookVec = player.getRotationVector();
        Vec3d startPos = player.getEyePos();

        // Perform raycast to find a valid teleport location
        BlockPos targetPos = findTeleportDestination(world, startPos, lookVec, MAX_DISTANCE);

        // If a valid position was found, teleport the player
        if (targetPos != null) {
            // Store original position for particle effects
            double originalX = player.getX();
            double originalY = player.getY();
            double originalZ = player.getZ();

            // Play teleport sound at current location
            world.playSound(null, originalX, originalY, originalZ,
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS,
                    0.5F, 1.0F);

            // Teleport the player
            player.teleport(targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5);

            // Play teleport sound at the destination
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS,
                    0.5F, 1.0F);
        }
    }

    private BlockPos findTeleportDestination(World world, Vec3d startPos, Vec3d direction, int maxDistance) {
        // Normalize the direction vector
        direction = direction.normalize();

        // Check each block along the ray
        for (int i = 1; i <= maxDistance; i++) {
            Vec3d checkPos = startPos.add(direction.multiply(i));
            BlockPos blockPos = new BlockPos((int) Math.floor(checkPos.x), (int) Math.floor(checkPos.y), (int) Math.floor(checkPos.z));

            // Check if the destination blocks are clear (for player's feet and head)
            if (isBlockSafeForTeleport(world, blockPos) &&
                    isBlockSafeForTeleport(world, blockPos.up()) &&
                    !isBlockSafeForTeleport(world, blockPos.down())) {
                return blockPos;
            }
        }

        // If we couldn't find a valid position, try teleporting as far as possible in a safe way
        for (int i = maxDistance; i > 0; i--) {
            Vec3d checkPos = startPos.add(direction.multiply(i));
            BlockPos blockPos = new BlockPos((int) Math.floor(checkPos.x), (int) Math.floor(checkPos.y), (int) Math.floor(checkPos.z));

            if (isBlockSafeForTeleport(world, blockPos) &&
                    isBlockSafeForTeleport(world, blockPos.up()) &&
                    !isBlockSafeForTeleport(world, blockPos.down())) {
                return blockPos;
            }
        }

        return null;
    }

    private boolean isBlockSafeForTeleport(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        // Check if the block is air or something else a player can stand in
        return state.getMaterial() == Material.AIR ||
                !state.getMaterial().blocksMovement();
    }

    @Override
    public void playClientEffects(World world, PlayerEntity player) {
        // Spawn particles at the player's position
        for (int i = 0; i < 20; i++) {
            world.addParticle(
                    ParticleTypes.PORTAL,
                    player.getX() + (world.random.nextDouble() - 0.5) * 2.0,
                    player.getY() + world.random.nextDouble() * 2.0,
                    player.getZ() + (world.random.nextDouble() - 0.5) * 2.0,
                    (world.random.nextDouble() - 0.5) * 2.0,
                    -world.random.nextDouble(),
                    (world.random.nextDouble() - 0.5) * 2.0
            );
        }
    }
}