package end3r.verdant_arcanum.block.entity;

import end3r.verdant_arcanum.registry.ModBlockEntities;
import end3r.verdant_arcanum.registry.ModTags;
import end3r.verdant_arcanum.screen.MagicHiveScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class MagicHiveBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SidedInventory, ImplementedInventory {
    // The hive inventory - 20 slots for essences
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(20, ItemStack.EMPTY);

    // Tracks when to produce new essences
    private int essenceProductionTicks = 0;

    // Debug flag
    private static final boolean DEBUG_MODE = true;

    public MagicHiveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAGIC_HIVE_ENTITY, pos, state);
    }

    // Static ticker method
    public static void tick(World world, BlockPos pos, BlockState state, MagicHiveBlockEntity blockEntity) {
        if (world.isClient) return;

        // Any periodic processing can go here
        blockEntity.essenceProductionTicks--;
    }

    public boolean addEssence(ItemStack essence) {
        if (DEBUG_MODE) {
            System.out.println("MagicHive at " + pos + " attempting to add essence: " + essence.getItem());
        }

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);

            if (stack.isEmpty()) {
                // Empty slot - add the essence
                inventory.set(i, essence);
                markDirty();  // Make sure this change is saved
                if (DEBUG_MODE) System.out.println("Added essence to empty slot " + i);

                // Additional sync to ensure state is updated
                if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);

                return true;
            } else if (ItemStack.canCombine(stack, essence) && stack.getCount() < stack.getMaxCount()) {
                // Matching essence with space - increase stack
                stack.increment(1);
                markDirty();  // Make sure this change is saved
                if (DEBUG_MODE) System.out.println("Incremented essence in slot " + i + " to " + stack.getCount());

                // Additional sync to ensure state is updated
                if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);

                return true;
            }
        }

        if (DEBUG_MODE) System.out.println("Failed to add essence - inventory full");
        return false; // Inventory is full
    }

    // Get total number of essences in the hive
    public int getEssenceCount() {
        int total = 0;
        for (ItemStack stack : inventory) {
            total += stack.getCount();
        }
        return total;
    }

    // Save inventory to NBT
    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        Inventories.writeNbt(nbt, inventory);
        nbt.putInt("EssenceProductionTicks", essenceProductionTicks);
    }

    // Load inventory from NBT
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        Inventories.readNbt(nbt, inventory);
        essenceProductionTicks = nbt.getInt("EssenceProductionTicks");
    }

    // NamedScreenHandlerFactory implementation
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.verdant_arcanum.magic_hive");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new MagicHiveScreenHandler(syncId, playerInventory, this);
    }

    // ImplementedInventory implementation
    @Override
    public DefaultedList<ItemStack> getItems() {
        return inventory;
    }

    // SidedInventory implementation for automation compatibility
    private static final int[] AVAILABLE_SLOTS = new int[20];
    static {
        for (int i = 0; i < 20; i++) {
            AVAILABLE_SLOTS[i] = i;
        }
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return AVAILABLE_SLOTS;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        // Only allow automation to insert essences
        return stack.isIn(ModTags.Items.SPELL_ESSENCES);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        // Allow extraction from any slot
        return true;
    }
}