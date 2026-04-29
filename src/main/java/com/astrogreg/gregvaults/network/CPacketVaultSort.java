package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.screen.VaultSortMode;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;

import java.util.function.Supplier;

public class CPacketVaultSort {

    private final VaultSortMode mode;

    public CPacketVaultSort(VaultSortMode mode) {
        this.mode = mode;
    }

    public static void encode(CPacketVaultSort packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.mode);
    }

    public static CPacketVaultSort decode(FriendlyByteBuf buf) {
        return new CPacketVaultSort(buf.readEnum(VaultSortMode.class));
    }

    public static void handle(CPacketVaultSort packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.containerMenu instanceof VaultTerminalMenu menu) {
                menu.setSortMode(packet.mode);
                player.containerMenu.broadcastFullState();
            } else if (player.containerMenu instanceof VaultContainerMenu menu) {
                menu.setSortMode(packet.mode);
                player.containerMenu.broadcastFullState();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
