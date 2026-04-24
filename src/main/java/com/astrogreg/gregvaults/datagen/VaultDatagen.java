package com.astrogreg.gregvaults.datagen;

import com.astrogreg.gregvaults.datagen.lang.VaultLangHandler;
import com.tterrag.registrate.providers.ProviderType;

import static com.astrogreg.gregvaults.GregTechVaults.REGISTRATE;

public class VaultDatagen {

    public static void init() {
        REGISTRATE.addDataGenerator(ProviderType.LANG, VaultLangHandler::init);
    }
}
