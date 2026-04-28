package com.astrogreg.gregvaults.datagen.lang;

import com.astrogreg.gregvaults.items.WirelessTerminalItem;
import com.tterrag.registrate.providers.RegistrateLangProvider;

@SuppressWarnings("all")
public class VaultLangHandler {

    public static void init(RegistrateLangProvider provider) {
        provider.add(WirelessTerminalItem.KEY_VAULT_TERMINAL_TITLE, "Vault Terminal");
        provider.add(WirelessTerminalItem.KEY_VAULT_NOT_FORMED, "The vault is not formed.");
        provider.add(WirelessTerminalItem.KEY_VAULT_LINKED, "Vault linked!");
        provider.add(WirelessTerminalItem.KEY_NOT_LINKED, "Terminal is not linked to any vault.");
        provider.add(WirelessTerminalItem.KEY_DIMENSION_NOT_FOUND, "Linked dimension not found.");
        provider.add(WirelessTerminalItem.KEY_DIFFERENT_DIMENSION, "Vault is in a different dimension.");
        provider.add(WirelessTerminalItem.KEY_VAULT_NOT_FOUND, "Vault not found at linked position.");
        provider.add(WirelessTerminalItem.KEY_TOOLTIP_LINKED, "Linked");
        provider.add(WirelessTerminalItem.KEY_TOOLTIP_NOT_LINKED, "Not linked");
        provider.add(WirelessTerminalItem.KEY_TOOLTIP_HOW_TO_LINK, "Shift + right-click on vault to link");
    }
}
