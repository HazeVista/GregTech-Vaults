package com.astrogreg.gregvaults.registry;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;

import net.minecraft.network.chat.Component;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.multiblock.VaultInterfacePart;

public class VaultMachines {

    public static PartAbility VAULT_INTERFACE_ABILITY;

    public static MachineDefinition VAULT_INTERFACE;

    public static void init() {
        VAULT_INTERFACE_ABILITY = new PartAbility("vault_interface");

        com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator
                .registerConfigHandler(machine -> machine instanceof VaultInterfacePart part ?
                        () -> new com.astrogreg.gregvaults.multiblock.AutoInputItemConfigHandler(part) : null);

        VAULT_INTERFACE = GregTechVaults.REGISTRATE
                .machine("vault_interface", VaultInterfacePart::new)
                .rotationState(RotationState.ALL)
                .abilities(VAULT_INTERFACE_ABILITY)
                .overlaySteamHullModel("vault_interface")
                .tooltips(Component.translatable("tooltip.gregtechvaults.vault_interface"))
                .register();
    }
}
