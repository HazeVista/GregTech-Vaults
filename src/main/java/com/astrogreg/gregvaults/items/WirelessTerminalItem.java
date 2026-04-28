package com.astrogreg.gregvaults.items;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

import com.astrogreg.gregvaults.multiblock.VaultMachine;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;
import com.mojang.datafixers.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class WirelessTerminalItem extends Item {

    private static final Logger LOG = LoggerFactory.getLogger(WirelessTerminalItem.class);
    private static final String TAG_LINKED_POS = "linkedVault";

    public static final String KEY_VAULT_NOT_FORMED = "message.gregtechvaults.vault_not_formed";
    public static final String KEY_VAULT_LINKED = "message.gregtechvaults.vault_linked";
    public static final String KEY_NOT_LINKED = "message.gregtechvaults.terminal_not_linked";
    public static final String KEY_DIMENSION_NOT_FOUND = "message.gregtechvaults.dimension_not_found";
    public static final String KEY_DIFFERENT_DIMENSION = "message.gregtechvaults.different_dimension";
    public static final String KEY_VAULT_NOT_FOUND = "message.gregtechvaults.vault_not_found";
    public static final String KEY_VAULT_TERMINAL_TITLE = "gui.gregtechvaults.vault_terminal";
    public static final String KEY_TOOLTIP_LINKED = "tooltip.gregtechvaults.linked";
    public static final String KEY_TOOLTIP_NOT_LINKED = "tooltip.gregtechvaults.not_linked";
    public static final String KEY_TOOLTIP_HOW_TO_LINK = "tooltip.gregtechvaults.how_to_link";

    public WirelessTerminalItem(Properties properties) {
        super(properties);
    }

    @Nullable
    public static GlobalPos getLinkedPosition(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_LINKED_POS, Tag.TAG_COMPOUND)) {
            return GlobalPos.CODEC
                    .decode(NbtOps.INSTANCE, tag.get(TAG_LINKED_POS))
                    .resultOrPartial(Util.prefix("Vault link", LOG::error))
                    .map(Pair::getFirst)
                    .orElse(null);
        }
        return null;
    }

    public static void link(ItemStack stack, GlobalPos pos) {
        GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, pos)
                .result()
                .ifPresent(tag -> stack.getOrCreateTag().put(TAG_LINKED_POS, tag));
    }

    public static void unlink(ItemStack stack) {
        stack.removeTagKey(TAG_LINKED_POS);
    }

    public static boolean isLinked(ItemStack stack) {
        return getLinkedPosition(stack) != null;
    }

    public static final IVaultLinkableHandler LINKABLE_HANDLER = new IVaultLinkableHandler() {

        @Override
        public boolean canLink(ItemStack stack) {
            return stack.getItem() instanceof WirelessTerminalItem;
        }

        @Override
        public void link(ItemStack stack, GlobalPos pos) {
            WirelessTerminalItem.link(stack, pos);
        }

        @Override
        public void unlink(ItemStack stack) {
            WirelessTerminalItem.unlink(stack);
        }
    };

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (!(level.getBlockEntity(pos) instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity mbe) ||
                !(mbe.getMetaMachine() instanceof VaultMachine vault)) {
            return InteractionResult.PASS;
        }

        if (!vault.isFormed()) {
            serverPlayer.sendSystemMessage(
                    Component.translatable(KEY_VAULT_NOT_FORMED).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }

        ItemStack stack = ctx.getItemInHand();
        link(stack, GlobalPos.of(level.dimension(), pos));
        serverPlayer.sendSystemMessage(
                Component.translatable(KEY_VAULT_LINKED).withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResultHolder.pass(stack);

        GlobalPos linkedPos = getLinkedPosition(stack);
        if (linkedPos == null) {
            serverPlayer.sendSystemMessage(
                    Component.translatable(KEY_NOT_LINKED).withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        ServerLevel targetLevel = serverPlayer.getServer().getLevel(linkedPos.dimension());
        if (targetLevel == null) {
            serverPlayer.sendSystemMessage(
                    Component.translatable(KEY_DIMENSION_NOT_FOUND).withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        if (targetLevel != level) {
            serverPlayer.sendSystemMessage(
                    Component.translatable(KEY_DIFFERENT_DIMENSION).withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        BlockPos pos = linkedPos.pos();
        if (!(targetLevel.getBlockEntity(pos) instanceof com.gregtechceu.gtceu.api.machine.IMachineBlockEntity mbe) ||
                !(mbe.getMetaMachine() instanceof VaultMachine vault)) {
            serverPlayer.sendSystemMessage(
                    Component.translatable(KEY_VAULT_NOT_FOUND).withStyle(ChatFormatting.RED));
            unlink(stack);
            return InteractionResultHolder.fail(stack);
        }

        if (!vault.isFormed()) {
            serverPlayer.sendSystemMessage(
                    Component.translatable(KEY_VAULT_NOT_FORMED).withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        MenuProvider provider = new SimpleMenuProvider(
                (windowId, playerInv, p) -> new VaultTerminalMenu(windowId, playerInv, vault.getItemHandler()),
                Component.translatable(KEY_VAULT_TERMINAL_TITLE));
        NetworkHooks.openScreen(serverPlayer, provider, buf -> buf.writeInt(vault.getTotalSlots()));

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        GlobalPos pos = getLinkedPosition(stack);
        if (pos != null) {
            tooltip.add(Component.translatable(KEY_TOOLTIP_LINKED).withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.literal(
                    "  " + pos.pos().getX() + " " + pos.pos().getY() + " " + pos.pos().getZ())
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable(KEY_TOOLTIP_NOT_LINKED).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable(KEY_TOOLTIP_HOW_TO_LINK).withStyle(ChatFormatting.DARK_GRAY));
    }
}
