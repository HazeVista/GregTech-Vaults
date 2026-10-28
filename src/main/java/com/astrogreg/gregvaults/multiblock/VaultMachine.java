package com.astrogreg.gregvaults.multiblock;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDropSaveMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineModifyDrops;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.common.machine.owner.MachineOwner;

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
import net.minecraftforge.network.NetworkHooks;

import com.astrogreg.gregvaults.blocks.VaultCoreBlock;
import com.astrogreg.gregvaults.blocks.VaultCoreBlock.CoreTier;
import com.astrogreg.gregvaults.config.VaultConfig;
import com.astrogreg.gregvaults.screen.AbstractVaultMenu;
import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.storage.VaultStorage;

import java.util.List;

public class VaultMachine
                          extends MultiblockControllerMachine
                          implements IDropSaveMachine, IMachineModifyDrops {

    private static final int MAX_STORAGE_SLOTS = 1_000_000;

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
    private VaultStorage itemHandler;
    private ItemStack[] legacyCraftingItems = new ItemStack[9];

    private int batchDepth = 0;
    private boolean changedDuringBatch;

    private final java.util.Set<ServerPlayer> activeViewers = new java.util.HashSet<>();

    public void addViewer(ServerPlayer player) {
        activeViewers.add(player);
    }

    public void removeViewer(ServerPlayer player) {
        activeViewers.remove(player);
    }

    public VaultMachine(IMachineBlockEntity holder, VaultTier vaultTier) {
        super(holder);
        this.vaultTier = vaultTier;
        this.itemHandler = createHandler(0);
        java.util.Arrays.fill(this.legacyCraftingItems, ItemStack.EMPTY);
    }

    private VaultStorage createHandler(int size) {
        return new VaultStorage(size, slot -> {
            markDirty();
            if (batchDepth > 0) {
                changedDuringBatch = true;
            } else {
                refreshCatalogViewers();
            }
        });
    }

    public void beginBatch() {
        if (batchDepth == 0) changedDuringBatch = false;
        batchDepth++;
    }

    public void endBatch() {
        if (batchDepth <= 0) {
            batchDepth = 0;
            changedDuringBatch = false;
            return;
        }

        batchDepth--;
        if (batchDepth > 0) return;
        if (changedDuringBatch) refreshCatalogViewers();
        changedDuringBatch = false;
    }

    private void refreshCatalogViewers() {
        activeViewers.removeIf(player -> {
            if (player.containerMenu instanceof AbstractVaultMenu menu && menu.machine == this) {
                menu.refreshCatalog(player);
                return false;
            }
            return true;
        });
    }

    public VaultTier getVaultTier() {
        return vaultTier;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public int getAvailableSlots() {
        int empty = 0;
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (itemHandler.getStackInSlot(i).isEmpty()) empty++;
        }
        return empty;
    }

    public VaultStorage getItemHandler() {
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
        recoverLegacyCraftingItems();

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

        VaultStorage newHandler = createHandler(newSize);
        int copyCount = Math.min(itemHandler.getSlots(), newSize);
        for (int i = 0; i < copyCount; i++) {
            newHandler.setStackInSlot(i, itemHandler.getStackInSlot(i));
        }
        itemHandler = newHandler;
        activeViewers.clear();
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
            if (!MachineOwner.canOpenOwnerMachine(player, this)) return InteractionResult.FAIL;
            recoverLegacyCraftingItems();
            final int[] windowIdHolder = { -1 };
            MenuProvider provider = new SimpleMenuProvider(
                    (windowId, playerInv, p) -> {
                        windowIdHolder[0] = windowId;
                        return new VaultContainerMenu(windowId, playerInv, itemHandler, VaultMachine.this);
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
        activeViewers.add(player);
        if (player.containerMenu.containerId == containerId && player.containerMenu instanceof AbstractVaultMenu menu) {
            menu.sendCatalogPage(player);
        }
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
        for (int i = 0; i < legacyCraftingItems.length; i++) {
            ItemStack s = legacyCraftingItems[i];
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
            net.minecraft.nbt.ListTag list = tag.getCompound("VaultItems")
                    .getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                net.minecraft.nbt.CompoundTag entry = list.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < itemHandler.getSlots()) {
                    itemHandler.setStackInSlot(slot, ItemStack.of(entry));
                }
            }
        }

        java.util.Arrays.fill(legacyCraftingItems, ItemStack.EMPTY);
        if (tag.contains("CraftingGrid", net.minecraft.nbt.Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag craftList = tag.getList("CraftingGrid", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int i = 0; i < craftList.size(); i++) {
                net.minecraft.nbt.CompoundTag entry = craftList.getCompound(i);
                int slot = entry.getInt("Slot");
                if (slot >= 0 && slot < legacyCraftingItems.length) {
                    legacyCraftingItems[slot] = ItemStack.of(entry);
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
        totalSlots = readStorageSize(tag);
        itemHandler = createHandler(totalSlots);
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
        totalSlots = readStorageSize(tag);
        itemHandler = createHandler(totalSlots);
        deserializeItemsFromTag(tag);
    }

    private static int readStorageSize(CompoundTag tag) {
        int slots = tag.getInt("TotalSlots");
        if (slots <= 0 && tag.contains("VaultItems", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            slots = tag.getCompound("VaultItems").getInt("Size");
        }
        return Math.max(0, Math.min(slots, MAX_STORAGE_SLOTS));
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
        if (itemHandler == null) return;

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
        for (int slot = 0; slot < legacyCraftingItems.length; slot++) {
            ItemStack stack = legacyCraftingItems[slot];
            if (stack.isEmpty()) continue;
            drops.add(stack);
            legacyCraftingItems[slot] = ItemStack.EMPTY;
        }
        markDirty();
    }

    public void recoverLegacyCraftingItems() {
        boolean changed = false;
        beginBatch();
        try {
            for (int slot = 0; slot < legacyCraftingItems.length; slot++) {
                ItemStack stack = legacyCraftingItems[slot];
                if (stack.isEmpty()) continue;
                ItemStack remainder = itemHandler.insert(stack, false);
                if (remainder.getCount() != stack.getCount()) changed = true;
                legacyCraftingItems[slot] = remainder;
            }
        } finally {
            endBatch();
        }
        if (changed) markDirty();
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
