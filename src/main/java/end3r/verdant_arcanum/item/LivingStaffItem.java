package end3r.verdant_arcanum.item;

import end3r.verdant_arcanum.magic.ManaSystem;
import end3r.verdant_arcanum.registry.SpellRegistry;
import end3r.verdant_arcanum.screen.LivingStaffScreenHandlerFactory;
import end3r.verdant_arcanum.spell.Spell;
import end3r.verdant_arcanum.util.TooltipUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class LivingStaffItem extends Item {
    // Constants
    public static final int MAX_SLOTS = 3;
    public static final String ACTIVE_SLOT_KEY = "ActiveSlot";
    public static final String SLOT_PREFIX = "Slot_";

    public LivingStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack staffStack = player.getStackInHand(hand);

        // Handle sneaking to open GUI instead of casting
        if (player.isSneaking()) {
            if (!world.isClient) {
                // Open the GUI
                player.openHandledScreen(new LivingStaffScreenHandlerFactory(staffStack));
            }
            return TypedActionResult.success(staffStack);
        } else {
            // Cast the spell in the active slot
            return castSpell(world, player, staffStack);
        }
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, net.minecraft.entity.LivingEntity entity, Hand hand) {
        // Prevent using the staff when right-clicking entities
        return ActionResult.PASS;
    }

    // Method made public static to be used by the GUI
    public static int findNextActiveSlot(ItemStack staffStack, int currentSlot) {
        NbtCompound nbt = staffStack.getOrCreateNbt();

        // Start from the next slot and wrap around if needed
        for (int i = 1; i <= MAX_SLOTS; i++) {
            int nextSlot = (currentSlot + i) % MAX_SLOTS;
            String slotKey = SLOT_PREFIX + nextSlot;

            // If this slot has a spell, use it
            if (nbt.contains(slotKey) && !nbt.getString(slotKey).isEmpty()) {
                return nextSlot;
            }
        }

        // If no other slots have spells, just return the current slot
        return currentSlot;
    }

    private TypedActionResult<ItemStack> castSpell(World world, PlayerEntity player, ItemStack staffStack) {
        NbtCompound nbt = staffStack.getNbt();
        if (nbt == null) return TypedActionResult.pass(staffStack);

        int activeSlot = nbt.getInt(ACTIVE_SLOT_KEY);
        String slotKey = SLOT_PREFIX + activeSlot;

        // Check if the active slot has a spell
        if (!nbt.contains(slotKey) || nbt.getString(slotKey).isEmpty()) {
            // No spell in this slot - make a "failed" sound
            if (world.isClient) {
                world.playSound(player, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS,
                        0.5F, 1.2F);
            }
            return TypedActionResult.fail(staffStack);
        }

        // Get the ID of the spell essence in this slot
        String spellEssenceId = nbt.getString(slotKey);

        // Find the corresponding spell
        Spell spell = SpellRegistry.getSpellFromEssenceId(spellEssenceId);
        if (spell == null) return TypedActionResult.pass(staffStack);

        // Get the mana cost
        int manaCost = spell.getManaCost();

        // Check if player is on cooldown
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(staffStack);
        }

        // Check if player has enough mana
        ManaSystem manaSystem = ManaSystem.getInstance();

        if (!world.isClient) {  // SERVER-SIDE ONLY
            // Only attempt to cast the spell on the server side
            if (manaSystem.useMana(player, manaCost)) {
                // Cast the spell
                spell.cast(world, player);

                // Apply cooldown
                player.getItemCooldownManager().set(this, 20); // 1 second cooldown

                return TypedActionResult.success(staffStack);
            } else {
                // Not enough mana - play failure sound
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS,
                        0.5F, 1.2F);

                return TypedActionResult.fail(staffStack);
            }
        } else {  // CLIENT-SIDE ONLY
            // Check if the player has enough mana (client-side prediction)
            if (manaSystem.hasEnoughMana(player, manaCost)) {
                // Play successful cast effects
                spell.playClientEffects(world, player);
            } else {
                // Play failure effects
                spell.playFailureEffects(world, player);
                // Play the failure sound
                world.playSound(player, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS,
                        0.5F, 1.2F);

                // Add smoke particles to indicate failure (add this code)
                for (int i = 0; i < 5; i++) {
                    world.addParticle(
                            net.minecraft.particle.ParticleTypes.SMOKE,
                            player.getX(),
                            player.getY() + player.getStandingEyeHeight() - 0.1,
                            player.getZ(),
                            world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02,
                            world.random.nextGaussian() * 0.02
                    );
                }
            }

            return TypedActionResult.success(staffStack);
        }
    }


    // Method to handle scroll wheel spell switching
    public static void handleScrollWheel(World world, PlayerEntity player, ItemStack staffStack, int direction) {
        if (player.isSneaking()) {
            NbtCompound nbt = staffStack.getOrCreateNbt();
            int currentSlot = nbt.getInt(ACTIVE_SLOT_KEY);

            // Find the next valid slot in the scroll direction
            for (int i = 1; i <= MAX_SLOTS; i++) {
                int nextSlot = Math.floorMod(currentSlot + (i * direction), MAX_SLOTS);
                String slotKey = SLOT_PREFIX + nextSlot;

                // Check if this slot has a spell
                if (nbt.contains(slotKey) && !nbt.getString(slotKey).isEmpty()) {
                    // Set the new active slot
                    nbt.putInt(ACTIVE_SLOT_KEY, nextSlot);

                    // Play a slot change sound
                    if (world != null) {  // Added null check for safety
                        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.PLAYERS,
                                0.6F, 1.0F + (world.random.nextFloat() * 0.2F));
                    }
                    break;
                }
            }
        }
    }

    // Allow for grafting spell essences onto the staff
    public static TypedActionResult<ItemStack> graftSpellEssence(World world, PlayerEntity player,
                                                                 ItemStack staffStack, ItemStack essenceStack) {
        if (!(essenceStack.getItem() instanceof SpellEssenceItem spellEssence)) {
            return TypedActionResult.pass(staffStack);
        }

        // Get the first available slot or return failure if full
        int slot = findFirstEmptySlot(staffStack);
        if (slot == -1) {
            // Staff is full, play failure sound
            if (!world.isClient) {
                world.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS,
                        0.5F, 1.2F);
            }
            return TypedActionResult.fail(staffStack);
        }

        // Store the spell essence ID in the staff NBT
        NbtCompound nbt = staffStack.getOrCreateNbt();
        String slotKey = SLOT_PREFIX + slot;
        String essenceId = net.minecraft.util.registry.Registry.ITEM.getId(spellEssence).toString();
        nbt.putString(slotKey, essenceId);

        // If this is the first spell grafted, make it the active slot
        if (!nbt.contains(ACTIVE_SLOT_KEY)) {
            nbt.putInt(ACTIVE_SLOT_KEY, slot);
        }

        // Play a success sound and visual effect
        if (!world.isClient) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS,
                    0.5F, 1.0F);

            // Remove one essence from the stack
            essenceStack.decrement(1);
        }

        return TypedActionResult.success(staffStack);
    }

    private static int findFirstEmptySlot(ItemStack staffStack) {
        NbtCompound nbt = staffStack.getOrCreateNbt();

        for (int i = 0; i < MAX_SLOTS; i++) {
            String slotKey = SLOT_PREFIX + i;
            // Check if the slot key doesn't exist or has an empty value
            if (!nbt.contains(slotKey) || nbt.getString(slotKey).isEmpty()) {
                return i;
            }
        }

        return -1; // No empty slots
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        int activeSlot = nbt != null ? nbt.getInt(ACTIVE_SLOT_KEY) : 0;

        // Basic info about the staff
        TooltipUtils.addTooltipWithShift(
                stack, world, tooltip, context,
                // Basic info supplier
                () -> {
                    Text[] basicInfo = new Text[]{
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff", Formatting.GREEN),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff.slots", Formatting.GOLD, MAX_SLOTS)
                    };

                    // Add active slot info if spells are grafted
                    if (nbt != null) {
                        Text[] spellInfo = getGraftedSpellsInfo(nbt, activeSlot);

                        // Combine the arrays
                        Text[] combined = new Text[basicInfo.length + spellInfo.length];
                        System.arraycopy(basicInfo, 0, combined, 0, basicInfo.length);
                        System.arraycopy(spellInfo, 0, combined, basicInfo.length, spellInfo.length);

                        return combined;
                    }

                    return basicInfo;
                },
                // Detailed info supplier (shown when shift is pressed)
                () -> new Text[]{
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff.detailed.1", Formatting.AQUA),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff.detailed.2", Formatting.YELLOW),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff.detailed.3", Formatting.YELLOW),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff.scroll_hint", Formatting.GREEN)
                }
        );

        super.appendTooltip(stack, world, tooltip, context);
    }

    private Text[] getGraftedSpellsInfo(NbtCompound nbt, int activeSlot) {
        // Count how many spells are grafted
        int spellCount = 0;
        for (int i = 0; i < MAX_SLOTS; i++) {
            String slotKey = SLOT_PREFIX + i;
            if (nbt.contains(slotKey) && !nbt.getString(slotKey).isEmpty()) {
                spellCount++;
            }
        }

        // Create the text array
        Text[] spellInfo = new Text[spellCount + 1]; // +1 for the header
        spellInfo[0] = TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff.grafted_spells", Formatting.BLUE);

        // Add each grafted spell
        int index = 1;
        for (int i = 0; i < MAX_SLOTS; i++) {
            String slotKey = SLOT_PREFIX + i;
            if (nbt.contains(slotKey) && !nbt.getString(slotKey).isEmpty()) {
                String essenceId = nbt.getString(slotKey);
                Spell spell = SpellRegistry.getSpellFromEssenceId(essenceId);
                String essenceType = spell != null ? spell.getType() : "Unknown";

                // Mark the active slot with an indicator
                Formatting formatting = i == activeSlot ? Formatting.GREEN : Formatting.GRAY;
                String prefix = i == activeSlot ? "➤ " : "  ";

                spellInfo[index] = TooltipUtils.createTooltip(
                        "tooltip.verdant_arcanum.living_staff.spell_slot",
                        formatting, prefix, i + 1, essenceType);

                index++;
            }
        }

        return spellInfo;
    }
}