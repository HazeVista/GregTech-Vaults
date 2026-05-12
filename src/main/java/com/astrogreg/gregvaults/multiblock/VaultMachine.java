package com.astrogreg.gregvaults.multiblock;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import com.astrogreg.gregvaults.blocks.VaultCoreBlock;
import com.astrogreg.gregvaults.blocks.VaultCoreBlock.CoreTier;
import com.astrogreg.gregvaults.config.VaultConfig;
import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;

import java.util.List;

public class VaultMachine
        extends MultiblockControllerMachine
        implements IDropSaveMachine, IMachineModifyDrops {

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
                case BRONZE -> VaultConfig.INSTANCE.vaultValues.bronzeVault.bronzeBaseSlots;
                case STEEL -> VaultConfig.INSTANCE.vaultValues.steelVault.steelBaseSlots;
                case TITANIUM -> VaultConfig.INSTANCE.vaultValues.titaniumVault.titaniumBaseSlots;
            };
        }

        public boolean wirelessAllowed() {
            return switch (this) {
                case BRONZE -> VaultConfig.INSTANCE.vaultValues.bronzeVault.bronzeWireless;
                case STEEL -> VaultConfig.INSTANCE.vaultValues.steelVault.steelWireless;
                case TITANIUM -> VaultConfig.INSTANCE.vaultValues.titaniumVault.titaniumWireless;
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

        for (IMultiPart part : getParts()) {
            if (part instanceof VaultInterfacePart iface) {
                iface.refreshHandlerFromVault();
            }
        }

        subscribeServerTick(this::onServerTick);
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
                boolean shouldKick =
                        (sp.containerMenu instanceof VaultContainerMenu menu &&
                                menu.vaultHandler == this.itemHandler) ||
                                (sp.containerMenu instanceof VaultTerminalMenu tMenu &&
                                        tMenu.vaultHandler == this.itemHandler);
                if (shouldKick) {
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
            BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!isFormed()) {
                return InteractionResult.PASS;
            }
            MenuProvider provider = new SimpleMenuProvider(
                    (windowId, playerInv, p) -> new VaultContainerMenu(windowId, playerInv, itemHandler),
                    Component.translatable("gui.gregtechvaults.vault"));
            NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeInt(totalSlots));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void serializeItemsToTag(CompoundTag tag) {
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            net.minecraft.world.item.ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
            entry.putInt("Slot", i);
            stack.save(entry);
            list.add(entry);
        }
        tag.put("VaultSlots", list);
        tag.putInt("TotalSlots", totalSlots);
        tag.remove("VaultItems");
    }

    private void deserializeItemsFromTag(CompoundTag tag) {
        if (tag.contains("VaultSlots", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag list = tag.getList("VaultSlots", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                net.minecraft.nbt.CompoundTag entry = list.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < itemHandler.getSlots()) {
                    itemHandler.setStackInSlot(slot, net.minecraft.world.item.ItemStack.of(entry));
                }
            }
        } else if (tag.contains("VaultItems", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            itemHandler.deserializeNBT(tag.getCompound("VaultItems"));
        }
    }

    @Override
    public void saveToItem(CompoundTag tag) {
        serializeItemsToTag(tag);
    }

    @Override
    public void loadFromItem(CompoundTag tag) {
        totalSlots = tag.getInt("TotalSlots");
        itemHandler = createHandler(Math.max(totalSlots, 0));
        deserializeItemsFromTag(tag);
    }

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        serializeItemsToTag(tag);
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        totalSlots = tag.getInt("TotalSlots");
        itemHandler = createHandler(Math.max(totalSlots, 0));
        deserializeItemsFromTag(tag);
    }

    @Override
    public boolean saveBreak() {
        return false;
    }

    @Override
    public void onDrops(List<ItemStack> drops) {
        dropInventoryContents(drops);
    }

    private void dropInventoryContents(List<ItemStack> drops) {
        if (itemHandler == null || itemHandler.getSlots() <= 0) {
            return;
        }

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            while (!stack.isEmpty()) {
                int take = Math.min(stack.getMaxStackSize(), stack.getCount());
                drops.add(stack.copyWithCount(take));
                stack.shrink(take);
            }
            itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        }
        markDirty();
    }

    private int countSlots() {
        if (getLevel() == null) return vaultTier.baseSlots();

        Direction facing = getFrontFacing();
        Direction upward = getUpwardsFacing();
        boolean flipped = isFlipped();

        Direction back = com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.BACK.getRelative(facing, upward,
                flipped);
        Direction right = com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.RIGHT.getRelative(facing, upward,
                flipped);
        Direction up = com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.UP.getRelative(facing, upward, flipped);
        Direction left = right.getOpposite();
        Direction down = up.getOpposite();

        BlockPos origin = getPos();
        int slots = vaultTier.baseSlots();

        for (int d = 1; d <= 3; d++) {
            for (int h = -1; h <= 1; h++) {
                for (int w = -1; w <= 1; w++) {
                    BlockPos p = origin
                            .relative(back, d)
                            .relative(h >= 0 ? up : down, Math.abs(h))
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

    private void onServerTick() {}
}