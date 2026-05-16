package com.astrogreg.gregvaults.screen;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import com.astrogreg.gregvaults.registry.VaultMenuTypes;
import com.astrogreg.gregvaults.screen.VaultSlot.RemappingHandler;

import java.util.List;

@SuppressWarnings("all")
public class VaultTerminalMenu extends AbstractContainerMenu {

    public static final int COLS = 9;
    public static final int SLOT_SIZE = 18;
    public static final int SLOTS_X = 8;
    public static final int SLOTS_Y = 18;
    public static final int MAX_ROWS = 6;

    public final IItemHandler vaultHandler;
    public final int totalSlots;
    public final int visibleRows;
    public final int craftSectionY;
    public final int craftGridY;
    public final int craftGridX;
    public final int craftOutX;
    public final int craftOutY;
    public final int playerY;
    public final int hotbarY;

    private final RemappingHandler remapping;
    private int[] filteredIndices = null;
    private VaultSortMode sortMode = VaultSortMode.NAME;
    private boolean sortReversed = false;
    // STACKED_MODE: private VaultDisplayMode displayMode = VaultDisplayMode.SLOTS;
    // STACKED_MODE: private List<AggregatedStack> aggregatedView = java.util.Collections.emptyList();
    private String lastSearchQuery = "";
    public ItemStack[] clientCache = null;

    private boolean refilling = false;

    public final CraftingContainer craftingGrid = new TransientCraftingContainer(this, 3, 3);
    public final ResultContainer craftingResult = new ResultContainer();

    public final int playerSlotsStart;
    public final int craftingSlotsStart;
    public final int craftingOutputStart;
    public final int fullVaultSlotsStart;

    private int lockedMenuSlot = -1;

    // STACKED_MODE: public VaultDisplayMode getDisplayMode() { return displayMode; }
    public boolean isSortReversed() {
        return sortReversed;
    }
    // STACKED_MODE: public List<AggregatedStack> getAggregatedView() { return aggregatedView; }

    public VaultTerminalMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler) {
        super(VaultMenuTypes.VAULT_TERMINAL_MENU.get(), windowId);
        this.vaultHandler = vaultHandler;
        this.totalSlots = vaultHandler.getSlots();

        int usedRows = Math.max(1, (int) Math.ceil(totalSlots / (double) COLS));
        this.visibleRows = Math.min(usedRows, MAX_ROWS);

        this.craftSectionY = 17 + visibleRows * SLOT_SIZE;
        this.craftGridY = craftSectionY + SLOT_SIZE;
        this.craftGridX = SLOTS_X;
        this.craftOutX = craftGridX + 3 * SLOT_SIZE + 27;
        this.craftOutY = craftGridY + SLOT_SIZE;
        this.playerY = craftGridY + 3 * SLOT_SIZE + 14;
        this.hotbarY = playerY + 3 * SLOT_SIZE + 4;

        int windowSize = visibleRows * COLS;
        this.remapping = new RemappingHandler(vaultHandler, windowSize);
        for (int i = 0; i < windowSize; i++) {
            addSlot(new VaultSlot(remapping, i,
                    SLOTS_X + (i % COLS) * SLOT_SIZE,
                    SLOTS_Y + (i / COLS) * SLOT_SIZE));
        }

        ItemStack terminalStack = playerInv.player.getMainHandItem()
                .getItem() instanceof com.astrogreg.gregvaults.items.WirelessTerminalItem ?
                        playerInv.player.getMainHandItem() :
                        playerInv.player.getOffhandItem()
                                .getItem() instanceof com.astrogreg.gregvaults.items.WirelessTerminalItem ?
                                        playerInv.player.getOffhandItem() : ItemStack.EMPTY;
        int terminalInvSlot = -1;
        if (!terminalStack.isEmpty()) {
            for (int i = 0; i < playerInv.getContainerSize(); i++) {
                if (playerInv.getItem(i) == terminalStack) {
                    terminalInvSlot = i;
                    break;
                }
            }
        }

        this.playerSlotsStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int invSlot = col + row * 9 + 9;
                int menuSlot = slots.size();
                Slot s = invSlot == terminalInvSlot ?
                        new LockedSlot(playerInv, invSlot, SLOTS_X + col * SLOT_SIZE, playerY + row * SLOT_SIZE) :
                        new Slot(playerInv, invSlot, SLOTS_X + col * SLOT_SIZE, playerY + row * SLOT_SIZE);
                addSlot(s);
                if (invSlot == terminalInvSlot) lockedMenuSlot = menuSlot;
            }
        }
        for (int col = 0; col < 9; col++) {
            int invSlot = col;
            int menuSlot = slots.size();
            Slot s = invSlot == terminalInvSlot ?
                    new LockedSlot(playerInv, invSlot, SLOTS_X + col * SLOT_SIZE, hotbarY) :
                    new Slot(playerInv, invSlot, SLOTS_X + col * SLOT_SIZE, hotbarY);
            addSlot(s);
            if (invSlot == terminalInvSlot) lockedMenuSlot = menuSlot;
        }

        this.craftingSlotsStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftingGrid, col + row * 3,
                        craftGridX + col * SLOT_SIZE,
                        craftGridY + row * SLOT_SIZE));
            }
        }

        this.craftingOutputStart = slots.size();
        addSlot(new VaultCraftingResultSlot(playerInv.player, craftingGrid, craftingResult, 0,
                craftOutX, craftOutY));

        this.fullVaultSlotsStart = slots.size();
        for (int i = 0; i < vaultHandler.getSlots(); i++) {
            addSlot(new SlotItemHandler(vaultHandler, i, -10000, -10000));
        }

        updateCraftingResult();
    }

    private class VaultCraftingResultSlot extends ResultSlot {

        VaultCraftingResultSlot(Player player, CraftingContainer grid, ResultContainer result,
                                int slot, int x, int y) {
            super(player, grid, result, slot, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            snapshotGridIngredients();
            super.onTake(player, stack);
            refillGridFromVault();
        }
    }

    public VaultTerminalMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, new ItemStackHandler(buf.readInt()));
    }

    private java.util.function.Consumer<ItemStack[]> onGridClose = null;

    public void setOnGridClose(java.util.function.Consumer<ItemStack[]> callback) {
        this.onGridClose = callback;
    }

    public void initCraftingGrid(ItemStack[] saved) {
        if (saved == null) return;
        for (int i = 0; i < Math.min(saved.length, craftingGrid.getContainerSize()); i++) {
            craftingGrid.setItem(i, saved[i] == null ? ItemStack.EMPTY : saved[i].copy());
        }
        updateCraftingResult();
    }

    public void setClientCache(ItemStack[] cache) {
        this.clientCache = cache;
        remapping.setClientCache(cache);
        // STACKED_MODE: rebuildAggregatedView();
        refreshVisibleSlots();
    }

    public void updateClientCacheSlot(int slot, ItemStack stack) {
        if (clientCache != null && slot >= 0 && slot < clientCache.length) {
            clientCache[slot] = stack;
            remapping.setClientCache(clientCache);
            // STACKED_MODE: rebuildAggregatedView();
            refreshVisibleSlots();
        }
    }

    public void refreshVisibleSlots() {
        int visibleCount = getVisibleSlotCount();
        for (int i = 0; i < visibleCount; i++) {
            Slot slot = slots.get(i);
            if (slot instanceof VaultSlot) {
                slot.set(remapping.getStackInSlot(i));
            }
        }
    }

    // STACKED_MODE: private ItemStack[] buildCacheFromHandler() { ... }
    // STACKED_MODE: private void rebuildAggregatedView() { ... }

    public void updateScroll(int scrollRow) {
        remapping.setOffset(scrollRow * COLS);
        refreshVisibleSlots();
    }

    public void updateSearch(String query) {
        this.lastSearchQuery = query == null ? "" : query;
        if (query == null || query.isEmpty()) {
            filteredIndices = null;
            remapping.setFilteredIndices(null);
        } else {
            String q = query.toLowerCase();
            java.util.ArrayList<Integer> matching = new java.util.ArrayList<>();
            for (int i = 0; i < vaultHandler.getSlots(); i++) {
                ItemStack stack = vaultHandler.getStackInSlot(i);
                if (!stack.isEmpty() &&
                        stack.getHoverName().getString().toLowerCase().contains(q)) {
                    matching.add(i);
                }
            }
            filteredIndices = matching.stream().mapToInt(Integer::intValue).toArray();
            remapping.setFilteredIndices(filteredIndices);
        }
        // STACKED_MODE: rebuildAggregatedView();
    }

    public void organize() {
        if (!(vaultHandler instanceof ItemStackHandler handler)) return;
        int size = handler.getSlots();
        java.util.List<ItemStack> items = new java.util.ArrayList<>();
        for (int i = 0; i < size; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) items.add(s.copy());
        }
        items.sort(java.util.Comparator.comparing(s -> s.getHoverName().getString()));
        for (int i = 0; i < size; i++) {
            handler.setStackInSlot(i, i < items.size() ? items.get(i) : ItemStack.EMPTY);
        }
        broadcastChanges();
    }

    public void setSortMode(VaultSortMode mode) {
        this.sortMode = mode;
        // STACKED_MODE: rebuildAggregatedView();
    }

    public void setSortReversed(boolean reversed) {
        this.sortReversed = reversed;
        // STACKED_MODE: rebuildAggregatedView();
    }

    public int getVisibleSlotCount() {
        return visibleRows * COLS;
    }

    public int[] getFilteredIndices() {
        return filteredIndices;
    }

    public int getTotalFilteredRows() {
        // STACKED_MODE: if (displayMode == VaultDisplayMode.STACKED) { ... }
        int count = filteredIndices != null ? filteredIndices.length : totalSlots;
        return (int) Math.ceil(count / (double) COLS);
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == craftingGrid) {
            updateCraftingResult();
        }
    }

    private void refillGridFromVault() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        if (!(vaultHandler instanceof ItemStackHandler handler)) return;

        refilling = true;
        for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
            if (!craftingGrid.getItem(i).isEmpty()) continue;

            ItemStack needed = lastGridIngredients[i];
            if (needed == null || needed.isEmpty()) continue;

            for (int v = 0; v < handler.getSlots(); v++) {
                ItemStack vaultStack = handler.getStackInSlot(v);
                if (!ItemStack.isSameItemSameTags(vaultStack, needed)) continue;

                int take = Math.min(1, vaultStack.getCount());
                vaultStack.shrink(take);
                handler.setStackInSlot(v, vaultStack.isEmpty() ? ItemStack.EMPTY : vaultStack);
                craftingGrid.setItem(i, needed.copyWithCount(take));
                break;
            }
        }
        refilling = false;

        updateCraftingResult();
        broadcastChanges();
    }

    private final ItemStack[] lastGridIngredients = new ItemStack[9];

    public void snapshotGridIngredients() {
        for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
            ItemStack s = craftingGrid.getItem(i);
            lastGridIngredients[i] = s.isEmpty() ? ItemStack.EMPTY : s.copy();
        }
    }

    private Level getLevel() {
        for (Slot slot : slots) {
            if (slot.container instanceof Inventory inv) {
                return inv.player.level();
            }
        }
        return null;
    }

    private void updateCraftingResult() {
        Level level = getLevel();
        if (level == null) return;
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftingGrid, level)
                .orElse(null);
        craftingResult.setItem(0,
                recipe == null ? ItemStack.EMPTY : recipe.assemble(craftingGrid, level.registryAccess()));
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem().copy();
        ItemStack original = stack.copy();
        int vaultEnd = getVisibleSlotCount();
        int invStart = playerSlotsStart;
        int invEnd = craftingSlotsStart;
        int craftStart = craftingSlotsStart;
        int craftEnd = craftingOutputStart;
        int craftOut = craftingOutputStart;

        if (index == craftOut) {
            ItemStack result = slot.getItem();
            if (result.isEmpty()) return ItemStack.EMPTY;

            int outputPerCraft = result.getCount();
            if (outputPerCraft <= 0) return ItemStack.EMPTY;
            int maxCrafts = result.getMaxStackSize() / outputPerCraft;

            ItemStack itemAtStart = result.copy();
            ItemStack collected = ItemStack.EMPTY;

            for (int craft = 0; craft < maxCrafts; craft++) {
                ItemStack current = slot.getItem();
                if (current.isEmpty()) break;
                if (!ItemStack.isSameItemSameTags(current, itemAtStart)) break;

                ItemStack crafted = current.copyWithCount(outputPerCraft);
                if (!collected.isEmpty()) {
                    if (!ItemStack.isSameItemSameTags(collected, crafted)) break;
                    if (collected.getCount() + crafted.getCount() > collected.getMaxStackSize()) break;
                }

                ItemStack simCollect = collected.isEmpty() ? crafted.copy() :
                        collected.copyWithCount(collected.getCount() + crafted.getCount());
                ItemStack remainder = simulateMoveToPlayer(simCollect, invStart, invEnd);
                if (!remainder.isEmpty() && remainder.getCount() == simCollect.getCount()) break;

                snapshotGridIngredients();
                slot.onTake(player, current);

                if (collected.isEmpty()) collected = crafted.copy();
                else collected.grow(crafted.getCount());
            }

            if (!collected.isEmpty()) {
                if (!moveItemStackTo(collected, invStart, invEnd, true) && !collected.isEmpty()) {
                    insertIntoFullVault(collected);
                }
            }
            return ItemStack.EMPTY;

        } else if (index < vaultEnd) {
            // STACKED_MODE: aggregated shift-click branch removed — restore here when re-enabling
            if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
        } else if (index >= craftStart && index < craftEnd) {
            stack = insertIntoFullVault(stack);
            if (!stack.isEmpty()) moveItemStackTo(stack, invStart, invEnd, true);
        } else {
            ItemStack remaining = insertIntoFullVault(stack.copy());
            int moved = stack.getCount() - remaining.getCount();
            stack.shrink(moved);
            if (!stack.isEmpty()) moveItemStackTo(stack, craftStart, craftEnd, false);
        }

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    private ItemStack simulateMoveToPlayer(ItemStack stack, int invStart, int invEnd) {
        ItemStack sim = stack.copy();
        for (int i = invEnd - 1; i >= invStart && !sim.isEmpty(); i--) {
            Slot s = slots.get(i);
            ItemStack inSlot = s.getItem();
            if (!inSlot.isEmpty() && ItemStack.isSameItemSameTags(inSlot, sim)) {
                int canFit = Math.min(s.getMaxStackSize(), inSlot.getMaxStackSize()) - inSlot.getCount();
                int move = Math.min(canFit, sim.getCount());
                sim.shrink(move);
            }
        }
        for (int i = invEnd - 1; i >= invStart && !sim.isEmpty(); i--) {
            Slot s = slots.get(i);
            if (s.getItem().isEmpty()) {
                int move = Math.min(s.getMaxStackSize(), sim.getCount());
                sim.shrink(move);
            }
        }
        return sim;
    }

    private ItemStack insertIntoFullVault(ItemStack stack) {
        if (!(vaultHandler instanceof ItemStackHandler handler)) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && onGridClose != null) {
            ItemStack[] grid = new ItemStack[craftingGrid.getContainerSize()];
            for (int i = 0; i < grid.length; i++) {
                grid[i] = craftingGrid.getItem(i).copy();
            }
            onGridClose.accept(grid);
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        if (slot.index == lockedMenuSlot) return false;
        return super.canTakeItemForPickAll(stack, slot);
    }

    private static final class LockedSlot extends Slot {

        LockedSlot(net.minecraft.world.entity.player.Inventory inv, int index, int x, int y) {
            super(inv, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
