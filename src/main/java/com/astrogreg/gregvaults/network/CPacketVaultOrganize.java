package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;

import java.util.function.Supplier;

public class CPacketVaultOrganize {

    public CPacketVaultOrganize() {}

    public static void encode(CPacketVaultOrganize packet, FriendlyByteBuf buf) {}

    public static CPacketVaultOrganize decode(FriendlyByteBuf buf) {
        return new CPacketVaultOrganize();
    }

    public static void handle(CPacketVaultOrganize packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.containerMenu instanceof VaultTerminalMenu menu) {
                menu.organize();
                player.containerMenu.broadcastFullState();
            } else if (player.containerMenu instanceof VaultContainerMenu menu) {
                menu.organize();
                player.containerMenu.broadcastFullState();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
