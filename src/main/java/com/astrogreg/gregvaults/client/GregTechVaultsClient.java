package com.astrogreg.gregvaults.client;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.registry.VaultMenuTypes;
import com.astrogreg.gregvaults.screen.VaultScreen;
import com.astrogreg.gregvaults.screen.VaultTerminalScreen;

@OnlyIn(Dist.CLIENT)
public final class GregTechVaultsClient {

    private GregTechVaultsClient() {}

    public static void init(IEventBus modEventBus) {
        DynamicRenderManager.register(
                GregTechVaults.id("vault_overlay"),
                VaultOverlayRender.TYPE);

        modEventBus.addListener(VaultOverlayRender::registerModel);
        modEventBus.addListener(GregTechVaultsClient::clientSetup);
        modEventBus.addListener(VaultKeyBindings::register);

        MinecraftForge.EVENT_BUS.register(VaultKeyBindings.TickHandler.class);
    }

    private static void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(VaultMenuTypes.VAULT_MENU.get(), VaultScreen::new);
            MenuScreens.register(VaultMenuTypes.VAULT_TERMINAL_MENU.get(), VaultTerminalScreen::new);
        });
    }
}
