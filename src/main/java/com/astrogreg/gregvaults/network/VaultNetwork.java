package com.astrogreg.gregvaults.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import com.astrogreg.gregvaults.GregTechVaults;

public class VaultNetwork {

    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GregTechVaults.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(
                id++,
                CPacketVaultScroll.class,
                CPacketVaultScroll::encode,
                CPacketVaultScroll::decode,
                CPacketVaultScroll::handle);
    }
}
