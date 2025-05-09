package end3r.verdant_arcanum.item;

import end3r.verdant_arcanum.magic.ManaSystem;
import end3r.verdant_arcanum.registry.SpellRegistry;
import end3r.verdant_arcanum.spell.Spell;
import end3r.verdant_arcanum.util.TooltipUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.world.World;

import java.util.List;

public class SpellEssenceItem extends Item {
    private final String essenceType;
    private static final int COOLDOWN_TICKS = 20; // 1 second cooldown

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

        // Try case-insensitive lookup first
        Spell spell = SpellRegistry.getSpell(essenceType);

        // If not found, try with first letter capitalized (this is for backward compatibility)
        if (spell == null && essenceType.length() > 0) {
            String capitalizedType = essenceType.substring(0, 1).toUpperCase() +
                    (essenceType.length() > 1 ? essenceType.substring(1) : "");
            spell = SpellRegistry.getSpell(capitalizedType);
        }

        // If still not found, try all uppercase
        if (spell == null) {
            spell = SpellRegistry.getSpell(essenceType.toUpperCase());
        }

        if (spell == null) {
            // For debugging: print what we're searching for
            if (!world.isClient) {
                System.out.println("Could not find spell for essence type: " + essenceType);
            }
            return TypedActionResult.pass(itemStack);
        }

        // Get mana cost
        int manaCost = spell.getManaCost();

        // Check if player has enough mana
        ManaSystem manaSystem = ManaSystem.getInstance();

        // Only cast the spell on the server side if player has enough mana
        if (!world.isClient) {
            if (manaSystem.useMana(player, manaCost)) {
                // Cast the spell
                spell.cast(world, player);

                // Apply cooldown
                player.getItemCooldownManager().set(this, COOLDOWN_TICKS);

                // Increment the player's "used item" statistic
                player.incrementStat(Stats.USED.getOrCreateStat(this));

                // Create a copy of the stack to modify
                ItemStack resultStack = itemStack.copy();

                // Consume the item by decreasing its count by 1
                resultStack.decrement(1);

                // Always return consume result - this tells Minecraft to update the player's inventory
                return TypedActionResult.consume(resultStack);
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
                spell.playClientEffects(world, player);
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

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        Spell spell = SpellRegistry.getSpell(essenceType);
        int manaCost = spell != null ? spell.getManaCost() : 10;

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
                                Formatting.BLUE, formatArg(manaCost)),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.consumable",
                                Formatting.RED)
                },
                // Detailed info supplier (shown when shift is pressed)
                () -> new Text[] {
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence.detailed.1", Formatting.GOLD),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence.detailed.2", Formatting.GOLD),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence." + essenceType.toLowerCase() + ".spell",
                                getFormattingForType(essenceType)),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.spell_essence.consumed", Formatting.RED)
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
            case "blink" -> Formatting.LIGHT_PURPLE;
            case "gust" -> Formatting.AQUA;
            case "rootgrasp" -> Formatting.GREEN;
            case "timestop" -> Formatting.GOLD;
            case "breezevine" -> Formatting.BLUE;
            case "solarbloom" -> Formatting.YELLOW;
            case "phantomstep" -> Formatting.BLUE;

            default -> Formatting.GRAY;
        };
    }
}