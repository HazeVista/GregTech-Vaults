package com.astrogreg.gregvaults.datagen.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

@SuppressWarnings("all")
public class VaultLangHandler {

    public static void init(RegistrateLangProvider provider) {
        provider.add("block.gregtechvaults.vault_core_mk1", "Vault Core MK1");
        provider.add("block.gregtechvaults.vault_core_mk2", "Vault Core MK2");
        provider.add("block.gregtechvaults.vault_core_mk3", "Vault Core MK3");
    }
}
