package com.astrogreg.gregvaults.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.astrogreg.gregvaults.network.SPacketVaultCatalog;
import com.astrogreg.gregvaults.screen.AbstractVaultMenu;
import com.astrogreg.gregvaults.screen.AbstractVaultScreen;

@OnlyIn(Dist.CLIENT)
public final class ClientVaultPacketHandlers {

    private ClientVaultPacketHandlers() {}

    public static void handleCatalog(SPacketVaultCatalog packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (!(minecraft.player.containerMenu instanceof AbstractVaultMenu menu)) return;
        if (menu.containerId != packet.containerId()) return;
        menu.applyCatalogPage(packet.scrollRow(), packet.totalEntries(), packet.entries());
        if (minecraft.screen instanceof AbstractVaultScreen<?> screen && screen.getMenu() == menu) {
            screen.syncScrollFromServer(packet.scrollRow());
        }
    }
}
