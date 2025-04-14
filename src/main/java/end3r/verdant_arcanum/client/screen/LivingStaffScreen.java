package end3r.verdant_arcanum.screen;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.item.LivingStaffItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class LivingStaffScreenHandler extends ScreenHandler {
    private final ItemStack staffStack;
    private final Hand hand;
    private final SimpleInventory spellInventory;
    private int activeSlot = 0;
    private final PlayerEntity player;

    public LivingStaffScreenHandler(int syncId, PlayerInventory playerInventory, Hand hand) {
        super(VerdantArcanum.LIVING_STAFF_SCREEN_HANDLER, syncId);
        this.player = playerInventory.player;
        this.hand = hand;
        this.staffStack = playerInventory.player.getStackInHand(hand);
        this.spellInventory = new SimpleInventory(3); // 3 spell slots

        // Load spells from staff's NBT
        loadSpellsFromNbt();

        // Initialize active slot from NBT
        if (staffStack.hasNbt() && staffStack.getNbt().contains("ActiveSlot", NbtElement.NUMBER_TYPE)) {
            this.activeSlot = staffStack.getNbt().getInt("ActiveSlot");
        }

        // Add spell slots
        for (int i = 0; i < 3; i++) {
            this.addSlot(new SpellSlot(spellInventory, i, 62 + i * 26, 37));
        }

        // Add player inventory slots
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
            }
        }

        // Add player hotbar slots
        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 142));
        }
    }

    private void loadSpellsFromNbt() {
        if (staffStack.hasNbt() && staffStack.getNbt().contains("Spells", NbtElement.LIST_TYPE)) {
            NbtList spellList = staffStack.getNbt().getList("Spells", NbtElement.COMPOUND_TYPE);
            for (int i = 0; i < Math.min(spellList.size(), 3); i++) {
                NbtCompound spellNbt = spellList.getCompound(i);
                // Create a spell item stack from the NBT data
                ItemStack spellStack = new ItemStack(VerdantArcanum.SPELL_ITEM);
                spellStack.setNbt(spellNbt);
                spellInventory.setStack(i, spellStack);
            }
        }
    }

    private void saveSpellsToNbt() {
        NbtCompound nbt = staffStack.getOrCreateNbt();
        NbtList spellList = new NbtList();

        for (int i = 0; i < 3; i++) {
            ItemStack spellStack = spellInventory.getStack(i);
            if (!spellStack.isEmpty()) {
                spellList.add(spellStack.getNbt().copy());
            } else {
                spellList.add(new NbtCompound()); // Empty slot
            }
        }

        nbt.put("Spells", spellList);
        nbt.putInt("ActiveSlot", activeSlot);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.staffStack.getItem() instanceof LivingStaffItem;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        saveSpellsToNbt();
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        // Handle shift-clicking items between inventory and spell slots
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasStack()) {
            ItemStack stackInSlot = slot.getStack();
            originalStack = stackInSlot.copy();

            if (index < 3) {
                // From spell slot to inventory
                if (!this.insertItem(stackInSlot, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (isValidSpellItem(stackInSlot)) {
                // From inventory to spell slot
                if (!this.insertItem(stackInSlot, 0, 3, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (stackInSlot.getCount() == originalStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, stackInSlot);
        }

        return originalStack;
    }

    private boolean isValidSpellItem(ItemStack stack) {
        // Check if the item is a valid spell that can be placed in the staff
        return stack.getItem() == VerdantArcanum.SPELL_ITEM;
    }

    public int getActiveSlot() {
        return activeSlot;
    }

    public void setActiveSlot(int slot) {
        if (slot >= 0 && slot < 3) {
            activeSlot = slot;
            saveSpellsToNbt();
        }
    }

    public boolean hasSpellInSlot(int slot) {
        return !spellInventory.getStack(slot).isEmpty();
    }

    public int getSpellIdInSlot(int slot) {
        ItemStack spellStack = spellInventory.getStack(slot);
        if (!spellStack.isEmpty() && spellStack.hasNbt()) {
            return spellStack.getNbt().getInt("SpellId");
        }
        return -1;
    }

    public void rotateSpellsRight() {
        ItemStack slot0 = spellInventory.getStack(0).copy();
        ItemStack slot1 = spellInventory.getStack(1).copy();
        ItemStack slot2 = spellInventory.getStack(2).copy();

        spellInventory.setStack(0, slot2);
        spellInventory.setStack(1, slot0);
        spellInventory.setStack(2, slot1);

        saveSpellsToNbt();
    }

    public void rotateSpellsLeft() {
        ItemStack slot0 = spellInventory.getStack(0).copy();
        ItemStack slot1 = spellInventory.getStack(1).copy();
        ItemStack slot2 = spellInventory.getStack(2).copy();

        spellInventory.setStack(0, slot1);
        spellInventory.setStack(1, slot2);
        spellInventory.setStack(2, slot0);

        saveSpellsToNbt();
    }

    // Custom slot class for spell slots
    private class SpellSlot extends Slot {
        public SpellSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return isValidSpellItem(stack);
        }
    }
}