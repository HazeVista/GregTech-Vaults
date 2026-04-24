package com.astrogreg.gregvaults.multiblock;

import com.astrogreg.gregvaults.blocks.VaultCoreBlock;
import com.astrogreg.gregvaults.blocks.VaultCoreBlock.CoreTier;
import com.astrogreg.gregvaults.config.VaultConfig;
import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

public class VaultMachine
    extends MultiblockControllerMachine
    implements IDropSaveMachine
{

    public enum VaultTier {
        BRONZE,
        STEEL,
        TITANIUM;

        public CoreTier maxCoreTier() {
            return switch (this) {
                case BRONZE -> CoreTier.MK1;
                case STEEL -> CoreTier.MK2;
                case TITANIUM -> CoreTier.MK3;
            };
        }

        public int baseSlots() {
            return switch (this) {
                case BRONZE -> 36;
                case STEEL -> 72;
                case TITANIUM -> 108;
            };
        }
    }

    private final VaultTier vaultTier;
    private int totalSlots = 0;
    private ItemStackHandler itemHandler;

    public VaultMachine(IMachineBlockEntity holder, VaultTier vaultTier) {
        super(holder);
        this.vaultTier = vaultTier;
        this.itemHandler = createHandler(0);
    }

    private ItemStackHandler createHandler(int size) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
            }
        };
    }

    public VaultTier getVaultTier() {
        return vaultTier;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        int newSlots = countSlots();
        totalSlots = newSlots;
    
        if (itemHandler.getSlots() != newSlots) {
            kickPlayersAndResize(newSlots);
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        kickPlayers();
        totalSlots = 0;
    }

    private void kickPlayers() {
        if (getLevel() instanceof ServerLevel serverLevel) {
            for (ServerPlayer sp : serverLevel.players()) {
                if (
                    sp.containerMenu instanceof VaultContainerMenu menu &&
                    menu.vaultHandler == this.itemHandler
                ) {
                    sp.closeContainer();
                }
            }
        }
    }

    private void kickPlayersAndResize(int newSize) {
        kickPlayers();
        resizeHandler(newSize);
    }

    private void resizeHandler(int newSize) {
        ItemStackHandler newHandler = createHandler(newSize);
        int copyCount = Math.min(itemHandler.getSlots(), newSize);
        for (int i = 0; i < copyCount; i++) {
            newHandler.setStackInSlot(i, itemHandler.getStackInSlot(i));
        }
        itemHandler = newHandler;
        markDirty();
    }

    @Override
    public InteractionResult onUse(
        BlockState state,
        net.minecraft.world.level.Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hit
    ) {
        if (
            !level.isClientSide && player instanceof ServerPlayer serverPlayer
        ) {
            if (!isFormed()) {
                return InteractionResult.PASS;
            }
            MenuProvider provider = new SimpleMenuProvider(
                (windowId, playerInv, p) ->
                    new VaultContainerMenu(windowId, playerInv, itemHandler),
                Component.translatable("gui.gregtechvaults.vault")
            );
            NetworkHooks.openScreen(serverPlayer, provider, buf ->
                buf.writeInt(totalSlots)
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        tag.put("VaultItems", itemHandler.serializeNBT());
        tag.putInt("TotalSlots", totalSlots);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        totalSlots = tag.getInt("TotalSlots");
        itemHandler = createHandler(Math.max(totalSlots, 0));
        if (tag.contains("VaultItems")) {
            itemHandler.deserializeNBT(tag.getCompound("VaultItems"));
        }
    }

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        tag.put("VaultItems", itemHandler.serializeNBT());
        tag.putInt("TotalSlots", totalSlots);
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        totalSlots = tag.getInt("TotalSlots");
        itemHandler = createHandler(Math.max(totalSlots, 0));
        if (tag.contains("VaultItems")) {
            itemHandler.deserializeNBT(tag.getCompound("VaultItems"));
        }
    }

    private int countSlots() {
        if (getLevel() == null) return vaultTier.baseSlots();

        Direction facing = getFrontFacing();
        Direction back = facing.getOpposite();
        Direction right = facing.getClockWise();
        Direction left = right.getOpposite();

        BlockPos origin = getPos();
        int slots = vaultTier.baseSlots();

        for (int d = 1; d <= 3; d++) {
            for (int h = -1; h <= 1; h++) {
                for (int w = -1; w <= 1; w++) {
                    BlockPos p = origin
                        .relative(back, d)
                        .relative(
                            h >= 0 ? Direction.UP : Direction.DOWN,
                            Math.abs(h)
                        )
                        .relative(w >= 0 ? right : left, Math.abs(w));

                    BlockState s = getLevel().getBlockState(p);
                    if (s.getBlock() instanceof VaultCoreBlock core) {
                        CoreTier coreTier = core.getTier();
                        if (coreTier.level <= vaultTier.maxCoreTier().level) {
                            slots += VaultConfig.getSlotValue(coreTier);
                        }
                    }
                }
            }
        }
        return slots;
    }
}
