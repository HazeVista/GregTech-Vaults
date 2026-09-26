package com.astrogreg.gregvaults;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

@SuppressWarnings("unused")
@GTAddon
public class VaultsGTAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return GregTechVaults.REGISTRATE;
    }

    @Override
    public void initializeAddon() {}

    @Override
    public String addonModId() {
        return GregTechVaults.MOD_ID;
    }
}
