package com.astrogreg.gregvaults.screen;

import com.gregtechceu.gtceu.api.item.IGTTool;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.PacketDistributor;

import com.astrogreg.gregvaults.multiblock.VaultMachine;
import com.astrogreg.gregvaults.network.SPacketVaultCatalog;
import com.astrogreg.gregvaults.network.VaultNetwork;
import com.astrogreg.gregvaults.storage.VaultStorage;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractVaultMenu extends AbstractContainerMenu {

    public static final int COLS = 9;
    public static final int SLOT_SIZE = 18;
    public static final int MAX_ROWS = 6;
    public static final int SLOTS_X = 8;
    public static final int SLOTS_Y = 18;

    public final IItemHandler vaultHandler;
    public final int totalSlots;
    public final int visibleRows;

    @Nullable
    public final VaultMachine machine;

    public final int craftSectionY;
    public final int craftGridY;
    public final int craftGridX;
    public final int craftOutX;
    public final int craftOutY;
    public final int playerY;
    public final int hotbarY;

    public final CraftingContainer craftingGrid = new TransientCraftingContainer(this, 3, 3);
    public final ResultContainer craftingResult = new ResultContainer();

    public final int playerSlotsStart;
    public final int craftingSlotsStart;
    public final int craftingOutputStart;

    private final Inventory playerInventory;
    private final ItemStack[] lastGridIngredients = new ItemStack[9];

    private List<VaultCatalogEntry> catalogPage = List.of();
    private int catalogTotal;
    private int scrollRow;
    private String searchQuery = "";
    private VaultSortMode sortMode = VaultSortMode.NAME;
    private boolean sortReversed;

    private boolean suppressCraftingResultUpdates;
    private boolean deferCraftingBroadcasts;
    private int lastGridHash;

    protected AbstractVaultMenu(MenuType<?> menuType, int windowId, Inventory playerInventory,
                                IItemHandler vaultHandler, @Nullable VaultMachine machine, int totalSlots,
                                int lockedPlayerSlot) {
        super(menuType, windowId);
        this.playerInventory = playerInventory;
        this.vaultHandler = vaultHandler;
        this.machine = machine;
        this.totalSlots = Math.max(0, totalSlots);

        int usedRows = Math.max(1, (int) Math.ceil(this.totalSlots / (double) COLS));
        this.visibleRows = Math.min(usedRows, MAX_ROWS);

        this.craftSectionY = SLOTS_Y + visibleRows * SLOT_SIZE - 1;
        this.craftGridY = craftSectionY + SLOT_SIZE;
        this.craftGridX = SLOTS_X;
        this.craftOutX = craftGridX + 3 * SLOT_SIZE + 27;
        this.craftOutY = craftGridY + SLOT_SIZE;
        this.playerY = craftGridY + 3 * SLOT_SIZE + 14;
        this.hotbarY = playerY + 3 * SLOT_SIZE + 4;

        this.playerSlotsStart = slots.size();
        addPlayerSlots(playerInventory, lockedPlayerSlot);

        this.craftingSlotsStart = slots.size();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Slot(craftingGrid, col + row * 3,
                        craftGridX + col * SLOT_SIZE,
                        craftGridY + row * SLOT_SIZE));
            }
        }

        this.craftingOutputStart = slots.size();
        addSlot(new VaultCraftingResultSlot(playerInventory.player, craftingGrid, craftingResult, 0,
                craftOutX, craftOutY));
        updateCraftingResult();
    }

    private static Slot createPlayerSlot(Inventory inventory, int slot, int x, int y, int lockedPlayerSlot) {
        return slot == lockedPlayerSlot ? new LockedSlot(inventory, slot, x, y) : new Slot(inventory, slot, x, y);
    }

    private static boolean isCraftingToolLike(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof IGTTool) return true;
        if (!ToolHelper.getCraftingToolTypes(stack).isEmpty()) return true;
        return stack.isDamageableItem() && stack.getMaxStackSize() == 1;
    }

    private static void damageCraftingTool(ItemStack stack, Player player) {
        if (stack.getItem() instanceof IGTTool || !ToolHelper.getCraftingToolTypes(stack).isEmpty()) {
            ToolHelper.damageItemWhenCrafting(stack, player);
        } else if (stack.isDamageableItem()) {
            stack.hurtAndBreak(1, player, ignored -> {});
        }
    }

    private void addPlayerSlots(Inventory playerInventory, int lockedPlayerSlot) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int inventorySlot = col + row * 9 + 9;
                addSlot(createPlayerSlot(playerInventory, inventorySlot,
                        SLOTS_X + col * SLOT_SIZE, playerY + row * SLOT_SIZE, lockedPlayerSlot));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(createPlayerSlot(playerInventory, col,
                    SLOTS_X + col * SLOT_SIZE, hotbarY, lockedPlayerSlot));
        }
    }

    public List<VaultCatalogEntry> getCatalogPage() {
        return catalogPage;
    }

    public int getCatalogTotal() {
        return catalogTotal;
    }

    public int getScrollRow() {
        return scrollRow;
    }

    public int getPageSize() {
        return visibleRows * COLS;
    }

    public int getTotalCatalogRows() {
        return (int) Math.ceil(catalogTotal / (double) COLS);
    }

    public void applyCatalogPage(int scrollRow, int totalEntries, List<VaultCatalogEntry> entries) {
        this.scrollRow = scrollRow;
        this.catalogTotal = totalEntries;
        this.catalogPage = List.copyOf(entries);
    }

    public void setSearch(String query, ServerPlayer player) {
        this.searchQuery = query == null ? "" : query.strip();
        this.scrollRow = 0;
        sendCatalogPage(player);
    }

    public void setSort(VaultSortMode mode, boolean reversed, ServerPlayer player) {
        this.sortMode = Objects.requireNonNull(mode);
        this.sortReversed = reversed;
        this.scrollRow = 0;
        sendCatalogPage(player);
    }

    public void setScrollRow(int row, ServerPlayer player) {
        this.scrollRow = Math.max(0, row);
        sendCatalogPage(player);
    }

    public void sendCatalogPage(ServerPlayer player) {
        if (!(vaultHandler instanceof VaultStorage storage)) return;

        List<VaultCatalogEntry> entries = new ArrayList<>();
        String normalizedQuery = searchQuery.toLowerCase(Locale.ROOT);
        for (VaultStorage.Entry entry : storage.getEntries()) {
            ItemStack stack = entry.displayStack();
            if (!normalizedQuery.isEmpty() &&
                    !stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                continue;
            }
            entries.add(new VaultCatalogEntry(stack, entry.count()));
        }

        Comparator<VaultCatalogEntry> byName = Comparator
                .comparing((VaultCatalogEntry entry) -> entry.stack().getHoverName().getString(),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(entry -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString())
                .thenComparing(entry -> Objects.toString(entry.stack().getTag(), ""));
        Comparator<VaultCatalogEntry> comparator = sortMode == VaultSortMode.COUNT ?
                Comparator.comparingLong(VaultCatalogEntry::count).thenComparing(byName) : byName;
        if (sortReversed) comparator = comparator.reversed();
        entries.sort(comparator);

        int totalRows = (int) Math.ceil(entries.size() / (double) COLS);
        int maxScroll = Math.max(0, totalRows - visibleRows);
        scrollRow = Math.min(scrollRow, maxScroll);
        int from = Math.min(scrollRow * COLS, entries.size());
        int to = Math.min(from + getPageSize(), entries.size());
        List<VaultCatalogEntry> page = entries.subList(from, to);

        VaultNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SPacketVaultCatalog(containerId, scrollRow, entries.size(), page));
    }

    public void handleCatalogClick(ServerPlayer player, ItemStack requested, int button, boolean shiftDown) {
        if (!(vaultHandler instanceof VaultStorage storage)) return;
        if (button < 0 || button > 1) return;

        VaultStorage.StackKey key = requested.isEmpty() ? null : VaultStorage.StackKey.of(requested);
        if (key != null && storage.getCount(key) <= 0) return;

        if (machine != null) machine.beginBatch();
        try {
            ItemStack carried = getCarried();
            if (shiftDown && key != null) {
                moveCatalogStackToInventory(player, storage, key);
            } else if (!carried.isEmpty() && (key == null || !key.matches(carried))) {
                insertCarried(storage, carried, button == 1 ? 1 : carried.getCount());
            } else if (key != null) {
                extractToCursor(storage, key, button == 1);
            } else if (!carried.isEmpty()) {
                insertCarried(storage, carried, button == 1 ? 1 : carried.getCount());
            }
        } finally {
            if (machine != null) machine.endBatch();
        }
        broadcastChanges();
    }

    private void moveCatalogStackToInventory(ServerPlayer player, VaultStorage storage, VaultStorage.StackKey key) {
        ItemStack template = key.createStack();
        int capacity = computePlayerCapacity(template);
        int amount = Math.min(capacity, template.getMaxStackSize());
        ItemStack extracted = storage.extract(key, amount, false);
        if (extracted.isEmpty()) return;

        player.getInventory().add(extracted);
        if (!extracted.isEmpty()) storage.insert(extracted, false);
    }

    private void extractToCursor(VaultStorage storage, VaultStorage.StackKey key, boolean single) {
        ItemStack carried = getCarried();
        ItemStack template = key.createStack();
        int capacity = carried.isEmpty() ? template.getMaxStackSize() : carried.getMaxStackSize() - carried.getCount();
        if (capacity <= 0) return;

        int amount;
        if (single && carried.isEmpty()) {
            amount = (int) Math.min((storage.getCount(key) + 1) / 2, capacity);
        } else {
            amount = single ? 1 : capacity;
        }

        ItemStack extracted = storage.extract(key, amount, false);
        if (extracted.isEmpty()) return;
        if (carried.isEmpty()) {
            setCarried(extracted);
        } else {
            carried.grow(extracted.getCount());
            setCarried(carried);
        }
    }

    private void insertCarried(VaultStorage storage, ItemStack carried, int amount) {
        ItemStack offered = carried.copyWithCount(Math.min(amount, carried.getCount()));
        ItemStack remainder = storage.insert(offered, false);
        int inserted = offered.getCount() - remainder.getCount();
        if (inserted <= 0) return;
        carried.shrink(inserted);
        setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
    }

    private int computePlayerCapacity(ItemStack template) {
        int capacity = 0;
        for (int index = playerSlotsStart; index < craftingSlotsStart; index++) {
            Slot slot = slots.get(index);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                capacity += Math.min(slot.getMaxStackSize(), template.getMaxStackSize());
            } else if (ItemStack.isSameItemSameTags(stack, template)) {
                capacity += Math.min(slot.getMaxStackSize(), stack.getMaxStackSize()) - stack.getCount();
            }
        }
        return capacity;
    }

    public void refreshCraftingResult() {
        updateCraftingResult();
        broadcastChanges();
    }

    public void beginCraftingGridBulkUpdate() {
        suppressCraftingResultUpdates = true;
    }

    public void endCraftingGridBulkUpdate() {
        suppressCraftingResultUpdates = false;
        lastGridHash = gridHash();
        updateCraftingResult();
        if (!deferCraftingBroadcasts) broadcastChanges();
    }

    @Override
    public void slotsChanged(Container container) {
        if (container != craftingGrid || suppressCraftingResultUpdates) return;
        int hash = gridHash();
        if (hash != lastGridHash) {
            lastGridHash = hash;
            updateCraftingResult();
        }
    }

    private int gridHash() {
        int hash = 1;
        for (int slot = 0; slot < craftingGrid.getContainerSize(); slot++) {
            ItemStack stack = craftingGrid.getItem(slot);
            hash = 31 * hash + (stack.isEmpty() ? 0 : BuiltInRegistries.ITEM.getId(stack.getItem()));
            hash = 31 * hash + stack.getCount();
            hash = 31 * hash + stack.getDamageValue();
            hash = 31 * hash + Objects.hashCode(stack.getTag());
        }
        return hash;
    }

    private void updateCraftingResult() {
        Level level = playerInventory.player.level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftingGrid, level)
                .orElse(null);
        craftingResult.setRecipeUsed(recipe);
        craftingResult.setItem(0,
                recipe == null ? ItemStack.EMPTY : recipe.assemble(craftingGrid, level.registryAccess()));
    }

    private void snapshotGridIngredients() {
        for (int slot = 0; slot < craftingGrid.getContainerSize(); slot++) {
            ItemStack stack = craftingGrid.getItem(slot);
            lastGridIngredients[slot] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
    }

    private void refillGridFromVault() {
        for (int slot = 0; slot < craftingGrid.getContainerSize(); slot++) {
            ItemStack needed = lastGridIngredients[slot];
            if (needed == null || needed.isEmpty() || !craftingGrid.getItem(slot).isEmpty()) continue;

            if (vaultHandler instanceof VaultStorage storage) {
                ItemStack extracted = storage.extract(VaultStorage.StackKey.of(needed), 1, false);
                if (!extracted.isEmpty()) craftingGrid.setItem(slot, extracted);
                continue;
            }

            for (int vaultSlot = 0; vaultSlot < vaultHandler.getSlots(); vaultSlot++) {
                ItemStack stored = vaultHandler.getStackInSlot(vaultSlot);
                if (!ItemStack.isSameItemSameTags(stored, needed)) continue;
                ItemStack extracted = vaultHandler.extractItem(vaultSlot, 1, false);
                if (!extracted.isEmpty()) {
                    craftingGrid.setItem(slot, extracted);
                    break;
                }
            }
        }
        updateCraftingResult();
    }

    private void applyCraftingToolDamageToGrid(Player player) {
        for (int slot = 0; slot < craftingGrid.getContainerSize(); slot++) {
            ItemStack before = lastGridIngredients[slot];
            if (!isCraftingToolLike(before)) continue;

            ItemStack after = craftingGrid.getItem(slot);
            if (!after.isEmpty() && !ItemStack.isSameItemSameTags(before, after)) continue;

            ItemStack damaged = after.isEmpty() ? before.copyWithCount(1) : after.copyWithCount(1);
            damageCraftingTool(damaged, player);
            craftingGrid.setItem(slot, damaged.isEmpty() ? ItemStack.EMPTY : damaged);
        }
    }

    private ItemStack insertIntoVault(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        if (vaultHandler instanceof VaultStorage storage) return storage.insert(stack, false);
        if (!(vaultHandler instanceof ItemStackHandler handler)) return stack;

        ItemStack remainder = stack;
        for (int slot = 0; slot < handler.getSlots() && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (player.level().isClientSide || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index == craftingOutputStart) {
            if (machine != null) machine.beginBatch();
            deferCraftingBroadcasts = true;
            try {
                int outputPerCraft = original.getCount();
                int maxCrafts = original.getMaxStackSize() / outputPerCraft;
                for (int craft = 0; craft < maxCrafts; craft++) {
                    source = slot.getItem();
                    if (source.isEmpty() || !ItemStack.isSameItemSameTags(source, original)) break;
                    if (computePlayerCapacity(original) < outputPerCraft) break;

                    ItemStack crafted = source.copy();
                    if (!moveItemStackTo(source, playerSlotsStart, craftingSlotsStart, true)) break;
                    slot.onQuickCraft(source, crafted);
                    if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
                    else slot.setChanged();
                    if (source.getCount() == crafted.getCount()) break;
                    slot.onTake(player, source);
                    if (!source.isEmpty()) player.drop(source, false);
                }
            } finally {
                deferCraftingBroadcasts = false;
                broadcastChanges();
                if (machine != null) machine.endBatch();
            }
            return ItemStack.EMPTY;
        }

        if (machine != null) machine.beginBatch();
        try {
            ItemStack remaining;
            if (index >= craftingSlotsStart && index < craftingOutputStart) {
                remaining = insertIntoVault(source.copy());
                if (!remaining.isEmpty()) moveItemStackTo(remaining, playerSlotsStart, craftingSlotsStart, true);
            } else if (index >= playerSlotsStart && index < craftingSlotsStart) {
                remaining = insertIntoVault(source.copy());
                if (!remaining.isEmpty()) moveItemStackTo(remaining, craftingSlotsStart, craftingOutputStart, false);
            } else {
                return ItemStack.EMPTY;
            }

            int moved = original.getCount() - remaining.getCount();
            if (moved <= 0) return ItemStack.EMPTY;
            int left = source.getCount() - moved;
            slot.set(left > 0 ? source.copyWithCount(left) : ItemStack.EMPTY);
            slot.setChanged();
            slot.onTake(player, original.copyWithCount(moved));
            return original;
        } finally {
            if (machine != null) machine.endBatch();
        }
    }

    @Override
    public void removed(Player player) {
        if (!player.level().isClientSide) {
            returnCraftingGrid(player);
            if (machine != null && player instanceof ServerPlayer serverPlayer) {
                machine.removeViewer(serverPlayer);
            }
        }
        super.removed(player);
    }

    private void returnCraftingGrid(Player player) {
        if (machine != null) machine.beginBatch();
        try {
            for (int slot = 0; slot < craftingGrid.getContainerSize(); slot++) {
                ItemStack stack = craftingGrid.removeItemNoUpdate(slot);
                if (stack.isEmpty()) continue;
                ItemStack remainder = insertIntoVault(stack);
                if (!remainder.isEmpty()) player.getInventory().placeItemBackInInventory(remainder);
            }
        } finally {
            if (machine != null) machine.endBatch();
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != craftingResult && super.canTakeItemForPickAll(stack, slot);
    }

    private static final class LockedSlot extends Slot {

        private LockedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
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

    private final class VaultCraftingResultSlot extends ResultSlot {

        private VaultCraftingResultSlot(Player player, CraftingContainer grid, ResultContainer result,
                                        int slot, int x, int y) {
            super(player, grid, result, slot, x, y);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            snapshotGridIngredients();
            super.onTake(player, stack);
            applyCraftingToolDamageToGrid(player);

            if (machine != null) machine.beginBatch();
            beginCraftingGridBulkUpdate();
            try {
                refillGridFromVault();
            } finally {
                endCraftingGridBulkUpdate();
                if (machine != null) machine.endBatch();
            }
        }
    }
}
