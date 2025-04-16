package end3r.verdant_arcanum.screen;

import end3r.verdant_arcanum.registry.ModScreenHandlers;
import end3r.verdant_arcanum.registry.ModTags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class MagicHiveScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    // This constructor is called on the client side
    public MagicHiveScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(20));
    }

    // This constructor is called from the BlockEntity on the server side
    public MagicHiveScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModScreenHandlers.MAGIC_HIVE_SCREEN_HANDLER, syncId);
        checkSize(inventory, 20);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        //tieni baby ora funziona <3
        // Add the hive inventory slots (3x3)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 5; ++col) {
                this.addSlot(new Slot(inventory, col + row * 3, 45 + col * 18, 7 + row * 18) {
                    // Only allow spell essence items to be added manually
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return stack.isIn(ModTags.Items.SPELL_ESSENCES);
                    }
                });
            }
        }


        // Add the player inventory slots (3 rows of 9)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 9 + col * 18, 71 + row * 18));
            }
        }

        // Add the player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 9 + col * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    // Implementing the abstract method transferSlot (previously called quickMove)
    @Override
    public ItemStack transferSlot(PlayerEntity player, int slotIndex) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot != null && slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotIndex < this.inventory.size()) {
                // Move from hive to player inventory
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (originalStack.isIn(ModTags.Items.SPELL_ESSENCES)) {
                // Move from player inventory to hive
                if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move within player inventory
                if (slotIndex < this.inventory.size() + 27) {
                    // From main inventory to hotbar
                    if (!this.insertItem(originalStack, this.inventory.size() + 27, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // From hotbar to main inventory
                    if (!this.insertItem(originalStack, this.inventory.size(), this.inventory.size() + 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (originalStack.getCount() == newStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, originalStack);
        }

        return newStack;
    }
}