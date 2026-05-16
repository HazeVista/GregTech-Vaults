package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;

import java.util.function.Supplier;

public class SPacketVaultContents {

    private final ItemStack[] stacks;

    public SPacketVaultContents(ItemStack[] stacks) {
        this.stacks = stacks;
    }

    public static void encode(SPacketVaultContents packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.stacks.length);
        for (ItemStack stack : packet.stacks) {
            buf.writeItem(stack);
        }
    }

    public static SPacketVaultContents decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        ItemStack[] stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            stacks[i] = buf.readItem();
        }
        return new SPacketVaultContents(stacks);
    }

    public static void handle(SPacketVaultContents packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;
            if (mc.player.containerMenu instanceof VaultTerminalMenu menu) {
                menu.setClientCache(packet.stacks);
            } else if (mc.player.containerMenu instanceof VaultContainerMenu menu) {
                menu.setClientCache(packet.stacks);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
