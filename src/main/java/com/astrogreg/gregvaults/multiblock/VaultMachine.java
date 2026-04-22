package com.astrogreg.gregvaults.multiblock;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

public class VaultMachine extends MultiblockControllerMachine {

    public enum VaultTier {
        BRONZE,
        STEEL,
        TITANIUM
    }

    private final VaultTier vaultTier;
    private int totalSlots = 0;

    public VaultMachine(IMachineBlockEntity holder, VaultTier vaultTier) {
        super(holder);
        this.vaultTier = vaultTier;
    }

    public VaultTier getVaultTier() {
        return vaultTier;
    }


    public int getTotalSlots() {
        return totalSlots;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        totalSlots = 0;
    }
}