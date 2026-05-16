package com.astrogreg.gregvaults.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import com.astrogreg.gregvaults.GregTechVaults;

@SuppressWarnings("all")
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
        CHANNEL.registerMessage(
                id++,
                CPacketVaultSearch.class,
                CPacketVaultSearch::encode,
                CPacketVaultSearch::decode,
                CPacketVaultSearch::handle);
        CHANNEL.registerMessage(
                id++,
                CPacketVaultSort.class,
                CPacketVaultSort::encode,
                CPacketVaultSort::decode,
                CPacketVaultSort::handle);
        CHANNEL.registerMessage(
                id++,
                CPacketVaultOrganize.class,
                CPacketVaultOrganize::encode,
                CPacketVaultOrganize::decode,
                CPacketVaultOrganize::handle);
        CHANNEL.registerMessage(
                id++,
                SPacketVaultContents.class,
                SPacketVaultContents::encode,
                SPacketVaultContents::decode,
                SPacketVaultContents::handle);
        CHANNEL.registerMessage(
                id++,
                SPacketVaultSlotUpdate.class,
                SPacketVaultSlotUpdate::encode,
                SPacketVaultSlotUpdate::decode,
                SPacketVaultSlotUpdate::handle);
        CHANNEL.registerMessage(
                id++,
                CPacketOpenTerminal.class,
                CPacketOpenTerminal::encode,
                CPacketOpenTerminal::decode,
                CPacketOpenTerminal::handle);
        // CHANNEL.registerMessage(
        // id++,
        // CPacketVaultDisplayMode.class,
        // CPacketVaultDisplayMode::encode,
        // CPacketVaultDisplayMode::decode,
        // CPacketVaultDisplayMode::handle);
    }
}
