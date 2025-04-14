package end3r.verdant_arcanum.entity;

import end3r.verdant_arcanum.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;

public class MagicInfusedBee extends BeeEntity {

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

    @Override
    public BeeEntity createChild(ServerWorld world, PassiveEntity entity) {
        // Return a new magic infused bee for breeding
        return (BeeEntity) ModEntities.MAGIC_INFUSED_BEE.create(world);
    }
}