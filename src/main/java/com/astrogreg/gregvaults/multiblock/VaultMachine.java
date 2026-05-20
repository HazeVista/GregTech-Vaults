package com.astrogreg.gregvaults.multiblock;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import com.astrogreg.gregvaults.blocks.VaultCoreBlock;
import com.astrogreg.gregvaults.blocks.VaultCoreBlock.CoreTier;
import com.astrogreg.gregvaults.config.VaultConfig;
import com.astrogreg.gregvaults.network.SPacketVaultContents;
import com.astrogreg.gregvaults.network.SPacketVaultDelta;
import com.astrogreg.gregvaults.network.VaultNetwork;
import com.astrogreg.gregvaults.screen.VaultContainerMenu;

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
    private ItemStack[] savedCraftingGrid = new ItemStack[9];

    public enum BatchSyncMode {
        AUTO,
        DELTA_ONLY,
        FULL_SNAPSHOT
    }

    private int batchDepth = 0;
    private BatchSyncMode batchSyncMode = BatchSyncMode.AUTO;
    private final java.util.Set<Integer> dirtySlots = new java.util.HashSet<>();

    private final java.util.Set<ServerPlayer> activeViewers = new java.util.HashSet<>();
    private final java.util.Map<java.util.UUID, ItemStack[]> lastSentByViewer = new java.util.HashMap<>();

    private ItemStack[] getLastSentItems(ServerPlayer viewer) {
        return lastSentByViewer.computeIfAbsent(viewer.getUUID(), k -> {
            ItemStack[] arr = new ItemStack[itemHandler.getSlots()];
            java.util.Arrays.fill(arr, ItemStack.EMPTY);
            return arr;
        });
    }

    public void addViewer(ServerPlayer player) {
        activeViewers.add(player);
    }

    public void removeViewer(ServerPlayer player) {
        activeViewers.remove(player);
        lastSentByViewer.remove(player.getUUID());
    }

    public VaultMachine(IMachineBlockEntity holder, VaultTier vaultTier) {
        super(holder);
        this.vaultTier = vaultTier;
        this.itemHandler = createHandler(0);
        java.util.Arrays.fill(this.savedCraftingGrid, ItemStack.EMPTY);
    }

    private ItemStackHandler createHandler(int size) {
        return new ItemStackHandler(size) {

            @Override
            protected void onContentsChanged(int slot) {
                markDirty();
                if (batchDepth > 0) {
                    dirtySlots.add(slot);
                } else {
                    notifySlotChanged(slot);
                }
            }
        };
    }

    public void beginBatch() {
        beginBatch(BatchSyncMode.AUTO);
    }

    public void beginBatch(BatchSyncMode mode) {
        BatchSyncMode requestedMode = mode == null ? BatchSyncMode.AUTO : mode;
        if (batchDepth == 0) {
            dirtySlots.clear();
            batchSyncMode = requestedMode;
        } else if (requestedMode == BatchSyncMode.FULL_SNAPSHOT) {
            batchSyncMode = BatchSyncMode.FULL_SNAPSHOT;
        } else if (requestedMode == BatchSyncMode.DELTA_ONLY && batchSyncMode != BatchSyncMode.FULL_SNAPSHOT) {
            batchSyncMode = BatchSyncMode.DELTA_ONLY;
        }
        batchDepth++;
    }

    public void endBatch() {
        if (batchDepth <= 0) {
            batchDepth = 0;
            batchSyncMode = BatchSyncMode.AUTO;
            dirtySlots.clear();
            return;
        }

        batchDepth--;
        if (batchDepth > 0) return;

        if (dirtySlots.isEmpty()) {
            batchSyncMode = BatchSyncMode.AUTO;
            return;
        }
        if (activeViewers.isEmpty()) {
            dirtySlots.clear();
            batchSyncMode = BatchSyncMode.AUTO;
            return;
        }

        if (batchSyncMode == BatchSyncMode.FULL_SNAPSHOT || dirtySlots.size() > 256) {
            sendFullSnapshot(new java.util.ArrayList<>(activeViewers));
        } else if (batchSyncMode != BatchSyncMode.DELTA_ONLY || !dirtySlots.isEmpty()) {
            sendDirtySlotDeltas();
        }
        dirtySlots.clear();
        batchSyncMode = BatchSyncMode.AUTO;
    }

    private void sendDirtySlotDeltas() {
        for (ServerPlayer sp : activeViewers) {
            SPacketVaultDelta.Builder builder = new SPacketVaultDelta.Builder(sp.containerMenu.containerId);
            for (int slot : dirtySlots) buildDeltaEntry(builder, slot, sp);
            if (!builder.isEmpty())
                VaultNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                        builder.build());
        }
    }

    private void notifySlotChanged(int slot) {
        if (activeViewers.isEmpty()) return;
        for (ServerPlayer sp : activeViewers) {
            SPacketVaultDelta.Builder builder = new SPacketVaultDelta.Builder(sp.containerMenu.containerId);
            buildDeltaEntry(builder, slot, sp);
            if (!builder.isEmpty())
                VaultNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                        builder.build());
        }
    }

    private void buildDeltaEntry(SPacketVaultDelta.Builder builder, int slot, ServerPlayer viewer) {
        ItemStack current = itemHandler.getStackInSlot(slot);
        ItemStack[] sent = getLastSentItems(viewer);
        ItemStack last = (slot < sent.length) ? sent[slot] : ItemStack.EMPTY;
        if (last == null) last = ItemStack.EMPTY;

        if (current.isEmpty()) {
            if (!last.isEmpty()) {
                builder.addRemoved(slot);
                sent[slot] = ItemStack.EMPTY;
            }
        } else if (!last.isEmpty() && ItemStack.isSameItemSameTags(current, last)) {
            if (current.getCount() != last.getCount()) {
                builder.addCountOnly(slot, current.getCount());
                sent[slot] = current.copy();
            }
        } else {
            builder.addFull(slot, current.copy());
            sent[slot] = current.copy();
        }
    }

    private void sendFullSnapshot(java.util.List<ServerPlayer> watching) {
        ItemStack[] stacks = new ItemStack[itemHandler.getSlots()];
        for (int i = 0; i < stacks.length; i++) stacks[i] = itemHandler.getStackInSlot(i).copy();
        for (ServerPlayer sp : watching) {
            VaultNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new SPacketVaultContents(sp.containerMenu.containerId, stacks));
            ItemStack[] sent = getLastSentItems(sp);
            for (int i = 0; i < stacks.length && i < sent.length; i++) sent[i] = stacks[i].copy();
        }
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

    public ItemStack[] getSavedCraftingGrid() {
        return savedCraftingGrid;
    }

    public void setSavedCraftingGrid(ItemStack[] grid) {
        this.savedCraftingGrid = grid;
        markDirty();
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
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        kickPlayers();
        totalSlots = 0;
    }

    private void kickPlayers() {
        for (ServerPlayer sp : new java.util.ArrayList<>(activeViewers)) {
            sp.closeContainer();
        }
        activeViewers.clear();
        lastSentByViewer.clear();
    }

    private void kickPlayersAndResize(int newSize) {
        kickPlayers();
        resizeHandler(newSize);
    }

    private void resizeHandler(int newSize) {
        if (newSize < itemHandler.getSlots() && getLevel() instanceof ServerLevel serverLevel) {
            BlockPos pos = getPos();
            for (int i = newSize; i < itemHandler.getSlots(); i++) {
                ItemStack overflow = itemHandler.getStackInSlot(i);
                if (overflow.isEmpty()) continue;
                while (!overflow.isEmpty()) {
                    int take = Math.min(overflow.getMaxStackSize(), overflow.getCount());
                    net.minecraft.world.level.block.Block.popResource(serverLevel, pos, overflow.copyWithCount(take));
                    overflow.shrink(take);
                }
            }
        }

        ItemStackHandler newHandler = createHandler(newSize);
        int copyCount = Math.min(itemHandler.getSlots(), newSize);
        for (int i = 0; i < copyCount; i++) {
            newHandler.setStackInSlot(i, itemHandler.getStackInSlot(i));
        }
        itemHandler = newHandler;
        activeViewers.clear();
        lastSentByViewer.clear();
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
            if (!isFormed()) return InteractionResult.PASS;
            final int[] windowIdHolder = { -1 };
            MenuProvider provider = new SimpleMenuProvider(
                    (windowId, playerInv, p) -> {
                        windowIdHolder[0] = windowId;
                        VaultContainerMenu menu = new VaultContainerMenu(windowId, playerInv, itemHandler,
                                VaultMachine.this);
                        menu.initCraftingGrid(savedCraftingGrid);
                        menu.setOnGridClose(grid -> {
                            savedCraftingGrid = grid;
                            markDirty();
                        });
                        return menu;
                    },
                    Component.translatable("gui.gregtechvaults.vault"));
            NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeInt(totalSlots));
            if (windowIdHolder[0] >= 0) {
                sendFullContents(serverPlayer, windowIdHolder[0]);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void sendFullContents(ServerPlayer player, int containerId) {
        ItemStack[] stacks = new ItemStack[itemHandler.getSlots()];
        for (int i = 0; i < stacks.length; i++) stacks[i] = itemHandler.getStackInSlot(i).copy();
        VaultNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SPacketVaultContents(containerId, stacks));
        ItemStack[] sent = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) sent[i] = stacks[i].copy();
        lastSentByViewer.put(player.getUUID(), sent);
        activeViewers.add(player);
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

        net.minecraft.nbt.ListTag craftList = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < savedCraftingGrid.length; i++) {
            ItemStack s = savedCraftingGrid[i];
            if (s == null || s.isEmpty()) continue;
            net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
            entry.putInt("Slot", i);
            s.save(entry);
            craftList.add(entry);
        }
        tag.put("CraftingGrid", craftList);
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

        java.util.Arrays.fill(savedCraftingGrid, ItemStack.EMPTY);
        if (tag.contains("CraftingGrid", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag craftList = tag.getList("CraftingGrid", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < craftList.size(); i++) {
                net.minecraft.nbt.CompoundTag entry = craftList.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < savedCraftingGrid.length) {
                    savedCraftingGrid[slot] = ItemStack.of(entry);
                }
            }
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
}