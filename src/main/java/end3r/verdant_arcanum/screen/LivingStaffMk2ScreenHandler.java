package end3r.verdant_arcanum.screen;

import end3r.verdant_arcanum.VerdantArcanum;
import end3r.verdant_arcanum.item.LivingStaffMk2Item;
import end3r.verdant_arcanum.item.SpellEssenceItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.network.PacketByteBuf;

public class LivingStaffMk2ScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    private final ItemStack staffStack;
    private final PlayerEntity player;

    // Property delegate indexes (same as LivingStaffScreenHandler)
    public static final int ACTIVE_SLOT_INDEX = 0;
    public static final int PROPERTY_COUNT = 1;

    // Static registry for screen handler type
    public static final Identifier SCREEN_ID = new Identifier(VerdantArcanum.MOD_ID, "living_staff_mk2_screen");
    public static ScreenHandlerType<LivingStaffMk2ScreenHandler> HANDLER_TYPE;

    // Register the screen handler type
    public static void register() {
        HANDLER_TYPE = ScreenHandlerRegistry.registerExtended(SCREEN_ID,
                (syncId, inventory, buf) -> {
                    ItemStack staffStack = buf.readItemStack();
                    return new LivingStaffMk2ScreenHandler(syncId, inventory, staffStack);
                }
        );
    }

    // Constructor used by the client
    public LivingStaffMk2ScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, buf.readItemStack());
    }

    // Main constructor
    public LivingStaffMk2ScreenHandler(int syncId, PlayerInventory playerInventory, ItemStack staffStack) {
        super(HANDLER_TYPE, syncId);
        this.player = playerInventory.player;
        this.staffStack = staffStack;
        this.inventory = new SimpleInventory(LivingStaffMk2Item.MAX_SLOTS);

        // Set up property delegate for tracking active slot
        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                if (index == ACTIVE_SLOT_INDEX) {
                    return staffStack.getOrCreateNbt().getInt(LivingStaffMk2Item.ACTIVE_SLOT_KEY);
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == ACTIVE_SLOT_INDEX) {
                    staffStack.getOrCreateNbt().putInt(LivingStaffMk2Item.ACTIVE_SLOT_KEY, value);
                }
            }

            @Override
            public int size() {
                return PROPERTY_COUNT;
            }
        };

        // Load slots from staff NBT
        loadSlotsFromStaff();

        // Add the spell essence slots - now 5 instead of 3
        for (int i = 0; i < LivingStaffMk2Item.MAX_SLOTS; i++) {
            final int slotIndex = i;
            this.addSlot(new Slot(inventory, i, 44 + i * 18, 20) {
                @Override
                public boolean canInsert(ItemStack stack) {
                    return stack.getItem() instanceof SpellEssenceItem;
                }

                @Override
                public void setStack(ItemStack stack) {
                    super.setStack(stack);
                    updateStaffNbt(slotIndex, stack);
                }

                @Override
                public ItemStack takeStack(int amount) {
                    ItemStack result = super.takeStack(amount);
                    if (amount > 0) {
                        updateStaffNbt(slotIndex, getStack());
                    }
                    return result;
                }

                @Override
                public boolean canTakeItems(PlayerEntity playerEntity) {
                    return true;
                }
            });
        }

        // Add player inventory slots
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(playerInventory, x + y * 9 + 9, 9 + x * 18, 71 + y * 18));
            }
        }

        // Add player hotbar slots
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }

        // Add property delegate for tracking active slot
        this.addProperties(propertyDelegate);
    }

    // Load inventory slots from staff NBT
    private void loadSlotsFromStaff() {
        NbtCompound nbt = staffStack.getOrCreateNbt();

        for (int i = 0; i < LivingStaffMk2Item.MAX_SLOTS; i++) {
            String slotKey = LivingStaffMk2Item.SLOT_PREFIX + i;
            if (nbt.contains(slotKey) && !nbt.getString(slotKey).isEmpty()) {
                String essenceId = nbt.getString(slotKey);
                Identifier id = new Identifier(essenceId);
                ItemStack essenceStack = new ItemStack(Registry.ITEM.get(id));
                this.inventory.setStack(i, essenceStack);
            }
        }
    }

    // Update staff NBT when inventory changes
    private void updateStaffNbt(int slotIndex, ItemStack stack) {
        NbtCompound nbt = staffStack.getOrCreateNbt();
        String slotKey = LivingStaffMk2Item.SLOT_PREFIX + slotIndex;

        if (stack.isEmpty()) {
            // Remove the spell from this slot
            nbt.putString(slotKey, "");

            // If this was the active slot, find next valid slot
            if (propertyDelegate.get(ACTIVE_SLOT_INDEX) == slotIndex) {
                int nextSlot = LivingStaffMk2Item.findNextActiveSlotMk2(staffStack, slotIndex);
                propertyDelegate.set(ACTIVE_SLOT_INDEX, nextSlot);
            }
        } else {
            // Add the spell to this slot
            String essenceId = Registry.ITEM.getId(stack.getItem()).toString();
            nbt.putString(slotKey, essenceId);

            // If no active slot is set, make this one active
            if (!nbt.contains(LivingStaffMk2Item.ACTIVE_SLOT_KEY)) {
                propertyDelegate.set(ACTIVE_SLOT_INDEX, slotIndex);
            }
        }

        // Play a sound effect
        if (player.world.isClient) {
            player.world.playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.PLAYERS,
                    0.6F, 1.0F + (player.world.random.nextFloat() * 0.2F));
        }
    }

    // Change the active spell slot
    public void changeActiveSlot(int newSlot) {
        if (newSlot >= 0 && newSlot < LivingStaffMk2Item.MAX_SLOTS) {
            NbtCompound nbt = staffStack.getOrCreateNbt();
            String slotKey = LivingStaffMk2Item.SLOT_PREFIX + newSlot;

            // Only allow changing to slots that have spells
            if (nbt.contains(slotKey) && !nbt.getString(slotKey).isEmpty()) {
                propertyDelegate.set(ACTIVE_SLOT_INDEX, newSlot);

                // Play a sound effect
                if (player.world.isClient) {
                    player.world.playSound(player, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, SoundCategory.PLAYERS,
                            0.6F, 1.0F + (player.world.random.nextFloat() * 0.2F));
                }
            }
        }
    }

    // Handle scroll events (client-side)
    public void handleScroll(double scrollAmount) {
        if (player.isSneaking()) {
            int currentSlot = propertyDelegate.get(ACTIVE_SLOT_INDEX);
            int direction = scrollAmount > 0 ? 1 : -1;

            // Find the next valid slot in the scroll direction
            for (int i = 1; i <= LivingStaffMk2Item.MAX_SLOTS; i++) {
                int nextSlot = Math.floorMod(currentSlot + (i * direction), LivingStaffMk2Item.MAX_SLOTS);
                String slotKey = LivingStaffMk2Item.SLOT_PREFIX + nextSlot;

                // Check if this slot has a spell
                if (staffStack.getOrCreateNbt().contains(slotKey) &&
                        !staffStack.getOrCreateNbt().getString(slotKey).isEmpty()) {
                    changeActiveSlot(nextSlot);
                    break;
                }
            }
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            result = slotStack.copy();

            if (index < LivingStaffMk2Item.MAX_SLOTS) {
                // Move from staff to inventory
                if (!this.insertItem(slotStack, LivingStaffMk2Item.MAX_SLOTS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotStack.getItem() instanceof SpellEssenceItem) {
                // Move spell essence from inventory to staff
                if (!this.insertItem(slotStack, 0, LivingStaffMk2Item.MAX_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }

            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTakeItem(player, slotStack);
        }

        return result;
    }

    public int getActiveSlot() {
        return propertyDelegate.get(ACTIVE_SLOT_INDEX);
    }
}