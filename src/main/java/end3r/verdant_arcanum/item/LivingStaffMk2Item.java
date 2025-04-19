package end3r.verdant_arcanum.item;

import end3r.verdant_arcanum.magic.ManaSystem;
import end3r.verdant_arcanum.registry.SpellRegistry;
import end3r.verdant_arcanum.screen.LivingStaffMk2ScreenHandlerFactory;
import end3r.verdant_arcanum.spell.Spell;
import end3r.verdant_arcanum.util.TooltipUtils;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

import static end3r.verdant_arcanum.magic.ClientManaData.currentMana;
import static end3r.verdant_arcanum.magic.ClientManaData.maxMana;

public class LivingStaffMk2Item extends LivingStaffItem {
    // Constants
    public static final int MAX_SLOTS = 5; // Increased from 3 to 5
    public static final float MANA_COST_REDUCTION = 0.9f; // 10% reduction (multiply cost by 0.9)



    public LivingStaffMk2Item(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack staffStack = player.getStackInHand(hand);

        // Handle sneaking to open GUI instead of casting
        if (player.isSneaking()) {
            if (!world.isClient) {
                // Open the Mk2 GUI
                player.openHandledScreen(new LivingStaffMk2ScreenHandlerFactory(staffStack));
            }
            return TypedActionResult.success(staffStack);
        } else {
            // Cast the spell in the active slot with reduced cost
            return castSpellWithReduction(world, player, staffStack);
        }
    }

    // Custom method for MK2 staff to handle reduced mana costs
    private TypedActionResult<ItemStack> castSpellWithReduction(World world, PlayerEntity player, ItemStack staffStack) {
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

        // Get the reduced mana cost (10% reduction)
        int originalManaCost = spell.getManaCost(staffStack);
        int reducedManaCost = Math.max(1, (int)(originalManaCost * MANA_COST_REDUCTION));

        // Check if player is on cooldown
        if (player.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(staffStack);
        }


        // Check if player has enough mana
        ManaSystem manaSystem = ManaSystem.getInstance();

        if (!world.isClient) {  // SERVER-SIDE ONLY
            // Only attempt to cast the spell on the server side
            if (manaSystem.useMana(player, reducedManaCost)) {
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
            if (manaSystem.hasEnoughMana(player, reducedManaCost)) {
                // Play successful cast effects
                spell.playClientEffects(world, player);
            } else {
                // Play failure effects
                spell.playFailureEffects(world, player);
                // Play the failure sound
                world.playSound(player, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS,
                        0.5F, 1.2F);

                // Add smoke particles to indicate failure
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

    // Override the mouse scroll method to fix shift + wheel scrolling
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

    // Override handleScrollWheel to use MK2's MAX_SLOTS
    public static void handleScrollWheelMk2(World world, PlayerEntity player, ItemStack staffStack, int direction) {
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
                    if (world != null) {
                        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.PLAYERS,
                                0.6F, 1.0F + (world.random.nextFloat() * 0.2F));
                    }
                    break;
                }
            }
        }
    }

    // Override findNextActiveSlot for MK2's MAX_SLOTS
    public static int findNextActiveSlotMk2(ItemStack staffStack, int currentSlot) {
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

    // Override findFirstEmptySlot for MK2's MAX_SLOTS
    public static int findFirstEmptySlotMk2(ItemStack staffStack) {
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

    // Override graftSpellEssence to use MK2's MAX_SLOTS and methods
    public static TypedActionResult<ItemStack> graftSpellEssenceMk2(World world, PlayerEntity player,
                                                                    ItemStack staffStack, ItemStack essenceStack) {
        if (!(essenceStack.getItem() instanceof SpellEssenceItem spellEssence)) {
            return TypedActionResult.pass(staffStack);
        }

        // Get the first available slot or return failure if full
        int slot = findFirstEmptySlotMk2(staffStack);
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

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        int activeSlot = nbt != null ? nbt.getInt(ACTIVE_SLOT_KEY) : 0;

        // Basic info about the MK2 staff
        TooltipUtils.addTooltipWithShift(
                stack, world, tooltip, context,
                // Basic info supplier
                () -> {
                    Text[] basicInfo = new Text[]{
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff_mk2", Formatting.GREEN),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff.slots", Formatting.GOLD, MAX_SLOTS),
                            TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff_mk2.mana_reduction",
                                    Formatting.AQUA, (int)((1 - MANA_COST_REDUCTION) * 100))
                    };

                    // Add active slot info if spells are grafted
                    if (nbt != null) {
                        Text[] spellInfo = getGraftedSpellsInfoMk2(nbt, activeSlot);

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
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff_mk2.detailed.4", Formatting.LIGHT_PURPLE),
                        TooltipUtils.createTooltip("tooltip.verdant_arcanum.living_staff.scroll_hint", Formatting.GREEN)
                }
        );

        // DO NOT call super.appendTooltip() to avoid showing Mk1 tooltips
    }

    private Text[] getGraftedSpellsInfoMk2(NbtCompound nbt, int activeSlot) {
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

                // Get original and reduced mana cost if spell exists
                int originalCost = spell != null ? spell.getManaCost() : 0;
                int reducedCost = spell != null ? Math.max(1, (int)(originalCost * MANA_COST_REDUCTION)) : 0;

                // Mark the active slot with an indicator
                Formatting formatting = i == activeSlot ? Formatting.GREEN : Formatting.GRAY;
                String prefix = i == activeSlot ? "➤ " : "  ";

                // Show the reduced mana cost
                String costDisplay = spell != null ? " (" + reducedCost + " mana)" : "";

                spellInfo[index] = TooltipUtils.createTooltip(
                        "tooltip.verdant_arcanum.living_staff_mk2.spell_slot",
                        formatting, prefix, i + 1, essenceType, costDisplay);

                index++;
            }
        }

        return spellInfo;
    }
}