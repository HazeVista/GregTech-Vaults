package com.astrogreg.gregvaults.screen;

import com.astrogreg.gregvaults.registry.VaultMenuTypes;
import com.astrogreg.gregvaults.screen.VaultSlot.RemappingHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

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

    public VaultContainerMenu(
        int windowId,
        Inventory playerInv,
        IItemHandler vaultHandler
    ) {
        super(VaultMenuTypes.VAULT_MENU.get(), windowId);
        this.vaultHandler = vaultHandler;
        this.totalSlots = vaultHandler.getSlots();

        int usedRows = Math.max(1, (int) Math.ceil(totalSlots / (double) COLS));
        this.visibleRows = Math.min(usedRows, MAX_ROWS);
        int visibleSlots = visibleRows * COLS;

        this.playerY = SLOTS_Y + visibleRows * SLOT_SIZE + 14;
        this.hotbarY = this.playerY + 3 * SLOT_SIZE + 4;

        this.remapping = new RemappingHandler(vaultHandler, visibleSlots);

        for (int i = 0; i < visibleSlots; i++) {
            addSlot(
                new VaultSlot(
                    remapping,
                    i,
                    SLOTS_X + (i % COLS) * SLOT_SIZE,
                    SLOTS_Y + (i / COLS) * SLOT_SIZE
                )
            );
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(
                    new Slot(
                        playerInv,
                        col + row * 9 + 9,
                        SLOTS_X + col * SLOT_SIZE,
                        this.playerY + row * SLOT_SIZE
                    )
                );
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(
                new Slot(
                    playerInv,
                    col,
                    SLOTS_X + col * SLOT_SIZE,
                    this.hotbarY
                )
            );
        }
    }

    public VaultContainerMenu(
        int windowId,
        Inventory playerInv,
        FriendlyByteBuf buf
    ) {
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
                if (
                    !stack.isEmpty() &&
                    stack.getHoverName().getString().toLowerCase().contains(q)
                ) {
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
        int count =
            filteredIndices != null ? filteredIndices.length : totalSlots;
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
        ItemStack carried
    ) {
        if (items.size() > this.slots.size()) {
            net.minecraft.client.Minecraft mc =
                net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) mc.player.closeContainer();
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
            if (
                !moveItemStackTo(stack, vaultCount, slots.size(), true)
            ) return ItemStack.EMPTY;
        } else {
            if (
                !moveItemStackTo(stack, 0, vaultCount, false)
            ) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }
}
