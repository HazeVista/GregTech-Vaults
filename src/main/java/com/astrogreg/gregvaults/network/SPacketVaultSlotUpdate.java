package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;

import java.util.function.Supplier;

public class SPacketVaultSlotUpdate {

    private final int slot;
    private final ItemStack stack;

    public SPacketVaultSlotUpdate(int slot, ItemStack stack) {
        this.slot = slot;
        this.stack = stack;
    }

    public static void encode(SPacketVaultSlotUpdate packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.slot);
        buf.writeItem(packet.stack);
    }

    public static SPacketVaultSlotUpdate decode(FriendlyByteBuf buf) {
        return new SPacketVaultSlotUpdate(buf.readInt(), buf.readItem());
    }

    public static void handle(SPacketVaultSlotUpdate packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            if (mc.player.containerMenu instanceof VaultTerminalMenu menu) {
                menu.updateClientCacheSlot(packet.slot, packet.stack);
            } else if (mc.player.containerMenu instanceof VaultContainerMenu menu) {
                menu.updateClientCacheSlot(packet.slot, packet.stack);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
