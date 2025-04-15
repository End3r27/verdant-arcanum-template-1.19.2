package end3r.verdant_arcanum.entity;

import end3r.verdant_arcanum.block.MagicHiveBlock;
import end3r.verdant_arcanum.registry.ModBlocks;
import end3r.verdant_arcanum.registry.ModEntities;
import end3r.verdant_arcanum.registry.ModItems;
import end3r.verdant_arcanum.registry.ModTags;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.item.Item;
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

        // Spawn magic particles occasionally
        if (this.world.isClient() && this.random.nextInt(10) == 0) {
            double d = this.getX() + (this.random.nextDouble() - 0.5D) * 0.5D;
            double e = this.getY() + 0.3D;
            double f = this.getZ() + (this.random.nextDouble() - 0.5D) * 0.5D;

            this.world.addParticle(ParticleTypes.WITCH, d, e, f, 0.0D, 0.0D, 0.0D);
        }

        // Check for nectar and current pollen type
        if (!this.world.isClient() && this.hasNectar() && this.currentPollenType != null) {
            BlockPos pos = this.getBlockPos();

            // Check a 3x3x3 area around the bee for magic hives
            for (BlockPos checkPos : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
                BlockState state = this.world.getBlockState(checkPos);
                if (state.getBlock() instanceof MagicHiveBlock) {
                    // Deposit the pollen as essence
                    MagicHiveBlock hiveBlock = (MagicHiveBlock) state.getBlock();
                    Item essenceToDeposit = BLOOM_TO_ESSENCE_MAP.get(this.currentPollenType);

                    if (essenceToDeposit != null && hiveBlock.tryDepositEssence(world, checkPos, state, essenceToDeposit)) {
                        // Reset nectar state - we need to handle this differently
                        // Use a direct field access via reflection or let the bee naturally complete its cycle
                        // For now, we'll let the natural behavior handle resetting nectar
                        this.currentPollenType = null;
                        break;
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

    // Public method to be called by the BeeFlowerEventMixin when a magical flower is visited
    public void visitFlower(BlockPos pos) {
        if (!this.world.isClient()) {
            BlockState blockState = this.world.getBlockState(pos);

            // Determine which bloom type we're collecting from based on the block
            if (blockState.isIn(ModTags.Blocks.FLAME_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.FLAME_FLOWER_BLOOM;
            } else if (blockState.isIn(ModTags.Blocks.BLINK_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.BLINK_FLOWER_BLOOM;
            } else if (blockState.isIn(ModTags.Blocks.ROOTGRASP_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.ROOTGRASP_FLOWER_BLOOM;
            } else if (blockState.isIn(ModTags.Blocks.GUST_FLOWERS_IN_BLOOM)) {
                this.currentPollenType = ModItems.GUST_FLOWER_BLOOM;
            }
        }
    }

    @Override
    public BeeEntity createChild(ServerWorld world, PassiveEntity entity) {
        // Return a new magic infused bee for breeding
        return (BeeEntity) ModEntities.MAGIC_INFUSED_BEE.create(world);
    }
    public void setCurrentPollenType(Item type) {
        this.currentPollenType = type;
    }
}