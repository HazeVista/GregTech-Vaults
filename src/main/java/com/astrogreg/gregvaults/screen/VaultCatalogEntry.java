package com.astrogreg.gregvaults.screen;

import net.minecraft.world.item.ItemStack;

public record VaultCatalogEntry(ItemStack stack, long count) {

    public VaultCatalogEntry {
        stack = stack.copyWithCount(1);
        if (count < 0) throw new IllegalArgumentException("Count cannot be negative");
    }
}
