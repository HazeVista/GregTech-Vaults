package com.astrogreg.gregvaults.multiblock;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.multiblock.VaultMachine.VaultTier;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;

import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;

@SuppressWarnings("all")
public class VaultMachineDefinition {

    public static MultiblockMachineDefinition BRONZE_VAULT;
    public static MultiblockMachineDefinition STEEL_VAULT;
    public static MultiblockMachineDefinition TITANIUM_VAULT;

    public static void init() {
        BRONZE_VAULT   = registerVault(VaultTier.BRONZE);
        STEEL_VAULT    = registerVault(VaultTier.STEEL);
        TITANIUM_VAULT = registerVault(VaultTier.TITANIUM);
    }

    private static MultiblockMachineDefinition registerVault(VaultTier tier) {
        int maxLayers = switch (tier) {
            case BRONZE   -> 27;
            case STEEL    -> 27;
            case TITANIUM -> 27;
        };

        return GregTechVaults.REGISTRATE
                .multiblock(switch (tier) {
                    case BRONZE   -> "bronze_vault";
                    case STEEL    -> "steel_vault";
                    case TITANIUM -> "titanium_vault";
                }, holder -> new VaultMachine(holder, tier))
                .rotationState(RotationState.NON_Y_AXIS)
                .pattern(definition -> FactoryBlockPattern.start(LEFT, UP, BACK)
                        .build())
                .register();
    }
}