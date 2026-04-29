package com.astrogreg.gregvaults.datagen;

import com.gregtechceu.gtceu.api.registry.registrate.provider.GTBlockstateProvider;

import com.astrogreg.gregvaults.datagen.lang.VaultLangHandler;
import com.astrogreg.gregvaults.datagen.model.VaultModelProvider;
import com.astrogreg.gregvaults.recipe.GTVaultsRecipes;
import com.tterrag.registrate.providers.ProviderType;

import static com.astrogreg.gregvaults.GregTechVaults.REGISTRATE;

public class VaultDatagen {

    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, VaultLangHandler::init);
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, GTVaultsRecipes::init);
        REGISTRATE.addDataGenerator(ProviderType.BLOCKSTATE,
                provider -> VaultModelProvider.init((GTBlockstateProvider) provider));
    }
}
