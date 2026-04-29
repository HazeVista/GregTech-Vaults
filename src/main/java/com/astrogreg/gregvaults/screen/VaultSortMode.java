package com.astrogreg.gregvaults.screen;

public enum VaultSortMode {

    NAME,
    COUNT_DESC,
    COUNT_ASC;

    public VaultSortMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String label() {
        return switch (this) {
            case NAME -> "Name";
            case COUNT_DESC -> "Amount ↓";
            case COUNT_ASC -> "Amount ↑";
        };
    }
}
