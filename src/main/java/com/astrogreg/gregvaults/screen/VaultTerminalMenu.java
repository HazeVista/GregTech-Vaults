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

import com.astrogreg.gregvaults.registry.VaultMenuTypes;
import com.astrogreg.gregvaults.screen.VaultSlot.RemappingHandler;

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

    public final CraftingContainer craftingGrid = new TransientCraftingContainer(this, 3, 3);
    public final ResultContainer craftingResult = new ResultContainer();

    public final int playerSlotsStart;
    public final int craftingSlotsStart;
    public final int craftingOutputStart;

    public VaultTerminalMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler) {
        super(VaultMenuTypes.VAULT_TERMINAL_MENU.get(), windowId);
        this.vaultHandler = vaultHandler;
        this.totalSlots = vaultHandler.getSlots();

        int usedRows = Math.max(1, (int) Math.ceil(totalSlots / (double) COLS));
        this.visibleRows = Math.min(usedRows, MAX_ROWS);

        // Layout: top(17) + vaultRows + labelRow(18) + craftRows(3*18) + playerInv(14+54+4+18+6)
        this.craftSectionY = 17 + visibleRows * SLOT_SIZE;
        this.craftGridY = craftSectionY + SLOT_SIZE;
        this.craftGridX = SLOTS_X;
        this.craftOutX = craftGridX + 3 * SLOT_SIZE + 14;
        this.craftOutY = craftGridY + SLOT_SIZE;

        // playerY: craftGridY + 3*18 (slots) + 14 (player inv top border+label in texture)
        this.playerY = craftGridY + 3 * SLOT_SIZE + 14;
        this.hotbarY = playerY + 3 * SLOT_SIZE + 4;

        // Vault slots
        int windowSize = visibleRows * COLS;
        this.remapping = new RemappingHandler(vaultHandler, windowSize);
        for (int i = 0; i < windowSize; i++) {
            addSlot(new VaultSlot(remapping, i,
                    SLOTS_X + (i % COLS) * SLOT_SIZE,
                    SLOTS_Y + (i / COLS) * SLOT_SIZE));
        }

        // Player inventory
        this.playerSlotsStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9,
                        SLOTS_X + col * SLOT_SIZE,
                        playerY + row * SLOT_SIZE));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col,
                    SLOTS_X + col * SLOT_SIZE,
                    hotbarY));
        }

        // Crafting
        this.craftingSlotsStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftingGrid, col + row * 3,
                        craftGridX + col * SLOT_SIZE,
                        craftGridY + row * SLOT_SIZE));
            }
        }

        this.craftingOutputStart = slots.size();
        addSlot(new ResultSlot(playerInv.player, craftingGrid, craftingResult, 0,
                craftOutX, craftOutY));

        updateCraftingResult();
    }

    public VaultTerminalMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, new ItemStackHandler(buf.readInt()));
    }

    // Scroll / Search / Sort
    private ItemStack[] clientCache = null;

    public void setClientCache(ItemStack[] cache) {
        this.clientCache = cache;
        remapping.setClientCache(cache);
        refreshVisibleSlots();
    }

    public void updateClientCacheSlot(int slot, ItemStack stack) {
        if (clientCache != null && slot >= 0 && slot < clientCache.length) {
            clientCache[slot] = stack;
            remapping.setClientCache(clientCache);
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

    public void updateScroll(int scrollRow) {
        remapping.setOffset(scrollRow * COLS);
        refreshVisibleSlots();
    }

    public void setSortMode(VaultSortMode mode) {
        this.sortMode = mode;
        applySortToStorage();
    }

    private void applySortToStorage() {
        if (!(vaultHandler instanceof ItemStackHandler handler)) return;
        VaultMenuUtils.applySortToStorage(handler, sortMode, remapping);
    }

    public void organize() {
        if (!(vaultHandler instanceof ItemStackHandler handler)) return;
        VaultMenuUtils.organize(handler, remapping);
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
            filteredIndices = matching.stream().mapToInt(Integer::intValue).toArray();
            remapping.setFilteredIndices(filteredIndices);
        }
    }

    public int getVisibleSlotCount() {
        return visibleRows * COLS;
    }

    public int[] getFilteredIndices() {
        return filteredIndices;
    }

    public int getTotalFilteredRows() {
        int count = filteredIndices != null ? filteredIndices.length : totalSlots;
        return (int) Math.ceil(count / (double) COLS);
    }

    // Crafting
    @Override
    public void slotsChanged(Container container) {
        if (container == craftingGrid) updateCraftingResult();
    }

    private void updateCraftingResult() {
        Level level = null;
        for (Slot slot : slots) {
            if (slot.container instanceof Inventory inv) {
                level = inv.player.level();
                break;
            }
        }
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
            if (!slot.hasItem()) return ItemStack.EMPTY;
            ItemStack result = slot.getItem();
            int craftCount = howManyCrafts(result.getMaxStackSize());
            if (craftCount <= 0) return ItemStack.EMPTY;

            ItemStack collected = result.copyWithCount(craftCount);
            slot.set(result.getCount() > craftCount
                    ? result.copyWithCount(result.getCount() - craftCount)
                    : ItemStack.EMPTY);

            consumeIngredients(craftCount);

            updateCraftingResult();

            if (!moveItemStackTo(collected, invStart, invEnd, true) && !collected.isEmpty()) {
                insertIntoFullVault(collected);
            }
            return original;
        } else if (index < vaultEnd) {
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

    private int howManyCrafts(int maxCount) {
        int crafts = maxCount;
        for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
            ItemStack ingredient = craftingGrid.getItem(i);
            if (ingredient.isEmpty()) continue;
            if (ingredient.isDamageableItem()) continue;
            if (ingredient.getItem().hasCraftingRemainingItem()) continue;
            crafts = Math.min(crafts, ingredient.getCount());
        }
        return crafts;
    }

    private void consumeIngredients(int times) {
        for (int i = 0; i < craftingGrid.getContainerSize(); i++) {
            ItemStack ingredient = craftingGrid.getItem(i);
            if (ingredient.isEmpty()) continue;

            if (ingredient.isDamageableItem()) {
                int newDamage = ingredient.getDamageValue() + times;
                if (newDamage >= ingredient.getMaxDamage()) {
                    craftingGrid.setItem(i, ItemStack.EMPTY);
                } else {
                    ingredient.setDamageValue(newDamage);
                    craftingGrid.setItem(i, ingredient);
                }
            } else if (ingredient.getItem().hasCraftingRemainingItem()) {
                ItemStack container = new ItemStack(ingredient.getItem().getCraftingRemainingItem());
                craftingGrid.setItem(i, container);
            } else {
                ingredient.shrink(times);
                craftingGrid.setItem(i, ingredient.isEmpty() ? ItemStack.EMPTY : ingredient);
            }
        }
    }

    private ItemStack insertIntoFullVault(ItemStack stack) {
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

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide) clearContainer(player, craftingGrid);
    }
}