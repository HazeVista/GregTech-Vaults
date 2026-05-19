package com.astrogreg.gregvaults.multiblock;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.ICapabilityTrait;
import com.gregtechceu.gtceu.api.machine.trait.MachineTrait;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VaultInterfacePart extends MultiblockPartMachine {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            VaultInterfacePart.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    public enum ItemIoMode {

        DISABLED,
        INPUT,
        OUTPUT;

        public ItemIoMode next() {
            return switch (this) {
                case DISABLED -> INPUT;
                case INPUT -> OUTPUT;
                case OUTPUT -> DISABLED;
            };
        }

        public IO toCapabilityIO() {
            return switch (this) {
                case INPUT -> IO.IN;
                case OUTPUT -> IO.OUT;
                case DISABLED -> IO.NONE;
            };
        }

        public String displayName() {
            return switch (this) {
                case DISABLED -> "Disabled";
                case INPUT -> "Input";
                case OUTPUT -> "Output";
            };
        }
    }

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    private boolean autoTransferItems = false;

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    @Nullable
    private Direction itemFacing = null;

    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    private ItemIoMode itemIoMode = ItemIoMode.DISABLED;

    @Nullable
    private TickableSubscription autoTransferSubs;

    private final VaultItemHandlerTrait handlerTrait;

    public VaultInterfacePart(IMachineBlockEntity holder) {
        super(holder);
        this.handlerTrait = new VaultItemHandlerTrait(this);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean replacePartModelWhenFormed() {
        return isFormed();
    }

    @Override
    public void addedToController(IMultiController controller) {
        super.addedToController(controller);
        handlerTrait.updateHandler();
        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    @Override
    public void removedFromController(IMultiController controller) {
        super.removedFromController(controller);

        if (getControllers().isEmpty()) {
            handlerTrait.clearHandler();

            if (autoTransferSubs != null) {
                autoTransferSubs.unsubscribe();
                autoTransferSubs = null;
            }
        }

        notifyBlockUpdate();
    }

    private void updateAutoTransferSubscription() {
        if (autoTransferItems && itemFacing != null && itemIoMode != ItemIoMode.DISABLED && isFormed()) {
            autoTransferSubs = subscribeServerTick(autoTransferSubs, this::autoTransfer);
        } else if (autoTransferSubs != null) {
            autoTransferSubs.unsubscribe();
            autoTransferSubs = null;
        }
    }

    private void autoTransfer() {
        if (!isFormed() || !autoTransferItems || itemFacing == null || itemIoMode == ItemIoMode.DISABLED) return;
        if (getOffsetTimer() % 5 != 0) return;

        Level level = getLevel();
        VaultMachine vault = getVault();
        if (level == null || vault == null) return;

        GTTransferUtils.getAdjacentItemHandler(level, getPos(), itemFacing).ifPresent(adjacent -> {
            vault.beginBatch();
            try {
                if (canInputItems()) {
                    GTTransferUtils.transferItemsFiltered(adjacent, vault.getItemHandler(), stack -> true);
                } else if (canOutputItems()) {
                    GTTransferUtils.transferItemsFiltered(vault.getItemHandler(), adjacent, stack -> true);
                }
            } finally {
                vault.endBatch();
            }
        });
    }

    public boolean canInputItems() {
        return itemIoMode == ItemIoMode.INPUT;
    }

    public boolean canOutputItems() {
        return itemIoMode == ItemIoMode.OUTPUT;
    }

    public void setAutoTransferItems(boolean enabled) {
        this.autoTransferItems = enabled;
        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    public void setItemFacing(@Nullable Direction facing) {
        this.itemFacing = facing;
        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    public void setItemIoMode(@NotNull ItemIoMode mode) {
        this.itemIoMode = mode;

        if (mode == ItemIoMode.DISABLED) {
            this.itemFacing = null;
            this.autoTransferItems = false;
        }

        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    public void configureItemSide(@NotNull Direction side, @NotNull ItemIoMode mode) {
        this.itemFacing = mode == ItemIoMode.DISABLED ? null : side;
        this.itemIoMode = mode;

        if (mode == ItemIoMode.DISABLED) {
            this.autoTransferItems = false;
        }

        updateAutoTransferSubscription();
        notifyBlockUpdate();
    }

    public void refreshHandlerFromVault() {
        handlerTrait.updateHandler();
    }

    public void cycleItemMode(@NotNull Direction side) {
        if (itemFacing != side) {
            configureItemSide(side, ItemIoMode.INPUT);
        } else {
            configureItemSide(side, itemIoMode.next());
        }
    }

    @Override
    protected InteractionResult onScrewdriverClick(Player player, InteractionHand hand, Direction gridSide,
                                                   BlockHitResult hitResult) {
        if (isRemote()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            if (itemFacing == gridSide && itemIoMode != ItemIoMode.DISABLED) {
                setAutoTransferItems(!autoTransferItems);
                player.displayClientMessage(Component.literal(
                        "Vault Interface Auto Transfer: " + (autoTransferItems ? "Enabled" : "Disabled")), true);
            } else {
                player.displayClientMessage(Component.literal("Select an item side before enabling auto transfer"),
                        true);
            }
        } else {
            cycleItemMode(gridSide);
            player.displayClientMessage(Component.literal(
                    itemIoMode == ItemIoMode.DISABLED ? "Vault Interface: disabled" :
                            "Vault Interface: " + itemIoMode.displayName() + " on " + gridSide.getName()),
                    true);
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    private VaultMachine getVault() {
        return getControllers().stream()
                .filter(controller -> controller instanceof VaultMachine)
                .map(controller -> (VaultMachine) controller)
                .filter(VaultMachine::isFormed)
                .findFirst()
                .orElse(null);
    }

    public class VaultItemHandlerTrait extends MachineTrait implements ICapabilityTrait, IItemHandlerModifiable {

        protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
                VaultItemHandlerTrait.class);

        @Nullable
        private ItemStackHandler delegate = null;

        public VaultItemHandlerTrait(VaultInterfacePart machine) {
            super(machine);
            this.capabilityValidator = side -> delegate != null && isFormed() && itemFacing != null &&
                    itemIoMode != ItemIoMode.DISABLED && (side == null || side == itemFacing);
        }

        void updateHandler() {
            VaultMachine vault = getVault();
            delegate = vault != null ? vault.getItemHandler() : null;
        }

        void clearHandler() {
            delegate = null;
        }

        @Override
        public IO getCapabilityIO() {
            return itemIoMode.toCapabilityIO();
        }

        @Override
        public ManagedFieldHolder getFieldHolder() {
            return MANAGED_FIELD_HOLDER;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (delegate != null && canInputItems()) {
                delegate.setStackInSlot(slot, stack);
            }
        }

        @Override
        public int getSlots() {
            return delegate != null ? delegate.getSlots() : 0;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return delegate != null ? delegate.getStackInSlot(slot) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return delegate != null && canInputItems() ? delegate.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate != null && canOutputItems() ? delegate.extractItem(slot, amount, simulate) :
                    ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return delegate != null ? delegate.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return delegate != null && canInputItems() && delegate.isItemValid(slot, stack);
        }
    }
}
