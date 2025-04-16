package end3r.verdant_arcanum.entity;

import end3r.verdant_arcanum.block.MagicHiveBlock;
import end3r.verdant_arcanum.block.entity.MagicHiveBlockEntity;
import end3r.verdant_arcanum.registry.ModItems;
import end3r.verdant_arcanum.registry.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
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

    // Debug flag to show more detailed output
    private static final boolean DEBUG_MODE = false;

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
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
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

    @Override
    public void tick() {
        super.tick();

        // Decrease deposit cooldown if it's active
        if (depositCooldown > 0) {
            depositCooldown--;
        }

        // Spawn magic particles occasionally
        if (this.world.isClient && this.random.nextInt(10) == 0) {
            double d = this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D;
            double e = this.getY() + 0.3D;
            double f = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D;

            this.world.addParticle(ParticleTypes.WITCH, d, e, f, 0.0D, 0.0D, 0.0D);
        }

        // Client-side processing ends here
        if (this.world.isClient) return;

        // Check flower positions when the bee has nectar to see if we need to set pollen type
        if (this.hasNectar() && this.currentPollenType == null) {
            updatePollenTypeFromFlower();
        }

        // ========= ESSENCE DEPOSIT HANDLING =========
        // Only process essence delivery if we have nectar, know the pollen type, and aren't angry
        if (this.hasNectar() && this.currentPollenType != null && !this.hasAngerTime() && depositCooldown <= 0) {
            // Try to deposit essence at nearby hives
            boolean didDeposit = tryDepositEssence();

            // If we deposited, add a cooldown to prevent repeated attempts
            if (didDeposit) {
                depositCooldown = 20; // 1-second cooldown
                return; // Skip the rest of the tick logic
            }
        }

        // ========= FLOWER/HIVE FINDING LOGIC =========
        // When we have nectar, prioritize going to hives instead of flowers
        if (this.hasNectar()) {
            // Try to find a nearby magic hive to deposit essence if we have nectar
            BlockPos nearestHive = findNearestMagicHive();
            if (nearestHive != null) {
                if (DEBUG_MODE && this.random.nextInt(100) == 0) {
                    System.out.println("Magic bee has nectar and is heading to hive at " + nearestHive);
                }

                // Direct the bee to move toward the hive - aim slightly above for better landing
                this.getMoveControl().moveTo(nearestHive.getX() + 0.5, nearestHive.getY() + 1.0, nearestHive.getZ() + 0.5, 1.0);
                return; // Skip the flower-finding logic since we're heading to a hive
            }
        }
        // Only look for flowers if we don't have nectar
        else if (!this.hasNectar() && this.random.nextInt(20) == 0) {
            BlockPos nearestFlower = findNearestMagicalFlower();
            if (nearestFlower != null) {
                // Store our own flower position
                this.magicFlowerPos = nearestFlower;

                if (DEBUG_MODE && this.random.nextInt(100) == 0) {
                    System.out.println("Magic bee found flower at " + nearestFlower);
                }

                // We'll call the parent method if available
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
                this.getMoveControl().moveTo(nearestFlower.getX() + 0.5, nearestFlower.getY() + 0.5, nearestFlower.getZ() + 0.5, 1.0);
            }
        }

        // Debug output every so often
        if (DEBUG_MODE && this.random.nextInt(400) == 0) {
            System.out.println("MagicBee at " + this.getBlockPos() + " has flower pos: " + this.magicFlowerPos);
            System.out.println("Has nectar: " + this.hasNectar() + ", Current pollen type: " + this.currentPollenType);
        }
    }

    /**
     * Try to deposit essence at any nearby hives
     * @return true if essence was successfully deposited
     */
    private boolean tryDepositEssence() {
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
                        }

                        // Play sound for successful deposit
                        playEssenceDepositSound(checkPos, essenceToDeposit);

                        // Reset nectar state using the proper BeeEntity method
                        this.setNectarFlag(false);
                        this.currentPollenType = null;
                        return true;
                    } else if (DEBUG_MODE) {
                        System.out.println("Magic bee failed to deposit essence - hive may be full");
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
        int searchRadius = 24; // Large radius to find hives
        int verticalSearch = 8; // Search 8 blocks up and down

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

            // Play the sound at the hive location
            this.world.playSound(null, hivePos, soundEvent, this.getSoundCategory(), volume, pitch);
            return soundEvent;
        }
        return null;
    }
}