package com.astrogreg.gregvaults.screen;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@OnlyIn(Dist.CLIENT)
public class VaultScreenState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "gregtechvaults-client.json";
    private static State cached = null;

    public static State get() {
        if (cached == null) load();
        return cached;
    }

    public static void save(VaultSortMode sortMode, boolean sortReversed, String searchQuery) {
        State state = get();
        state.sortMode = sortMode;
        state.sortReversed = sortReversed;
        state.searchQuery = searchQuery != null ? searchQuery : "";
        persist();
    }

    private static void load() {
        cached = new State();
        Path file = configPath();
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            State loaded = GSON.fromJson(json, State.class);
            if (loaded != null) {
                if (loaded.sortMode == null) loaded.sortMode = VaultSortMode.NAME;
                if (loaded.searchQuery == null) loaded.searchQuery = "";
                cached = loaded;
            }
        } catch (IOException | RuntimeException ignored) {}
    }

    private static void persist() {
        try {
            Path file = configPath();
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(cached));
        } catch (IOException ignored) {}
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve(FILE_NAME);
    }

    public static class State {

        public VaultSortMode sortMode = VaultSortMode.NAME;
        public boolean sortReversed = false;
        public String searchQuery = "";
    }
}
