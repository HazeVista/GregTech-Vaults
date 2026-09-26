package com.astrogreg.gregvaults.screen;

import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import com.astrogreg.gregvaults.items.WirelessTerminalItem;
import com.astrogreg.gregvaults.multiblock.VaultMachine;
import com.astrogreg.gregvaults.registry.VaultRegistry;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("all")
public class VaultTerminalMenu extends AbstractVaultMenu {

    private final @Nullable GlobalPos linkedVault;

    public VaultTerminalMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler) {
        this(windowId, playerInv, vaultHandler, null, null, vaultHandler.getSlots(), -1);
    }

    public VaultTerminalMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler, VaultMachine machine) {
        this(windowId, playerInv, vaultHandler, machine, null, vaultHandler.getSlots(), -1);
    }

    public VaultTerminalMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler, VaultMachine machine,
                             @Nullable GlobalPos linkedVault, int terminalSlot) {
        this(windowId, playerInv, vaultHandler, machine, linkedVault, vaultHandler.getSlots(), terminalSlot);
    }

    private VaultTerminalMenu(int windowId, Inventory playerInv, IItemHandler vaultHandler, VaultMachine machine,
                              @Nullable GlobalPos linkedVault, int totalSlots, int terminalSlot) {
        super(VaultRegistry.VAULT_TERMINAL_MENU.get(), windowId, playerInv, vaultHandler, machine, totalSlots,
                terminalSlot);
        this.linkedVault = linkedVault;
    }

    public VaultTerminalMenu(int windowId, Inventory playerInv, FriendlyByteBuf buf) {
        this(windowId, playerInv, new ItemStackHandler(0), null, null,
                Math.max(0, buf.readInt()), buf.readInt());
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine == null) return true;
        if (!machine.isFormed() || !machine.getVaultTier().wirelessAllowed() || linkedVault == null ||
                !MachineOwner.canOpenOwnerMachine(player, machine)) {
            return false;
        }

        ItemStack terminal = findLinkedTerminal(player.getInventory());
        if (terminal.isEmpty()) return false;

        var config = com.astrogreg.gregvaults.config.VaultConfig.INSTANCE.wirelessTerminal;
        if (config.infiniteRange) return true;
        if (!player.level().dimension().equals(linkedVault.dimension())) return false;
        return player.blockPosition().distSqr(linkedVault.pos()) <=
                Math.pow(WirelessTerminalItem.getRange(terminal), 2);
    }

    private ItemStack findLinkedTerminal(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof WirelessTerminalItem &&
                    linkedVault.equals(WirelessTerminalItem.getLinkedPosition(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
}
