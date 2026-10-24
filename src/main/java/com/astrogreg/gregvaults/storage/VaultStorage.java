package com.astrogreg.gregvaults.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntConsumer;

public final class VaultStorage extends ItemStackHandler {

    private final Map<StackKey, Long> itemCounts = new HashMap<>();
    private final IntConsumer changeListener;
    private ItemStack[] indexedStacks;

    public VaultStorage(int slots, IntConsumer changeListener) {
        super(slots);
        this.changeListener = Objects.requireNonNull(changeListener);
        this.indexedStacks = emptyStacks(slots);
    }

    private static ItemStack[] emptyStacks(int size) {
        ItemStack[] stacks = new ItemStack[size];
        Arrays.fill(stacks, ItemStack.EMPTY);
        return stacks;
    }

    public List<Entry> getEntries() {
        List<Entry> entries = new ArrayList<>(itemCounts.size());
        itemCounts.forEach((key, count) -> entries.add(new Entry(key, count)));
        return List.copyOf(entries);
    }

    public long getCount(StackKey key) {
        return itemCounts.getOrDefault(key, 0L);
    }

    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

        ItemStack remainder = stack;
        StackKey key = StackKey.of(stack);

        for (int slot = 0; slot < getSlots() && !remainder.isEmpty(); slot++) {
            ItemStack stored = getStackInSlot(slot);
            if (!stored.isEmpty() && key.matches(stored)) {
                remainder = insertItem(slot, remainder, simulate);
            }
        }

        for (int slot = 0; slot < getSlots() && !remainder.isEmpty(); slot++) {
            if (getStackInSlot(slot).isEmpty()) {
                remainder = insertItem(slot, remainder, simulate);
            }
        }
        return remainder;
    }

    public ItemStack extract(StackKey key, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;

        int remaining = Math.min(amount, key.createStack().getMaxStackSize());
        ItemStack result = ItemStack.EMPTY;
        for (int slot = 0; slot < getSlots() && remaining > 0; slot++) {
            ItemStack stored = getStackInSlot(slot);
            if (!key.matches(stored)) continue;

            ItemStack extracted = extractItem(slot, remaining, simulate);
            if (extracted.isEmpty()) continue;
            if (result.isEmpty()) {
                result = extracted;
            } else {
                result.grow(extracted.getCount());
            }
            remaining -= extracted.getCount();
        }
        return result;
    }

    public void rebuildIndex() {
        itemCounts.clear();
        indexedStacks = emptyStacks(getSlots());
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            indexedStacks[slot] = stack.copy();
            addToIndex(stack, stack.getCount());
        }
    }

    @Override
    public void setSize(int size) {
        super.setSize(size);
        itemCounts.clear();
        indexedStacks = emptyStacks(size);
    }

    @Override
    protected void onLoad() {
        rebuildIndex();
    }

    @Override
    protected void onContentsChanged(int slot) {
        ItemStack previous = indexedStacks[slot];
        addToIndex(previous, -previous.getCount());

        ItemStack current = getStackInSlot(slot);
        indexedStacks[slot] = current.copy();
        addToIndex(current, current.getCount());
        changeListener.accept(slot);
    }

    private void addToIndex(ItemStack stack, long difference) {
        if (stack.isEmpty() || difference == 0) return;

        StackKey key = StackKey.of(stack);
        itemCounts.compute(key, (ignored, current) -> {
            long updated = (current == null ? 0 : current) + difference;
            return updated > 0 ? updated : null;
        });
    }

    public record Entry(StackKey key, long count) {

        public ItemStack displayStack() {
            return key.createStack();
        }
    }

    public static final class StackKey {

        private final Item item;
        private final @Nullable CompoundTag tag;

        private StackKey(Item item, @Nullable CompoundTag tag) {
            this.item = item;
            this.tag = tag == null ? null : tag.copy();
        }

        public static StackKey of(ItemStack stack) {
            if (stack.isEmpty()) throw new IllegalArgumentException("Cannot key an empty stack");
            return new StackKey(stack.getItem(), stack.getTag());
        }

        public ItemStack createStack() {
            ItemStack stack = new ItemStack(item);
            stack.setTag(tag == null ? null : tag.copy());
            return stack;
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty() && stack.is(item) && Objects.equals(tag, stack.getTag());
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof StackKey other)) return false;
            return item == other.item && Objects.equals(tag, other.tag);
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(item) + Objects.hashCode(tag);
        }
    }
}
