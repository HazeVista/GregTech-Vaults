package com.astrogreg.gregvaults.registry;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.items.VaultLinkables;
import com.astrogreg.gregvaults.items.WirelessTerminalItem;
import com.tterrag.registrate.util.entry.ItemEntry;

@SuppressWarnings("all")
public class VaultItems {

    public static final ItemEntry<WirelessTerminalItem> WIRELESS_VAULT_TERMINAL = GregTechVaults.REGISTRATE
            .item("wireless_vault_terminal", WirelessTerminalItem::new)
            .properties(p -> p.stacksTo(1))
            .register();

    public static void init() {}

    public static void registerLinkables() {
        VaultLinkables.register(WIRELESS_VAULT_TERMINAL.get(), WirelessTerminalItem.LINKABLE_HANDLER);
    }
}
