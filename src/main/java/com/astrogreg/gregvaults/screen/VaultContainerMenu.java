package com.astrogreg.gregvaults.screen;

import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import com.astrogreg.gregvaults.multiblock.VaultMachine;
import com.astrogreg.gregvaults.registry.VaultRegistry;

@SuppressWarnings("all")
public class VaultContainerMenu extends AbstractVaultMenu {

    public VaultContainerMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler) {
        this(VaultRegistry.VAULT_MENU.get(), windowId, playerInv, vaultHandler, null, vaultHandler.getSlots());
    }

    public VaultContainerMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler, VaultMachine machine) {
        this(VaultRegistry.VAULT_MENU.get(), windowId, playerInv, vaultHandler, machine, vaultHandler.getSlots());
    }

    protected VaultContainerMenu(MenuType<?> menuType, int windowId, Inventory playerInv,
                                 IItemHandler vaultHandler, VaultMachine machine, int totalSlots) {
        super(menuType, windowId, playerInv, vaultHandler, machine, totalSlots, -1);
    }

    public VaultContainerMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(VaultRegistry.VAULT_MENU.get(), windowId, playerInv, new ItemStackHandler(0), null,
                Math.max(0, buf.readInt()));
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine == null) return true;
        return machine.isFormed() && MachineOwner.canOpenOwnerMachine(player, machine) &&
                player.distanceToSqr(machine.getPos().getCenter()) <= 64.0;
    }
}
