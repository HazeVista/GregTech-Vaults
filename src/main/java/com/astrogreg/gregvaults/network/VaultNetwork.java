package com.astrogreg.gregvaults.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import com.astrogreg.gregvaults.GregTechVaults;

import java.util.Optional;

@SuppressWarnings("all")
public class VaultNetwork {

    private static final String PROTOCOL = "3";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GregTechVaults.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int id = 0;

    public static void init() {
        CHANNEL.registerMessage(id++, CPacketVaultAction.class,
                CPacketVaultAction::encode, CPacketVaultAction::decode, CPacketVaultAction::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CPacketOpenTerminal.class,
                CPacketOpenTerminal::encode, CPacketOpenTerminal::decode, CPacketOpenTerminal::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CPacketFillCraftingGrid.class,
                CPacketFillCraftingGrid::encode, CPacketFillCraftingGrid::decode, CPacketFillCraftingGrid::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, CPacketVaultCatalogClick.class,
                CPacketVaultCatalogClick::encode, CPacketVaultCatalogClick::decode,
                CPacketVaultCatalogClick::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CHANNEL.registerMessage(id++, SPacketVaultCatalog.class,
                SPacketVaultCatalog::encode, SPacketVaultCatalog::decode, SPacketVaultCatalog::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
