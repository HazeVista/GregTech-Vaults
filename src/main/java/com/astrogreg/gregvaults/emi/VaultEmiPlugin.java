package com.astrogreg.gregvaults.emi;

import com.astrogreg.gregvaults.registry.VaultMenuTypes;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class VaultEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addRecipeHandler(VaultMenuTypes.VAULT_MENU.get(), new VaultRecipeHandler<>());
        registry.addRecipeHandler(VaultMenuTypes.VAULT_TERMINAL_MENU.get(), new VaultRecipeHandler<>());
    }
}
