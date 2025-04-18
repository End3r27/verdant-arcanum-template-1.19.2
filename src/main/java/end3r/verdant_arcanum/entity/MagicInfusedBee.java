package end3r.verdant_arcanum.entity;

import end3r.verdant_arcanum.block.MagicHiveBlock;
import end3r.verdant_arcanum.block.entity.MagicHiveBlockEntity;
import end3r.verdant_arcanum.registry.ModItems;
import end3r.verdant_arcanum.registry.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class MagicInfusedBee extends BeeEntity {
    // Map to track which flower types correspond to which essence types
    private static final Map<Item, Item> BLOOM_TO_ESSENCE_MAP = new HashMap<>();

    // Track what kind of pollen this bee is carrying
    private Item currentPollenType = null;

    // Track our own flower position for magic bees
    private BlockPos magicFlowerPos = null;

    // Track deposit cooldown to prevent multiple attempts in one tick
    private int depositCooldown = 100;

    private boolean hasNectarOverride = false;

    @Override
    public boolean hasNectar() {
        // Either use our override or check the parent method
        return hasNectarOverride || super.hasNectar();
    }



    // Debug flag to show more detailed output
    public static final boolean DEBUG_MODE = false;

    static {
        // Initialize the mapping between flower blooms and spell essences
        BLOOM_TO_ESSENCE_MAP.put(ModItems.FLAME_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_FLAME);
        BLOOM_TO_ESSENCE_MAP.put(ModItems.BLINK_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_BLINK);
        BLOOM_TO_ESSENCE_MAP.put(ModItems.ROOTGRASP_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_ROOTGRASP);
        BLOOM_TO_ESSENCE_MAP.put(ModItems.GUST_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_GUST);
        BLOOM_TO_ESSENCE_MAP.put(ModItems.BREEZEVINE_FLOWER_BLOOM, ModItems.SPELL_ESSENCE_BREEZEVINE);

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
    protected void initGoals() {
        super.initGoals();

        // Add our custom goal with VERY HIGH priority
        this.goalSelector.add(1, new MagicBeeReturnToHiveGoal(this));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        // Save our override state
        nbt.putBoolean("HasNectarOverride", hasNectarOverride);
        // Save our magic flower position
        if (magicFlowerPos != null) {
            nbt.putInt("MagicFlowerX", magicFlowerPos.getX());
            nbt.putInt("MagicFlowerY", magicFlowerPos.getY());
            nbt.putInt("MagicFlowerZ", magicFlowerPos.getZ());
        }

        // Save our pollen type
        if (currentPollenType != null) {
            nbt.putString("PollenType", currentPollenType.toString());
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        // Load our override state
        hasNectarOverride = nbt.getBoolean("HasNectarOverride");
        // Load our magic flower position
        if (nbt.contains("MagicFlowerX")) {
            int x = nbt.getInt("MagicFlowerX");
            int y = nbt.getInt("MagicFlowerY");
            int z = nbt.getInt("MagicFlowerZ");
            magicFlowerPos = new BlockPos(x, y, z);
        }

        // We would need a registry lookup to properly restore the pollen type
        // This is a simplification - you'd need to handle item registration properly
        // currentPollenType = Registry.ITEM.get(new Identifier(nbt.getString("PollenType")));
    }

    public void setHasNectar(boolean hasNectar) {
        // Set the override field
        this.hasNectarOverride = hasNectar;

        // Also try to set the flag in the data tracker
        byte flags = this.getDataTracker().get(BeeEntity.FLAGS);
        if (hasNectar) {
            this.getDataTracker().set(BeeEntity.FLAGS, (byte)(flags | 8));
        } else {
            this.getDataTracker().set(BeeEntity.FLAGS, (byte)(flags & ~8));
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Decrease deposit cooldown if it's active
        if (depositCooldown > 0) {
            depositCooldown--;
        }

        // Spawn magic particles occasionally - client side code
        if (this.world.isClient && this.random.nextInt(10) == 0) {
            double d = this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D;
            double e = this.getY() + 0.3D;
            double f = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D;

            this.world.addParticle(ParticleTypes.WITCH, d, e, f, 0.0D, 0.0D, 0.0D);
            return; // End client-side processing here
        }

        // Server-side processing below
        if (this.world.isClient) return;

        // Check flower positions when the bee has nectar to see if we need to set pollen type
        if (this.hasNectar() && this.currentPollenType == null) {
            updatePollenTypeFromFlower();
        }

        // ========= FLOWER FINDING LOGIC =========
        // Only look for flowers if we don't have nectar
        if (!this.hasNectar() && this.random.nextInt(20) == 0) {
            BlockPos nearestFlower = findNearestMagicalFlower();
            if (nearestFlower != null) {
                // Store our own flower position
                this.magicFlowerPos = nearestFlower;

                try {
                    // Try to call the super implementation first if it exists
                    this.setFlowerPos(nearestFlower);
                } catch (Exception e) {
                    // If it fails, we'll just use our own implementation
                    if (DEBUG_MODE) {
                        System.out.println("Using custom flower position tracking for magic bee");
                    }
                }

                // Direct the bee to move toward the flower
                this.getMoveControl().moveTo(
                        nearestFlower.getX() + 0.5,
                        nearestFlower.getY() + 0.5,
                        nearestFlower.getZ() + 0.5,
                        1.0
                );

                if (DEBUG_MODE && this.random.nextInt(100) == 0) {
                    System.out.println("Magic bee found flower at " + nearestFlower);
                }
            }
        }

        // Add this to the tick method
        if (DEBUG_MODE && this.random.nextInt(200) == 0) {
            BlockPos hive = findNearestMagicHive();


        }
    }

    // Method to force the bee to navigate to a specific hive
    public void forceNavigateToHive(BlockPos hivePos) {
        if (hivePos != null) {
            // Force stop any current path
            this.getNavigation().stop();

            // Create a new path to the hive - use pathfinding
            Path path = this.getNavigation().findPathTo(hivePos, 0);
            if (path != null) {
                this.getNavigation().startMovingAlong(path, 1.0);
                if (DEBUG_MODE) {
                    System.out.println("FORCE: Magic bee is now navigating to hive at " + hivePos);
                }
            } else {
                // Fallback - direct movement if pathfinding fails
                this.getMoveControl().moveTo(
                        hivePos.getX() + 0.5,
                        hivePos.getY() + 1.0,
                        hivePos.getZ() + 0.5,
                        1.0);
                if (DEBUG_MODE) {
                    System.out.println("FORCE: Magic bee using direct movement to hive at " + hivePos);
                }
            }
        }
    }

    /**
     * Try to deposit essence at any nearby hives
     * @return true if essence was successfully deposited
     */
    public boolean tryDepositEssence() {
        // Only proceed if we have nectar and know which pollen type
        if (!this.hasNectar() || this.currentPollenType == null) {
            return false;
        }

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
                        if (DEBUG_MODE) {
                            System.out.println("Magic bee successfully deposited " + essenceToDeposit + " at " + checkPos);
                            System.out.println("Before reset - hasNectar(): " + this.hasNectar());
                        }

                        // Play sound for successful deposit
                        playEssenceDepositSound(checkPos, essenceToDeposit);

                        // FORCED RESET - Multiple approaches to ensure it's reset
                        // 1. Try the data tracker approach
                        byte flags = this.getDataTracker().get(BeeEntity.FLAGS);
                        this.getDataTracker().set(BeeEntity.FLAGS, (byte) (flags & ~8));

                        // 2. Use our override field
                        this.hasNectarOverride = false;

                        // 3. Reset the pollen type
                        this.currentPollenType = null;

                        if (DEBUG_MODE) {
                            System.out.println("After FORCED reset - hasNectar(): " + this.hasNectar());
                            System.out.println("New flags value: " + this.getDataTracker().get(BeeEntity.FLAGS));
                        }

                        return true;
                    }
                }

            }
        }

        return false;
    }

    /**
     * Updates the pollen type based on the flower the bee visited
     */
    private void updatePollenTypeFromFlower() {
        BlockPos flowerPos = this.magicFlowerPos;
        if (flowerPos != null) {
            BlockState state = this.world.getBlockState(flowerPos);

            // Check if it's a magical flower and set the pollen type
            if (state.isIn(ModTags.Blocks.FLAME_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.FLAME_FLOWER_BLOOM;
                if (DEBUG_MODE) System.out.println("Magic bee collected FLAME pollen");
            } else if (state.isIn(ModTags.Blocks.BLINK_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.BLINK_FLOWER_BLOOM;
                if (DEBUG_MODE) System.out.println("Magic bee collected BLINK pollen");
            } else if (state.isIn(ModTags.Blocks.ROOTGRASP_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.ROOTGRASP_FLOWER_BLOOM;
                if (DEBUG_MODE) System.out.println("Magic bee collected ROOTGRASP pollen");
            } else if (state.isIn(ModTags.Blocks.GUST_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.GUST_FLOWER_BLOOM;
                if (DEBUG_MODE) System.out.println("Magic bee collected GUST pollen");
            } else if (state.isIn(ModTags.Blocks.BREEZEVINE_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.BREEZEVINE_FLOWER_BLOOM;
                if (DEBUG_MODE) System.out.println("Magic bee collected BREEZEVINE pollen");
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

    // Find the nearest magic hive
    public BlockPos findNearestMagicHive() {
        BlockPos beePos = this.getBlockPos();
        int searchRadius = 32; // Increase search radius
        int verticalSearch = 16; // Increase vertical search radius

        // Debug output
        if (DEBUG_MODE && this.random.nextInt(400) == 0) {
            System.out.println("Magic bee searching for hive in radius " + searchRadius +
                    " around " + beePos);
        }

        // Start with the closest possible block to make search more efficient
        BlockPos nearestHive = null;
        double nearestDistance = Double.MAX_VALUE;

        for (BlockPos checkPos : BlockPos.iterate(
                beePos.add(-searchRadius, -verticalSearch, -searchRadius),
                beePos.add(searchRadius, verticalSearch, searchRadius))) {

            BlockState state = this.world.getBlockState(checkPos);
            if (state.getBlock() instanceof MagicHiveBlock) {
                double distance = checkPos.getSquaredDistance(beePos);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestHive = checkPos.toImmutable();

                    if (DEBUG_MODE) {
                        System.out.println("Found magic hive at " + nearestHive +
                                " (distance: " + Math.sqrt(distance) + ")");
                    }
                }
            }
        }

        return nearestHive;
    }

    private SoundEvent playEssenceDepositSound(BlockPos hivePos, Item essenceType) {
        // Play a sound effect based on the essence type
        if (!this.world.isClient) {
            float pitch = 1.0F;
            float volume = 0.8F;
            SoundEvent soundEvent = SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME; // Default sound

            // Fallback vanilla sounds that somewhat match the essence types
            if (essenceType == ModItems.SPELL_ESSENCE_FLAME) {
                soundEvent = SoundEvents.BLOCK_FIRE_AMBIENT;
            }
            else if (essenceType == ModItems.SPELL_ESSENCE_BLINK) {
                soundEvent = SoundEvents.ENTITY_ENDERMAN_TELEPORT;
            }
            else if (essenceType == ModItems.SPELL_ESSENCE_ROOTGRASP) {
                soundEvent = SoundEvents.BLOCK_GRASS_BREAK;
            }
            else if (essenceType == ModItems.SPELL_ESSENCE_GUST) {
                soundEvent = SoundEvents.ENTITY_PHANTOM_FLAP;
            }
            else if (essenceType == ModItems.SPELL_ESSENCE_BREEZEVINE) {
                soundEvent = SoundEvents.BLOCK_VINE_STEP; // Using vine step sound for breezevine
            }

            // Play the sound at the hive location
            this.world.playSound(null, hivePos, soundEvent, this.getSoundCategory(), volume, pitch);
            return soundEvent;
        }
        return null;
    }

    // Add this new method to check for magic hives in a very close proximity
    private BlockPos findNearbyMagicHive() {
        BlockPos beePos = this.getBlockPos();
        int searchRadius = 2; // Very small radius - just checking immediate vicinity

        // Check a smaller area for more immediate deposits
        for (BlockPos checkPos : BlockPos.iterate(
                beePos.add(-searchRadius, -searchRadius, -searchRadius),
                beePos.add(searchRadius, searchRadius, searchRadius))) {

            BlockState state = this.world.getBlockState(checkPos);
            if (state.getBlock() instanceof MagicHiveBlock) {
                return checkPos.toImmutable();
            }
        }

        return null;
    }

    // Getter for currentPollenType
    public Item getCurrentPollenType() {
        return this.currentPollenType;
    }

    // Setter for currentPollenType
    public void setCurrentPollenType(Item pollenType) {
        this.currentPollenType = pollenType;
    }

    // Custom goal for magic bees to return to magic hives
    public static class MagicBeeReturnToHiveGoal extends Goal {
        private final MagicInfusedBee bee;
        private BlockPos targetHivePos = null;
        private int searchCooldown = 0;
        private int goToHiveCounter = 0;

        public MagicBeeReturnToHiveGoal(MagicInfusedBee bee) {
            this.bee = bee;
            // Set control flags
            this.setControls(EnumSet.of(Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            // This goal can start if the bee has nectar and pollen
            if (!bee.hasNectar() || bee.getCurrentPollenType() == null) {
                return false;
            }

            // If we're on cooldown, decrement and skip
            if (searchCooldown > 0) {
                searchCooldown--;
                return false;
            }

            // Find a hive if we don't already have one
            if (targetHivePos == null) {
                targetHivePos = bee.findNearestMagicHive();

                // If still no hive found, set a longer cooldown
                if (targetHivePos == null) {
                    searchCooldown = 60; // 3 seconds
                    return false;
                }
            }

            // Double-check the hive still exists and is valid
            if (!isMagicHiveAt(targetHivePos)) {
                targetHivePos = null;
                return false;
            }

            return true;
        }

        @Override
        public boolean shouldContinue() {
            // Continue as long as the bee has nectar, pollen, and a valid hive
            return bee.hasNectar() &&
                    bee.getCurrentPollenType() != null &&
                    targetHivePos != null &&
                    isMagicHiveAt(targetHivePos);
        }

        @Override
        public void start() {
            if (DEBUG_MODE) {
                System.out.println("GOAL START: Magic bee beginning navigation to hive at " + targetHivePos);
            }
            goToHiveCounter = 0;
        }

        @Override
        public void stop() {
            if (DEBUG_MODE) {
                System.out.println("GOAL STOP: Magic bee stopping hive navigation");
            }
        }

        @Override
        public void tick() {
            // If we somehow lost our target, try to find a new one
            if (targetHivePos == null) {
                targetHivePos = bee.findNearestMagicHive();
                if (targetHivePos == null) return;
            }

            // Verify hive still exists
            if (!isMagicHiveAt(targetHivePos)) {
                targetHivePos = null;
                return;
            }

            // Calculate distance to hive
            double distanceToHive = bee.getBlockPos().getSquaredDistance(targetHivePos);

            // Try to deposit if we're close enough
            if (distanceToHive < 4.0) {
                boolean success = bee.tryDepositEssence();
                if (success) {
                    // Reset everything on successful deposit
                    targetHivePos = null;
                    return;
                }
            }

            // Force navigation more aggressively every 20 ticks
            goToHiveCounter++;
            if (goToHiveCounter % 20 == 0 || bee.getNavigation().isIdle()) {
                bee.forceNavigateToHive(targetHivePos);
            }

            if (DEBUG_MODE && bee.getRandom().nextInt(40) == 0) {
                System.out.println("GOAL TICK: Magic bee navigating to hive at " + targetHivePos +
                        " (distance: " + Math.sqrt(distanceToHive) +
                        ", counter: " + goToHiveCounter + ")");
            }
        }

        private boolean isMagicHiveAt(BlockPos pos) {
            // Check if the block at the position is still a magic hive
            return bee.world.getBlockState(pos).getBlock() instanceof MagicHiveBlock;
        }
    }
}