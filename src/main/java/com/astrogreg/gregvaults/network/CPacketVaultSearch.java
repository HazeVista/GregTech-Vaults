package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.screen.VaultContainerMenu;

import java.util.function.Supplier;

public class CPacketVaultSearch {

    private final String query;

    public CPacketVaultSearch(String query) {
        this.query = query;
    }

    public static void encode(CPacketVaultSearch packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.query, 64);
    }

    public static CPacketVaultSearch decode(FriendlyByteBuf buf) {
        return new CPacketVaultSearch(buf.readUtf(64));
    }

    public static void handle(CPacketVaultSearch packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof VaultContainerMenu menu) {
                menu.updateSearch(packet.query);
                menu.updateScroll(0);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
