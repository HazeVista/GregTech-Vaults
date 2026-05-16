package com.astrogreg.gregvaults.screen;

public enum VaultDisplayMode {

    SLOTS,
    STACKED;

    public VaultDisplayMode next() {
        return this == SLOTS ? STACKED : SLOTS;
    }
}
