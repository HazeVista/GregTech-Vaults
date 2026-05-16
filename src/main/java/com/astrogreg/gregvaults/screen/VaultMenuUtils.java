package com.astrogreg.gregvaults.screen;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import com.astrogreg.gregvaults.screen.VaultSlot.RemappingHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VaultMenuUtils {

    private VaultMenuUtils() {}

    public static void applySortToStorage(ItemStackHandler handler, VaultSortMode sortMode,
                                          boolean sortReversed, RemappingHandler remapping) {
        int slots = handler.getSlots();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) stacks.add(s.copy());
        }
        Comparator<ItemStack> cmp = switch (sortMode) {
            case NAME -> Comparator.comparing(s -> s.getHoverName().getString());
            case COUNT -> Comparator.comparingInt((ItemStack s) -> s.getCount()).reversed();
        };
        if (sortReversed) cmp = cmp.reversed();
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
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty()) stacks.add(s.copy());
        }
        stacks.sort(Comparator.comparing(s -> s.getHoverName().getString()));
        for (int i = 0; i < slots; i++) {
            handler.setStackInSlot(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
        remapping.setSortedIndices(null);
    }

    /**
     * Format a count for display in aggregated mode.
     * Shows one decimal place when abbreviated, e.g. 1164 -> "1.2k", 1500000 -> "1.5M"
     */
    // STACKED_MODE: public static String formatCount(int total) {
    // STACKED_MODE: if (total >= 1_000_000) {
    // STACKED_MODE: int whole = total / 1_000_000;
    // STACKED_MODE: int decimal = (total % 1_000_000) / 100_000;
    // STACKED_MODE: return decimal == 0 ? whole + "M" : whole + "." + decimal + "M";
    // STACKED_MODE: } else if (total >= 1_000) {
    // STACKED_MODE: int whole = total / 1_000;
    // STACKED_MODE: int decimal = (total % 1_000) / 100;
    // STACKED_MODE: return decimal == 0 ? whole + "k" : whole + "." + decimal + "k";
    // STACKED_MODE: } else {
    // STACKED_MODE: return String.valueOf(total);
    // STACKED_MODE: }
    // STACKED_MODE: }

    // STACKED_MODE: public static List<AggregatedStack> buildAggregatedView(
    // STACKED_MODE: ItemStack[] cache,
    // STACKED_MODE: String searchQuery,
    // STACKED_MODE: VaultSortMode sort,
    // STACKED_MODE: boolean reversed) {
    // STACKED_MODE:
    // STACKED_MODE: if (cache == null) return Collections.emptyList();
    // STACKED_MODE:
    // STACKED_MODE: LinkedHashMap<String, AggregatedStack> map = new LinkedHashMap<>();
    // STACKED_MODE:
    // STACKED_MODE: for (int i = 0; i < cache.length; i++) {
    // STACKED_MODE: ItemStack stack = cache[i];
    // STACKED_MODE: if (stack == null || stack.isEmpty()) continue;
    // STACKED_MODE:
    // STACKED_MODE: if (searchQuery != null && !searchQuery.isEmpty()) {
    // STACKED_MODE: if (!stack.getHoverName().getString().toLowerCase()
    // STACKED_MODE: .contains(searchQuery.toLowerCase())) continue;
    // STACKED_MODE: }
    // STACKED_MODE:
    // STACKED_MODE: String key = itemKey(stack);
    // STACKED_MODE: if (map.containsKey(key)) {
    // STACKED_MODE: AggregatedStack existing = map.get(key);
    // STACKED_MODE: existing.displayStack.grow(stack.getCount());
    // STACKED_MODE: existing.backingSlots.add(i);
    // STACKED_MODE: } else {
    // STACKED_MODE: ItemStack display = stack.copyWithCount(stack.getCount());
    // STACKED_MODE: List<Integer> slots = new ArrayList<>();
    // STACKED_MODE: slots.add(i);
    // STACKED_MODE: map.put(key, new AggregatedStack(display, slots));
    // STACKED_MODE: }
    // STACKED_MODE: }
    // STACKED_MODE:
    // STACKED_MODE: List<AggregatedStack> list = new ArrayList<>(map.values());
    // STACKED_MODE:
    // STACKED_MODE: Comparator<AggregatedStack> cmp = switch (sort) {
    // STACKED_MODE: case NAME -> Comparator.comparing(s -> s.displayStack.getHoverName().getString());
    // STACKED_MODE: case COUNT -> Comparator.comparingInt((AggregatedStack s) -> s.totalCount()).reversed();
    // STACKED_MODE: };
    // STACKED_MODE:
    // STACKED_MODE: if (reversed) cmp = cmp.reversed();
    // STACKED_MODE: list.sort(cmp);
    // STACKED_MODE:
    // STACKED_MODE: return list;
    // STACKED_MODE: }

    // STACKED_MODE: private static String itemKey(ItemStack stack) {
    // STACKED_MODE: return net.minecraft.resources.ResourceLocation.tryParse(
    // STACKED_MODE: net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()).toString())
    // STACKED_MODE: + "@" + (stack.hasTag() ? stack.getTag().toString() : "");
    // STACKED_MODE: }
}
