package end3r.verdant_arcanum.block.entity;

import end3r.verdant_arcanum.entity.MagicInfusedBee;
import end3r.verdant_arcanum.registry.ModBlockEntities;
import end3r.verdant_arcanum.registry.ModEntities;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

public class MagicBeeSpawnerBlockEntity extends BlockEntity {
    private int cooldown = 0;
    private static final int MAX_COOLDOWN = 1200; // 60 seconds (20 ticks per second)
    private static final int SPAWN_RADIUS = 10;
    private static final int MAX_BEES = 10;

    public MagicBeeSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_BEE_SPAWNER_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state, MagicBeeSpawnerBlockEntity entity) {
        if (world.isClient) {
            return; // Don't run on client side
        }

        // Decrease cooldown
        if (entity.cooldown > 0) {
            entity.cooldown--;
            return;
        }

        // Check the number of magic bees in the area
        int beeCount = entity.countNearbyBees();

        // If there are fewer than MAX_BEES, spawn a new one
        if (beeCount < MAX_BEES) {
            entity.spawnMagicBee((ServerWorld) world, pos);
            // Reset cooldown
            entity.cooldown = MAX_COOLDOWN;
            entity.markDirty();
        }
    }

    private void spawnMagicBee(ServerWorld world, BlockPos pos) {
        // Generate random position within spawn radius
        double x = pos.getX() + (world.getRandom().nextDouble() * SPAWN_RADIUS * 2) - SPAWN_RADIUS;
        double y = pos.getY() + world.getRandom().nextDouble() * 2; // Spawn slightly above or at the spawner
        double z = pos.getZ() + (world.getRandom().nextDouble() * SPAWN_RADIUS * 2) - SPAWN_RADIUS;

        // Create the bee
        MagicInfusedBee bee = ModEntities.MAGIC_INFUSED_BEE.create(world);
        if (bee != null) {
            bee.refreshPositionAndAngles(x, y, z, world.getRandom().nextFloat() * 360f, 0);

            // Add magical particle effects at spawn location
            world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.WITCH,
                    x, y, z,
                    15,  // number of particles
                    0.5, 0.5, 0.5,  // spread
                    0.1  // speed
            );

            world.spawnParticles(
                    net.minecraft.particle.ParticleTypes.ENCHANT,
                    x, y, z,
                    10,  // number of particles
                    0.5, 0.5, 0.5,  // spread
                    0.1  // speed
            );

            world.spawnEntity(bee);
        }
    }

    public int countNearbyBees() {
        if (world == null || world.isClient) {
            return 0;
        }

        // Create a bounding box centered on this block, with a radius of SPAWN_RADIUS
        Box box = new Box(
                pos.getX() - SPAWN_RADIUS, pos.getY() - SPAWN_RADIUS, pos.getZ() - SPAWN_RADIUS,
                pos.getX() + SPAWN_RADIUS, pos.getY() + SPAWN_RADIUS, pos.getZ() + SPAWN_RADIUS
        );

        // Get a list of all magic infused bees within the bounding box
        List<MagicInfusedBee> bees = world.getEntitiesByType(
                ModEntities.MAGIC_INFUSED_BEE,
                box,
                bee -> true // No additional filtering
        );

        return bees.size();
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.cooldown = nbt.getInt("Cooldown");
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("Cooldown", this.cooldown);
    }
}