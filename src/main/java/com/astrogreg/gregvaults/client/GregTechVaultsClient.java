package com.astrogreg.gregvaults.client;

import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

import com.astrogreg.gregvaults.GregTechVaults;

@OnlyIn(Dist.CLIENT)
public final class GregTechVaultsClient {

    private GregTechVaultsClient() {}

    public static void init(IEventBus modEventBus) {
        DynamicRenderManager.register(GregTechVaults.id("vault_overlay"), VaultOverlayRender.TYPE);
        modEventBus.addListener(VaultOverlayRender::registerModel);
        modEventBus.addListener(VaultKeyBindings::register);
        MinecraftForge.EVENT_BUS.register(VaultKeyBindings.TickHandler.class);
    }
}
