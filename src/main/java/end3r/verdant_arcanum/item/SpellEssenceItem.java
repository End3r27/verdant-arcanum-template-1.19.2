package end3r.verdant_arcanum.item;

import end3r.verdant_arcanum.magic.ManaSystem;
import end3r.verdant_arcanum.util.TooltipUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class SpellEssenceItem extends Item {
    private final String essenceType;
    private static final int COOLDOWN_TICKS = 20; // 1 second cooldown

    // Mana costs for different spell types
    private static final int FLAME_SPELL_MANA_COST = 25;

    public SpellEssenceItem(String essenceType, Settings settings) {
        super(settings);
        this.essenceType = essenceType;
    }

    public String getEssenceType() {
        return this.essenceType;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack itemStack = player.getStackInHand(hand);

        // If the player is on cooldown, just return
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(itemStack);
        }

        // Get mana cost based on spell type
        int manaCost = getManaCost();

        // Check if player has enough mana
        ManaSystem manaSystem = ManaSystem.getInstance();

        // Only cast the spell on the server side if player has enough mana
        if (!world.isClient) {
            if (manaSystem.useMana(player, manaCost)) {
                // Apply spell effect based on essence type
                if ("Flame".equalsIgnoreCase(essenceType)) {
                    castFlameSpell(world, player);
                }
                // Add more spell types here as you develop them

                // Apply cooldown
                player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

                // Increment the player's "used item" statistic
                player.incrementStat(Stats.USED.getOrCreateStat(this));
            } else {
                // Not enough mana - play failure sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS,
                        0.5F, 1.2F);

                return TypedActionResult.fail(itemStack);
            }
        } else {
            // On client side, check if player has enough mana to show appropriate effects
            if (manaSystem.getPlayerMana(player).getCurrentMana() >= manaCost) {
                // Play visual/sound effects on client side
                playSpellCastEffects(world, player);
            } else {
                // Not enough mana - play client-side failure effects
                for (int i = 0; i < 5; i++) {
                    world.addParticle(
                            ParticleTypes.SMOKE,
                            player.getX(),
                            player.getY() + player.getStandingEyeHeight() - 0.1,
                            player.getZ(),
                            world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02
                    );
                }
            }
        }

        return TypedActionResult.success(itemStack);
    }

    private int getManaCost() {
        return switch (essenceType.toLowerCase()) {
            case "flame" -> FLAME_SPELL_MANA_COST;
            // Add other spell types here
            default -> 10; // Default mana cost
        };
    }

    private void castFlameSpell(World world, PlayerEntity player) {
        // Calculate fireball direction from where the player is looking
        Vec3d lookVec = player.getRotationVector();

        // Create a small fireball entity
        SmallFireballEntity fireball = new SmallFireballEntity(
                world,
                player.getX() + lookVec.x * 0.5,
                player.getY() + player.getStandingEyeHeight() - 0.1,
                player.getZ() + lookVec.z * 0.5,
                lookVec.x * 0.5,
                lookVec.y * 0.5,
                lookVec.z * 0.5
        );

        // Set the fireball owner to the player
        fireball.setOwner(player);

        // Spawn the fireball in the world
        world.spawnEntity(fireball);

        // Play sound effect
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS,
                0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
    }

    private void playSpellCastEffects(World world, PlayerEntity player) {
        // Add client-side particle effects based on essence type
        if ("Flame".equalsIgnoreCase(essenceType)) {
            // Spawn flame particles around the player
            Vec3d lookVec = player.getRotationVector();
            double x = player.getX() + lookVec.x * 0.5;
            double y = player.getY() + player.getStandingEyeHeight() - 0.1;
            double z = player.getZ() + lookVec.z * 0.5;

            for (int i = 0; i < 10; i++) {
                world.addParticle(
                        ParticleTypes.FLAME,
                        x + world.random.nextGaussian() * 0.1,
                        y + world.random.nextGaussian() * 0.1,
                        z + world.random.nextGaussian() * 0.1,
                        lookVec.x * 0.2 + world.random.nextGaussian() * 0.02,
                        lookVec.y * 0.2 + world.random.nextGaussian() * 0.02,
                        lookVec.z * 0.2 + world.random.nextGaussian() * 0.02
                );
            }
        }
        // Add more essence type effects here as needed
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        int manaCost = getManaCost();

        TooltipUtils.addTooltipWithShift(
                stack, world, tooltip, context,
                // Basic info supplier
                () -> new Text[] {
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence", Formatting.AQUA),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence." + essenceType.toLowerCase(),
                                getFormattingForType(essenceType)),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence.usage",
                                Formatting.YELLOW),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.mana_cost",
                                Formatting.BLUE, formatArg(manaCost))
                },
                // Detailed info supplier (shown when shift is pressed)
                () -> new Text[] {
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence.detailed.1", Formatting.GOLD),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence.detailed.2", Formatting.GOLD),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence." + essenceType.toLowerCase() + ".spell",
                                getFormattingForType(essenceType))
                }
        );
        super.appendTooltip(stack, world, tooltip, context);
    }

    private Object[] formatArg(Object... args) {
        return args;
    }

    private Formatting getFormattingForType(String type) {
        return switch (type.toLowerCase()) {
            case "flame" -> Formatting.RED;
            case "water" -> Formatting.BLUE;
            case "earth" -> Formatting.DARK_GREEN;
            case "air" -> Formatting.WHITE;
            default -> Formatting.GRAY;
        };
    }
}