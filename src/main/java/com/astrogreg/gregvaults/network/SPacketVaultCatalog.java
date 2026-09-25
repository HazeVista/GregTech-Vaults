package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.client.ClientVaultPacketHandlers;
import com.astrogreg.gregvaults.screen.VaultCatalogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SPacketVaultCatalog(int containerId, int scrollRow, int totalEntries,
                                  List<VaultCatalogEntry> entries) {

    private static final int MAX_ENTRIES = 54;
    private static final int MAX_TOTAL_ENTRIES = 1_000_000;

    public SPacketVaultCatalog {
        entries = List.copyOf(entries);
    }

    public static void encode(SPacketVaultCatalog packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        buf.writeVarInt(packet.scrollRow);
        buf.writeVarInt(packet.totalEntries);
        buf.writeVarInt(packet.entries.size());
        for (VaultCatalogEntry entry : packet.entries) {
            buf.writeItem(entry.stack());
            buf.writeVarLong(entry.count());
        }
    }

    public static SPacketVaultCatalog decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int scrollRow = buf.readVarInt();
        int totalEntries = buf.readVarInt();
        int size = buf.readVarInt();
        if (scrollRow < 0 || totalEntries < 0 || totalEntries > MAX_TOTAL_ENTRIES || size < 0 ||
                size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid vault catalog metadata");
        }

        List<VaultCatalogEntry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buf.readItem();
            long count = buf.readVarLong();
            if (stack.isEmpty() || count <= 0) throw new IllegalArgumentException("Invalid vault catalog entry");
            entries.add(new VaultCatalogEntry(stack, count));
        }
        return new SPacketVaultCatalog(containerId, scrollRow, totalEntries, entries);
    }

    public static void handle(SPacketVaultCatalog packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientVaultPacketHandlers.handleCatalog(packet)));
        context.get().setPacketHandled(true);
    }
}
