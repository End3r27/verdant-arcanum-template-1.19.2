package end3r.verdant_arcanum.entity;

import end3r.verdant_arcanum.block.MagicHiveBlock;
import end3r.verdant_arcanum.block.entity.MagicHiveBlockEntity;
import end3r.verdant_arcanum.registry.ModBlocks;
import end3r.verdant_arcanum.registry.ModEntities;
import end3r.verdant_arcanum.registry.ModItems;
import end3r.verdant_arcanum.registry.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;

import java.util.HashMap;
import java.util.Map;

public class MagicInfusedBee extends BeeEntity {
    // Map to track which flower types correspond to which essence types
    private static final Map<Item, Item> BLOOM_TO_ESSENCE_MAP = new HashMap<>();

    // Track what kind of pollen this bee is carrying
    private Item currentPollenType = null;

    static {
        // Initialize the mapping between flower blooms and spell essences
        BLOOM_TO_ESSENCE_MAP.put(ModItems.FLAME_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_FLAME);
        BLOOM_TO_ESSENCE_MAP.put(ModItems.BLINK_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_BLINK);
        BLOOM_TO_ESSENCE_MAP.put(ModItems.ROOTGRASP_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_ROOTGRASP);
        BLOOM_TO_ESSENCE_MAP.put(ModItems.GUST_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_GUST);
    }

    public MagicInfusedBee(EntityType<? extends BeeEntity> entityType, World world) {
        super(entityType, world);
    }

    // Create the attribute container for the MagicInfusedBee
    public static DefaultAttributeContainer.Builder createMagicInfusedBeeAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 12.0D)
                .add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6F)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3F)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0D);
    }

    @Override
    public void tick() {
        super.tick();
        // Special handling for magic bees
        if (!this.world.isClient && !this.hasNectar() && this.random.nextInt(20) == 0) {
            BlockPos flowerPos = findNearestMagicalFlower();
            if (flowerPos != null) {
                // Force update the bee's flower position
                this.flowerPos = flowerPos;
                // Set the nectar goal to active
                this.getMoveControl().moveTo(flowerPos.getX(), flowerPos.getY(), flowerPos.getZ(), 1.0);
            }
        }

        if (!this.world.isClient && this.random.nextInt(100) == 0) {
            BlockPos flowerPos = this.getFlowerPos();
            System.out.println("MagicBee at " + this.getBlockPos() + " has flower pos: " + flowerPos);
            System.out.println("Has nectar: " + this.hasNectar());
            System.out.println("Current pollen type: " + this.currentPollenType);

            // Try to find nearby magical flowers
            int checkRadius = 10;
            boolean foundAny = false;
            for (BlockPos checkPos : BlockPos.iterate(
                    this.getBlockPos().add(-checkRadius, -checkRadius, -checkRadius),
                    this.getBlockPos().add(checkRadius, checkRadius, checkRadius))) {

                if (this.world.getBlockState(checkPos).isIn(ModTags.Blocks.MAGIC_FLOWERS_IN_BLOOM)) {
                    System.out.println("Found magical flower at " + checkPos);
                    foundAny = true;
                }
            }

            if (!foundAny) {
                System.out.println("No magical flowers found in " + checkRadius + " block radius");
            }
        }

        // Spawn magic particles occasionally
        if (this.world.isClient && this.random.nextInt(10) == 0) {
            double d = this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D;
            double e = this.getY() + 0.3D;
            double f = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D;

            this.world.addParticle(ParticleTypes.WITCH, d, e, f, 0.0D, 0.0D, 0.0D);
        }

        // Check flower positions when the bee has nectar to see if we need to set pollen type
        if (!this.world.isClient && this.hasNectar() && this.currentPollenType == null) {
            BlockPos flowerPos = this.getFlowerPos();
            if (flowerPos != null) {
                BlockState state = this.world.getBlockState(flowerPos);

                // Check if it's a magical flower and set the pollen type
                if (state.isIn(ModTags.Blocks.FLAME_FLOWERS_IN_BLOOM)) {
                    this.currentPollenType = ModItems.FLAME_FLOWER_BLOOM;
                } else if (state.isIn(ModTags.Blocks.BLINK_FLOWERS_IN_BLOOM)) {
                    this.currentPollenType = ModItems.BLINK_FLOWER_BLOOM;
                } else if (state.isIn(ModTags.Blocks.ROOTGRASP_FLOWERS_IN_BLOOM)) {
                    this.currentPollenType = ModItems.ROOTGRASP_FLOWER_BLOOM;
                } else if (state.isIn(ModTags.Blocks.GUST_FLOWERS_IN_BLOOM)) {
                    this.currentPollenType = ModItems.GUST_FLOWER_BLOOM;
                }
            }
        }

        // Process essence delivery to magic hives
        if (!this.world.isClient && !this.hasAngerTime()) {
            // Only proceed if we have nectar and know which pollen type
            if (this.hasNectar() && this.currentPollenType != null) {
                BlockPos pos = this.getBlockPos();

                // Check a 3x3x3 area around the bee for magic hives
                for (BlockPos checkPos : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                    BlockState state = this.world.getBlockState(checkPos);
                    if (state.getBlock() instanceof MagicHiveBlock) {
                        // Get the essence type based on the current pollen
                        Item essenceToDeposit = BLOOM_TO_ESSENCE_MAP.get(this.currentPollenType);

                        if (essenceToDeposit != null) {
                            boolean depositSuccessful = false;

                            // First try the block method
                            MagicHiveBlock hiveBlock = (MagicHiveBlock) state.getBlock();
                            depositSuccessful = hiveBlock.tryDepositEssence(world, checkPos, state, essenceToDeposit);

                            // If that didn't work, try using the block entity directly
                            if (!depositSuccessful && world.getBlockEntity(checkPos) instanceof MagicHiveBlockEntity) {
                                MagicHiveBlockEntity hiveEntity = (MagicHiveBlockEntity) world.getBlockEntity(checkPos);
                                ItemStack essenceStack = new ItemStack(essenceToDeposit, 1);
                                depositSuccessful = hiveEntity.addEssence(essenceStack);
                            }

                            // If deposit was successful via any method
                            if (depositSuccessful) {
                                // Reset nectar state using the proper BeeEntity method
                                this.setNectarFlag(false);
                                this.currentPollenType = null;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isInAir() {
        // Enhanced flying capabilities - can be considered in air even when touching blocks
        return !this.onGround;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        // Magic bees take less damage from non-magical attacks
        if (!source.isMagic() && !source.isOutOfWorld()) {
            amount = amount * 0.75f;
        }
        return super.damage(source, amount);
    }

    // Helper method for magical flower checking
    public boolean isMagicalFlower(BlockPos pos) {
        BlockState state = this.world.getBlockState(pos);
        return state.isIn(ModTags.Blocks.MAGIC_FLOWERS_IN_BLOOM);
    }

    // Helper method to access the protected flag setting method in BeeEntity
    private void setNectarFlag(boolean value) {
        // In Fabric 1.19.2, this is the appropriate way to access the BeeEntity's flags
        // Flag 8 is HAS_NECTAR_FLAG
        byte flags = this.getDataTracker().get(BeeEntity.FLAGS);
        if (value) {
            this.getDataTracker().set(BeeEntity.FLAGS, (byte)(flags | 8));
        } else {
            this.getDataTracker().set(BeeEntity.FLAGS, (byte)(flags & ~8));
        }
    }
    public BlockPos findNearestMagicalFlower() {
        BlockPos beePos = this.getBlockPos();
        int searchRadius = 16; // Increase this to search more blocks
        int verticalSearch = 8; // Search 8 blocks up and down

        // Start with the closest possible block to make search more efficient
        BlockPos nearestFlower = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos checkPos : BlockPos.iterate(
                beePos.add(-searchRadius, -verticalSearch, -searchRadius),
                beePos.add(searchRadius, verticalSearch, searchRadius))) {

            BlockState state = this.world.getBlockState(checkPos);
            if (state.isIn(ModTags.Blocks.MAGIC_FLOWERS_IN_BLOOM)) {
                double distance = checkPos.getSquaredDistance(beePos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestFlower = checkPos.toImmutable();
                }
            }
        }

        return nearestFlower;
    }

}