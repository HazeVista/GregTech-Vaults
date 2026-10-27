package com.astrogreg.gregvaults.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;

import com.astrogreg.gregvaults.screen.AbstractVaultMenu;
import com.astrogreg.gregvaults.storage.VaultStorage;

import java.util.function.Supplier;

public class CPacketFillCraftingGrid {

    private final ResourceLocation recipeId;
    private final ItemStack[] template;

    public CPacketFillCraftingGrid(ResourceLocation recipeId, ItemStack[] template) {
        this.recipeId = recipeId;
        this.template = template;
    }

    public static void encode(CPacketFillCraftingGrid packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.recipeId != null);
        if (packet.recipeId != null) buf.writeResourceLocation(packet.recipeId);
        for (int i = 0; i < 9; i++) buf.writeItem(packet.template[i]);
    }

    public static CPacketFillCraftingGrid decode(FriendlyByteBuf buf) {
        ResourceLocation id = buf.readBoolean() ? buf.readResourceLocation() : null;
        ItemStack[] template = new ItemStack[9];
        for (int i = 0; i < 9; i++) template[i] = buf.readItem();
        return new CPacketFillCraftingGrid(id, template);
    }

    public static void handle(CPacketFillCraftingGrid packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null || player.isSpectator()) return;
            if (!(player.containerMenu instanceof AbstractVaultMenu menu) || !menu.stillValid(player)) return;
            if (!(menu.vaultHandler instanceof ItemStackHandler vaultHandler)) return;

            Ingredient[] grid = resolveGrid(packet, player);
            fillCraftingGrid(menu, vaultHandler, grid, alignTemplate(grid, packet.template), player);
        });
        ctx.get().setPacketHandled(true);
    }

    private static Ingredient[] resolveGrid(CPacketFillCraftingGrid packet, ServerPlayer player) {
        Ingredient[] grid = new Ingredient[9];
        for (int i = 0; i < 9; i++) grid[i] = Ingredient.EMPTY;

        if (packet.recipeId != null) {
            var recipeOpt = player.level().getRecipeManager().byKey(packet.recipeId);
            if (recipeOpt.isPresent()) {
                Recipe<?> recipe = recipeOpt.get();
                var ingredients = recipe.getIngredients();

                if (recipe instanceof ShapedRecipe shaped) {
                    int w = shaped.getWidth();
                    for (int row = 0; row < shaped.getHeight(); row++) {
                        for (int col = 0; col < w; col++) {
                            int src = row * w + col;
                            int dst = row * 3 + col;
                            if (src < ingredients.size()) grid[dst] = ingredients.get(src);
                        }
                    }
                } else {
                    for (int i = 0; i < Math.min(ingredients.size(), 9); i++) {
                        grid[i] = ingredients.get(i);
                    }
                }
                return grid;
            }
        }

        for (int i = 0; i < 9; i++) {
            if (!packet.template[i].isEmpty()) {
                grid[i] = Ingredient.of(packet.template[i]);
            }
        }
        return grid;
    }

    private static ItemStack[] alignTemplate(Ingredient[] grid, ItemStack[] template) {
        ItemStack[] aligned = new ItemStack[9];
        boolean[] used = new boolean[9];
        for (int slot = 0; slot < 9; slot++) {
            aligned[slot] = ItemStack.EMPTY;
            Ingredient ingredient = grid[slot];
            if (ingredient == null || ingredient.isEmpty()) continue;

            int selected = !used[slot] && !template[slot].isEmpty() && ingredient.test(template[slot]) ? slot : -1;
            for (int candidate = 0; selected < 0 && candidate < 9; candidate++) {
                if (!used[candidate] && !template[candidate].isEmpty() && ingredient.test(template[candidate])) {
                    selected = candidate;
                }
            }
            if (selected >= 0) {
                aligned[slot] = template[selected];
                used[selected] = true;
            }
        }
        return aligned;
    }

    private static void fillCraftingGrid(AbstractVaultMenu menu, ItemStackHandler vaultHandler,
                                         Ingredient[] grid, ItemStack[] template, ServerPlayer player) {
        if (menu.machine != null)
            menu.machine.beginBatch();
        menu.beginCraftingGridBulkUpdate();
        try {
            for (int g = 0; g < menu.craftingGrid.getContainerSize(); g++) {
                ItemStack existing = menu.craftingGrid.getItem(g);
                if (existing.isEmpty()) continue;
                ItemStack remaining = tryInsertVault(vaultHandler, existing.copy());
                if (!remaining.isEmpty()) {
                    if (!player.getInventory().add(remaining)) {
                        player.drop(remaining, false);
                    }
                }
                menu.craftingGrid.setItem(g, ItemStack.EMPTY);
            }

            for (int g = 0; g < 9; g++) {
                Ingredient ing = grid[g];
                if (ing == null || ing.isEmpty()) continue;
                ItemStack requested = template[g];
                if (requested.isEmpty() || !ing.test(requested)) continue;

                ItemStack pulled = tryExtractVault(vaultHandler, requested);
                if (pulled.isEmpty()) pulled = tryExtractPlayerInv(menu, player, requested);
                if (!pulled.isEmpty()) {
                    menu.craftingGrid.setItem(g, pulled);
                }
            }
        } finally {
            menu.endCraftingGridBulkUpdate();
            if (menu.machine != null) menu.machine.endBatch();
        }
    }

    private static ItemStack tryExtractVault(ItemStackHandler handler, ItemStack requested) {
        if (handler instanceof VaultStorage storage) {
            return storage.extract(VaultStorage.StackKey.of(requested), 1, false);
        }
        for (int v = 0; v < handler.getSlots(); v++) {
            ItemStack s = handler.getStackInSlot(v);
            if (!ItemStack.isSameItemSameTags(s, requested)) continue;
            ItemStack extracted = handler.extractItem(v, 1, false);
            if (!extracted.isEmpty()) return extracted;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack tryInsertVault(ItemStackHandler handler, ItemStack stack) {
        if (handler instanceof VaultStorage storage) return storage.insert(stack, false);
        for (int v = 0; v < handler.getSlots() && !stack.isEmpty(); v++) {
            stack = handler.insertItem(v, stack, false);
        }
        return stack;
    }

    private static ItemStack tryExtractPlayerInv(AbstractVaultMenu menu, ServerPlayer player, ItemStack requested) {
        for (int i = menu.playerSlotsStart; i < menu.craftingSlotsStart; i++) {
            var slot = menu.slots.get(i);
            if (!slot.mayPickup(player) || !ItemStack.isSameItemSameTags(slot.getItem(), requested)) continue;
            return slot.safeTake(1, 1, player);
        }
        return ItemStack.EMPTY;
    }
}
