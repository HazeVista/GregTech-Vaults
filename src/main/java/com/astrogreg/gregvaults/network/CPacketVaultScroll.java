package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.screen.VaultContainerMenu;

import java.util.function.Supplier;

public class CPacketVaultScroll {

    private final int scrollRow;

    public CPacketVaultScroll(int scrollRow) {
        this.scrollRow = scrollRow;
    }

    public static void encode(CPacketVaultScroll packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.scrollRow);
    }

    public static CPacketVaultScroll decode(FriendlyByteBuf buf) {
        return new CPacketVaultScroll(buf.readInt());
    }

    public static void handle(
                              CPacketVaultScroll packet,
                              Supplier<NetworkEvent.Context> ctx) {
        ctx
                .get()
                .enqueueWork(() -> {
                    ServerPlayer player = ctx.get().getSender();
                    if (player != null &&
                            player.containerMenu instanceof VaultContainerMenu menu) {
                        menu.updateScroll(packet.scrollRow);
                    }
                });
        ctx.get().setPacketHandled(true);
    }
}
