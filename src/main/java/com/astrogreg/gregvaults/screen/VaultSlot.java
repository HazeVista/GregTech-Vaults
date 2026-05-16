package com.astrogreg.gregvaults.screen;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import org.jetbrains.annotations.NotNull;

// STACKED_MODE: import java.util.List;

@SuppressWarnings("all")
public class VaultSlot extends SlotItemHandler {

    private final RemappingHandler remapping;

    public VaultSlot(RemappingHandler remapping, int visibleIndex, int x, int y) {
        super(remapping, visibleIndex, x, y);
        this.remapping = remapping;
    }

    @Override
    public boolean isActive() {
        return remapping.isIndexActive(this.getSlotIndex());
    }

    // STACKED_MODE: public boolean isAggregated() {
    // STACKED_MODE: return remapping.isAggregated();
    // STACKED_MODE: }

    public static class RemappingHandler implements IItemHandlerModifiable {

        private final IItemHandler real;
        private int offset = 0;
        private final int windowSize;

        private int[] filteredIndices = null;
        private int[] sortedIndices = null;
        private ItemStack[] clientCache = null;
        // STACKED_MODE: private List<AggregatedStack> aggregatedView = null;

        public RemappingHandler(IItemHandler real, int windowSize) {
            this.real = real;
            this.windowSize = windowSize;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public void setFilteredIndices(int[] indices) {
            this.filteredIndices = indices;
        }

        public void setSortedIndices(int[] indices) {
            this.sortedIndices = indices;
        }

        public void setClientCache(ItemStack[] cache) {
            this.clientCache = cache;
        }

        // STACKED_MODE: public void setAggregatedView(List<AggregatedStack> view) {
        // STACKED_MODE: this.aggregatedView = view;
        // STACKED_MODE: }

        // STACKED_MODE: public boolean isAggregated() {
        // STACKED_MODE: return aggregatedView != null;
        // STACKED_MODE: }

        // STACKED_MODE: public int getAggregatedIndex(int visibleSlot) {
        // STACKED_MODE: if (aggregatedView == null) return -1;
        // STACKED_MODE: int absolute = offset + visibleSlot;
        // STACKED_MODE: if (absolute < 0 || absolute >= aggregatedView.size()) return -1;
        // STACKED_MODE: return absolute;
        // STACKED_MODE: }

        private int realIndex(int visibleSlot) {
            int absolute = offset + visibleSlot;
            if (filteredIndices != null) {
                if (absolute < 0 || absolute >= filteredIndices.length) return -1;
                return filteredIndices[absolute];
            } else if (sortedIndices != null) {
                if (absolute < 0 || absolute >= sortedIndices.length) return -1;
                return sortedIndices[absolute];
            } else {
                if (absolute < 0 || absolute >= real.getSlots()) return -1;
                return absolute;
            }
        }

        public boolean isIndexActive(int visibleSlot) {
            // STACKED_MODE: if (aggregatedView != null) {
            // STACKED_MODE: int absolute = offset + visibleSlot;
            // STACKED_MODE: return absolute >= 0 && absolute < aggregatedView.size();
            // STACKED_MODE: }
            return realIndex(visibleSlot) >= 0;
        }

        @Override
        public int getSlots() {
            return windowSize;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            // STACKED_MODE: if (aggregatedView != null) {
            // STACKED_MODE: int absolute = offset + slot;
            // STACKED_MODE: if (absolute < 0 || absolute >= aggregatedView.size()) return ItemStack.EMPTY;
            // STACKED_MODE: AggregatedStack agg = aggregatedView.get(absolute);
            // STACKED_MODE: // Return count of 1 so vanilla skips rendering its own count label.
            // STACKED_MODE: // Our screen renders the true total as a custom label instead.
            // STACKED_MODE: return agg.displayStack.copyWithCount(1);
            // STACKED_MODE: }
            int ri = realIndex(slot);
            if (ri < 0) return ItemStack.EMPTY;
            if (clientCache != null && ri < clientCache.length) {
                return clientCache[ri] != null ? clientCache[ri] : ItemStack.EMPTY;
            }
            return real.getStackInSlot(ri);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // STACKED_MODE: if (aggregatedView != null) {
            // STACKED_MODE: // Route insertion to first matching backing slot with space,
            // STACKED_MODE: // then first empty slot, then any available slot in the handler.
            // STACKED_MODE: int absolute = offset + slot;
            // STACKED_MODE: if (absolute >= 0 && absolute < aggregatedView.size()) {
            // STACKED_MODE: AggregatedStack agg = aggregatedView.get(absolute);
            // STACKED_MODE: // Try matching backing slots first
            // STACKED_MODE: for (int backingSlot : agg.backingSlots) {
            // STACKED_MODE: ItemStack existing = real.getStackInSlot(backingSlot);
            // STACKED_MODE: if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, stack)) {
            // STACKED_MODE: ItemStack remainder = real.insertItem(backingSlot, stack, simulate);
            // STACKED_MODE: if (remainder.getCount() < stack.getCount()) return remainder;
            // STACKED_MODE: }
            // STACKED_MODE: }
            // STACKED_MODE: // Try empty backing slots
            // STACKED_MODE: for (int backingSlot : agg.backingSlots) {
            // STACKED_MODE: if (real.getStackInSlot(backingSlot).isEmpty()) {
            // STACKED_MODE: return real.insertItem(backingSlot, stack, simulate);
            // STACKED_MODE: }
            // STACKED_MODE: }
            // STACKED_MODE: }
            // STACKED_MODE: // Fall through to any slot in real handler
            // STACKED_MODE: ItemStack remaining = stack.copy();
            // STACKED_MODE: for (int i = 0; i < real.getSlots() && !remaining.isEmpty(); i++) {
            // STACKED_MODE: ItemStack existing = real.getStackInSlot(i);
            // STACKED_MODE: if (!existing.isEmpty() && ItemStack.isSameItemSameTags(existing, remaining)) {
            // STACKED_MODE: remaining = real.insertItem(i, remaining, simulate);
            // STACKED_MODE: }
            // STACKED_MODE: }
            // STACKED_MODE: for (int i = 0; i < real.getSlots() && !remaining.isEmpty(); i++) {
            // STACKED_MODE: if (real.getStackInSlot(i).isEmpty()) {
            // STACKED_MODE: return real.insertItem(i, remaining, simulate);
            // STACKED_MODE: }
            // STACKED_MODE: }
            // STACKED_MODE: return remaining;
            // STACKED_MODE: }
            int ri = realIndex(slot);
            return ri < 0 ? stack : real.insertItem(ri, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // STACKED_MODE: if (aggregatedView != null) {
            // STACKED_MODE: int absolute = offset + slot;
            // STACKED_MODE: if (absolute < 0 || absolute >= aggregatedView.size()) return ItemStack.EMPTY;
            // STACKED_MODE: AggregatedStack agg = aggregatedView.get(absolute);
            // STACKED_MODE: for (int backingSlot : agg.backingSlots) {
            // STACKED_MODE: ItemStack inSlot = real.getStackInSlot(backingSlot);
            // STACKED_MODE: if (!inSlot.isEmpty()) {
            // STACKED_MODE: ItemStack result = real.extractItem(backingSlot, amount, simulate);
            // STACKED_MODE: if (!result.isEmpty()) return result;
            // STACKED_MODE: }
            // STACKED_MODE: }
            // STACKED_MODE: return ItemStack.EMPTY;
            // STACKED_MODE: }
            int ri = realIndex(slot);
            return ri < 0 ? ItemStack.EMPTY : real.extractItem(ri, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            // STACKED_MODE: if (aggregatedView != null) return 64;
            int ri = realIndex(slot);
            return ri < 0 ? 0 : real.getSlotLimit(ri);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // STACKED_MODE: if (aggregatedView != null) return !stack.isEmpty();
            int ri = realIndex(slot);
            return ri >= 0 && real.isItemValid(ri, stack);
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            // STACKED_MODE: if (aggregatedView != null) return;
            int ri = realIndex(slot);
            if (ri >= 0 && real instanceof IItemHandlerModifiable m) {
                m.setStackInSlot(ri, stack);
            }
        }
    }
}
