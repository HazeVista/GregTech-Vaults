package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.screen.AbstractVaultMenu;

import java.util.function.Supplier;

public record CPacketVaultCatalogClick(ItemStack stack, int button, boolean shiftDown) {

    public static void encode(CPacketVaultCatalogClick packet, FriendlyByteBuf buf) {
        buf.writeItem(packet.stack);
        buf.writeByte(packet.button);
        buf.writeBoolean(packet.shiftDown);
    }

    public static CPacketVaultCatalogClick decode(FriendlyByteBuf buf) {
        ItemStack stack = buf.readItem();
        int button = buf.readUnsignedByte();
        if (button > 1) throw new IllegalArgumentException("Invalid catalog mouse button: " + button);
        return new CPacketVaultCatalogClick(stack, button, buf.readBoolean());
    }

    public static void handle(CPacketVaultCatalogClick packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null || player.isSpectator()) return;
            if (!(player.containerMenu instanceof AbstractVaultMenu menu) || !menu.stillValid(player)) return;
            menu.handleCatalogClick(player, packet.stack, packet.button, packet.shiftDown);
        });
        context.get().setPacketHandled(true);
    }
}
