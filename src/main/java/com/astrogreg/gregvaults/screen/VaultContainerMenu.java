package com.astrogreg.gregvaults.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import com.astrogreg.gregvaults.registry.VaultMenuTypes;
import com.astrogreg.gregvaults.screen.VaultSlot.RemappingHandler;

public class VaultContainerMenu extends AbstractContainerMenu {

    public final IItemHandler vaultHandler;
    public final int totalSlots;

    public static final int COLS = 9;
    public static final int SLOT_SIZE = 18;
    public static final int MAX_ROWS = 6;
    public static final int SLOTS_X = 8;
    public static final int SLOTS_Y = 18;
    private static final int WINDOW_SIZE = MAX_ROWS * COLS;

    public final int visibleRows;
    public final int playerY;
    public final int hotbarY;

    private final RemappingHandler remapping;

    private int[] filteredIndices = null;
    private VaultSortMode sortMode = VaultSortMode.NAME;

    public VaultSortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(VaultSortMode mode) {
        this.sortMode = mode;
        applySortToStorage();
    }

    private void applySortToStorage() {
        if (!(vaultHandler instanceof net.minecraftforge.items.ItemStackHandler handler)) return;
        int slots = handler.getSlots();

        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) stacks.add(s.copy());
        }

        java.util.Comparator<ItemStack> cmp = switch (sortMode) {
            case NAME -> java.util.Comparator.comparing(
                    s -> s.getHoverName().getString());
            case COUNT_DESC -> java.util.Comparator.comparingInt(
                    (ItemStack s) -> s.getCount()).reversed();
            case COUNT_ASC -> java.util.Comparator.comparingInt(
                    s -> s.getCount());
        };
        stacks.sort(cmp);

        for (int i = 0; i < slots; i++) {
            handler.setStackInSlot(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
        remapping.setSortedIndices(null);
    }

    public void organize() {
        if (!(vaultHandler instanceof net.minecraftforge.items.ItemStackHandler handler)) return;

        int slots = handler.getSlots();

        for (int i = 0; i < slots; i++) {
            ItemStack stackI = handler.getStackInSlot(i);
            if (stackI.isEmpty()) continue;
            int limit = handler.getSlotLimit(i);
            if (stackI.getCount() >= limit) continue;

            for (int j = i + 1; j < slots; j++) {
                ItemStack stackJ = handler.getStackInSlot(j);
                if (stackJ.isEmpty()) continue;
                if (!ItemStack.isSameItemSameTags(stackI, stackJ)) continue;

                int canTake = Math.min(stackJ.getCount(), limit - stackI.getCount());
                stackI.grow(canTake);
                stackJ.shrink(canTake);
                handler.setStackInSlot(i, stackI);
                handler.setStackInSlot(j, stackJ.isEmpty() ? ItemStack.EMPTY : stackJ);
                if (stackI.getCount() >= limit) break;
            }
        }

        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) stacks.add(s.copy());
        }

        stacks.sort(java.util.Comparator.comparing(s -> s.getHoverName().getString()));

        for (int i = 0; i < slots; i++) {
            handler.setStackInSlot(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
        remapping.setSortedIndices(null);
    }

    public VaultContainerMenu(
            int windowId,
            Inventory playerInv,
            IItemHandler vaultHandler) {
        this(VaultMenuTypes.VAULT_MENU.get(), windowId, playerInv, vaultHandler);
    }

    protected VaultContainerMenu(
            MenuType<?> menuType,
            int windowId,
            Inventory playerInv,
            IItemHandler vaultHandler) {
        this(menuType, windowId, playerInv, vaultHandler, 0);
    }

    protected VaultContainerMenu(
            MenuType<?> menuType,
            int windowId,
            Inventory playerInv,
            IItemHandler vaultHandler,
            int extraPlayerYOffset) {
        this(menuType, windowId, playerInv, vaultHandler, extraPlayerYOffset, MAX_ROWS);
    }

    protected VaultContainerMenu(
            MenuType<?> menuType,
            int windowId,
            Inventory playerInv,
            IItemHandler vaultHandler,
            int extraPlayerYOffset,
            int maxRows) {
        super(menuType, windowId);
        this.vaultHandler = vaultHandler;
        this.totalSlots = vaultHandler.getSlots();

        int usedRows = Math.max(1, (int) Math.ceil(totalSlots / (double) COLS));
        this.visibleRows = Math.min(usedRows, maxRows);
        int visibleSlots = visibleRows * COLS;

        this.playerY = SLOTS_Y + visibleRows * SLOT_SIZE + 14 + extraPlayerYOffset;
        this.hotbarY = this.playerY + 3 * SLOT_SIZE + 4;

        this.remapping = new RemappingHandler(vaultHandler, visibleSlots);

        for (int i = 0; i < visibleSlots; i++) {
            addSlot(
                    new VaultSlot(
                            remapping,
                            i,
                            SLOTS_X + (i % COLS) * SLOT_SIZE,
                            SLOTS_Y + (i / COLS) * SLOT_SIZE));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                        new Slot(
                                playerInv,
                                col + row * 9 + 9,
                                SLOTS_X + col * SLOT_SIZE,
                                this.playerY + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(
                    new Slot(
                            playerInv,
                            col,
                            SLOTS_X + col * SLOT_SIZE,
                            this.hotbarY));
        }
    }

    public VaultContainerMenu(
            int windowId,
            Inventory playerInv,
            FriendlyByteBuf buf) {
        this(windowId, playerInv, new ItemStackHandler(buf.readInt()));
    }

    public void updateScroll(int scrollRow) {
        remapping.setOffset(scrollRow * COLS);
    }

    public void updateSearch(String query) {
        if (query == null || query.isEmpty()) {
            filteredIndices = null;
            remapping.setFilteredIndices(null);
        } else {
            String q = query.toLowerCase();
            java.util.List<Integer> matching = new java.util.ArrayList<>();
            for (int i = 0; i < vaultHandler.getSlots(); i++) {
                ItemStack stack = vaultHandler.getStackInSlot(i);
                if (!stack.isEmpty() &&
                        stack.getHoverName().getString().toLowerCase().contains(q)) {
                    matching.add(i);
                }
            }
            filteredIndices = matching
                    .stream()
                    .mapToInt(Integer::intValue)
                    .toArray();
            remapping.setFilteredIndices(filteredIndices);
        }
    }

    public int[] getFilteredIndices() {
        return filteredIndices;
    }

    public int getVisibleSlotCount() {
        return visibleRows * COLS;
    }

    public int getTotalFilteredRows() {
        int count = filteredIndices != null ? filteredIndices.length : totalSlots;
        return (int) Math.ceil(count / (double) COLS);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void initializeContents(
            int stateId,
            java.util.List<ItemStack> items,
            ItemStack carried) {
        if (items.size() > this.slots.size()) {
            return;
        }
        super.initializeContents(stateId, items, carried);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem().copy();
        ItemStack original = stack.copy();
        int vaultCount = getVisibleSlotCount();
        if (index < vaultCount) {
            if (!moveItemStackTo(stack, vaultCount, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            stack = insertIntoFullVault(stack);
            if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.onTake(player, stack);
        return original;
    }

    protected ItemStack insertIntoFullVault(ItemStack stack) {
        if (!(vaultHandler instanceof net.minecraftforge.items.ItemStackHandler handler)) {
            moveItemStackTo(stack, 0, getVisibleSlotCount(), false);
            return stack;
        }
        int slots = handler.getSlots();
        for (int i = 0; i < slots && !stack.isEmpty(); i++) {
            ItemStack existing = handler.getStackInSlot(i);
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, stack)) continue;
            int limit = Math.min(handler.getSlotLimit(i), existing.getMaxStackSize());
            int canFit = limit - existing.getCount();
            if (canFit <= 0) continue;
            int moved = Math.min(canFit, stack.getCount());
            existing.grow(moved);
            stack.shrink(moved);
            handler.setStackInSlot(i, existing);
        }
        for (int i = 0; i < slots && !stack.isEmpty(); i++) {
            if (!handler.getStackInSlot(i).isEmpty()) continue;
            int limit = Math.min(handler.getSlotLimit(i), stack.getMaxStackSize());
            int moved = Math.min(limit, stack.getCount());
            handler.setStackInSlot(i, stack.copyWithCount(moved));
            stack.shrink(moved);
        }
        return stack;
    }
}