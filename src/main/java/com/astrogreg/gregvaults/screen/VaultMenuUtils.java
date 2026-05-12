package com.astrogreg.gregvaults.screen;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import com.astrogreg.gregvaults.screen.VaultSlot.RemappingHandler;

public final class VaultMenuUtils {

    private VaultMenuUtils() {}

    public static void applySortToStorage(ItemStackHandler handler, VaultSortMode sortMode, RemappingHandler remapping) {
        int slots = handler.getSlots();
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) stacks.add(s.copy());
        }
        java.util.Comparator<ItemStack> cmp = switch (sortMode) {
            case NAME -> java.util.Comparator.comparing(s -> s.getHoverName().getString());
            case COUNT_DESC -> java.util.Comparator.comparingInt(ItemStack::getCount).reversed();
            case COUNT_ASC -> java.util.Comparator.comparingInt(ItemStack::getCount);
        };
        stacks.sort(cmp);
        for (int i = 0; i < slots; i++) {
            handler.setStackInSlot(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
        remapping.setSortedIndices(null);
    }

    public static void organize(ItemStackHandler handler, RemappingHandler remapping) {
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stackI = handler.getStackInSlot(i);
            if (stackI.isEmpty()) continue;
            int limit = handler.getSlotLimit(i);
            if (stackI.getCount() >= limit) continue;
            for (int j = i + 1; j < slots; j++) {
                ItemStack stackJ = handler.getStackInSlot(j);
                if (stackJ.isEmpty() || !ItemStack.isSameItemSameTags(stackI, stackJ)) continue;
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
}