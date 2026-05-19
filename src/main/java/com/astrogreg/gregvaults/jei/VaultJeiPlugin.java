package com.astrogreg.gregvaults.jei;

import net.minecraft.resources.ResourceLocation;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.registry.VaultRegistry;
import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeTransferRegistration;

@JeiPlugin
public class VaultJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(GregTechVaults.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addUniversalRecipeTransferHandler(
                new VaultJeiTransferHandler<>(VaultContainerMenu.class, VaultRegistry.VAULT_MENU.get()));
        registration.addUniversalRecipeTransferHandler(
                new VaultJeiTransferHandler<>(VaultTerminalMenu.class, VaultRegistry.VAULT_TERMINAL_MENU.get()));
    }
}
