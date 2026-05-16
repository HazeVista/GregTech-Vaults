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
    private final boolean reversed;

    public CPacketVaultSort(VaultSortMode mode, boolean reversed) {
        this.mode = mode;
        this.reversed = reversed;
    }

    public static void encode(CPacketVaultSort packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.mode);
        buf.writeBoolean(packet.reversed);
    }

    public static CPacketVaultSort decode(FriendlyByteBuf buf) {
        return new CPacketVaultSort(buf.readEnum(VaultSortMode.class), buf.readBoolean());
    }

    public static void handle(CPacketVaultSort packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.containerMenu instanceof VaultTerminalMenu menu) {
                menu.setSortMode(packet.mode);
                menu.setSortReversed(packet.reversed);
            } else if (player.containerMenu instanceof VaultContainerMenu menu) {
                menu.setSortMode(packet.mode);
                menu.setSortReversed(packet.reversed);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
