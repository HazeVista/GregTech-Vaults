package com.astrogreg.gregvaults.emi;

import com.astrogreg.gregvaults.registry.VaultRegistry;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class VaultEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(VaultRegistry.VAULT_MENU.get(), new VaultRecipeHandler<>());
        registry.addRecipeHandler(VaultRegistry.VAULT_TERMINAL_MENU.get(), new VaultRecipeHandler<>());
    }
}
