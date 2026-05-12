package com.astrogreg.gregvaults.screen;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class VaultSlot extends SlotItemHandler {

    private final RemappingHandler remapping;

    public VaultSlot(
                     RemappingHandler remapping,
                     int visibleIndex,
                     int x,
                     int y) {
        super(remapping, visibleIndex, x, y);
        this.remapping = remapping;
    }

    @Override
    public boolean isActive() {
        return remapping.isIndexActive(this.getSlotIndex());
    }

    public static class RemappingHandler implements IItemHandlerModifiable {

        private final IItemHandler real;
        private int offset = 0;
        private final int windowSize;

        private int[] filteredIndices = null;
        private int[] sortedIndices = null;  // sorted view of all slots

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
            return realIndex(visibleSlot) >= 0;
        }

        @Override
        public int getSlots() {
            return windowSize;
        }

        private ItemStack[] clientCache = null;

        public void setClientCache(ItemStack[] cache) {
            this.clientCache = cache;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            int ri = realIndex(slot);
            if (ri < 0) return ItemStack.EMPTY;
            if (clientCache != null && ri < clientCache.length) {
                return clientCache[ri] != null ? clientCache[ri] : ItemStack.EMPTY;
            }
            return real.getStackInSlot(ri);
        }

        @Override
        public @NotNull ItemStack insertItem(
                                    int slot,
                                    ItemStack stack,
                                    boolean simulate) {
            int ri = realIndex(slot);
            return ri < 0 ? stack : real.insertItem(ri, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            int ri = realIndex(slot);
            return ri < 0 ? ItemStack.EMPTY : real.extractItem(ri, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            int ri = realIndex(slot);
            return ri < 0 ? 0 : real.getSlotLimit(ri);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            int ri = realIndex(slot);
            return ri >= 0 && real.isItemValid(ri, stack);
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            int ri = realIndex(slot);
            if (ri >= 0 && real instanceof IItemHandlerModifiable m) {
                m.setStackInSlot(ri, stack);
            }
        }
    }
}
