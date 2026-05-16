package com.astrogreg.gregvaults.screen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VaultScreenState {

    public static class State {

        public VaultDisplayMode displayMode = VaultDisplayMode.SLOTS;
        public VaultSortMode sortMode = VaultSortMode.NAME;
        public boolean sortReversed = false;
        public String searchQuery = "";
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static State get(UUID playerUuid) {
        return STATES.computeIfAbsent(playerUuid, k -> new State());
    }

    public static void save(UUID playerUuid, VaultDisplayMode displayMode,
                            VaultSortMode sortMode, boolean sortReversed, String searchQuery) {
        State state = STATES.computeIfAbsent(playerUuid, k -> new State());
        state.displayMode = displayMode;
        state.sortMode = sortMode;
        state.sortReversed = sortReversed;
        state.searchQuery = searchQuery;
    }
}
