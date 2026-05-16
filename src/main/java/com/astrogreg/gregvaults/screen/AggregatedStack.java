package com.astrogreg.gregvaults.screen;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AggregatedStack {

    public final ItemStack displayStack;
    public final List<Integer> backingSlots;

    public AggregatedStack(ItemStack displayStack, List<Integer> backingSlots) {
        this.displayStack = displayStack;
        this.backingSlots = backingSlots;
    }

    public int totalCount() {
        return displayStack.getCount();
    }
}
