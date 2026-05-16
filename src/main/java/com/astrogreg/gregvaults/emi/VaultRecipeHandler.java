package com.astrogreg.gregvaults.emi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.astrogreg.gregvaults.network.CPacketVaultScroll;
import com.astrogreg.gregvaults.network.VaultNetwork;
import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public class VaultRecipeHandler<T extends AbstractContainerMenu>
                               implements StandardRecipeHandler<T> {

    private boolean isCrafting = false;

    @Override
    public List<Slot> getInputSources(T menu) {
        AbstractContainerMenu vaultMenu = getVaultMenu();
        if (vaultMenu == null) vaultMenu = menu;

        if (!isCrafting) {
            ItemStack[] fullContents = getFullContentsFromVault();
            List<Slot> result = new ArrayList<>();
            if (fullContents != null) {
                for (ItemStack stack : fullContents) {
                    result.add(new FakeSlot(stack != null ? stack : ItemStack.EMPTY));
                }
            }
            int invStart = getPlayerSlotsStart(vaultMenu);
            int invEnd = getCraftingSlotsStart(vaultMenu);
            for (int i = invStart; i < invEnd; i++) {
                result.add(vaultMenu.slots.get(i));
            }
            return result;
        } else {
            List<Slot> result = new ArrayList<>();
            if (vaultMenu instanceof VaultContainerMenu m) {
                for (int i = m.fullVaultSlotsStart; i < m.fullVaultSlotsStart + m.totalSlots; i++) {
                    result.add(vaultMenu.slots.get(i));
                }
                int invStart = m.playerSlotsStart;
                int invEnd = m.craftingSlotsStart;
                for (int i = invStart; i < invEnd; i++) {
                    result.add(vaultMenu.slots.get(i));
                }
            } else if (vaultMenu instanceof VaultTerminalMenu m) {
                for (int i = m.fullVaultSlotsStart; i < m.fullVaultSlotsStart + m.totalSlots; i++) {
                    result.add(vaultMenu.slots.get(i));
                }
                int invStart = m.playerSlotsStart;
                int invEnd = m.craftingSlotsStart;
                for (int i = invStart; i < invEnd; i++) {
                    result.add(vaultMenu.slots.get(i));
                }
            }
            return result;
        }
    }

    @Override
    public List<Slot> getCraftingSlots(T menu) {
        AbstractContainerMenu vaultMenu = getVaultMenu();
        if (vaultMenu instanceof VaultContainerMenu m) {
            return vaultMenu.slots.subList(m.craftingSlotsStart, m.craftingOutputStart);
        }
        if (vaultMenu instanceof VaultTerminalMenu m) {
            return vaultMenu.slots.subList(m.craftingSlotsStart, m.craftingOutputStart);
        }
        return List.of();
    }

    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<T> screen) {
        ItemStack[] fullContents = getFullContentsFromVault();
        List<EmiStack> stacks = new ArrayList<>();

        if (fullContents != null) {
            for (ItemStack s : fullContents) {
                if (s != null && !s.isEmpty()) {
                    stacks.add(EmiStack.of(s));
                }
            }
        }

        AbstractContainerMenu vaultMenu = getVaultMenu();
        if (vaultMenu != null) {
            int invStart = getPlayerSlotsStart(vaultMenu);
            int invEnd = getCraftingSlotsStart(vaultMenu);
            for (int i = invStart; i < invEnd; i++) {
                Slot slot = vaultMenu.slots.get(i);
                if (slot.hasItem()) stacks.add(EmiStack.of(slot.getItem()));
            }
        }

        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory().equals(VanillaEmiRecipeCategories.CRAFTING) && recipe.getInputs().size() <= 9;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<T> context) {
        ItemStack[] fullContents = getFullContentsFromVault();
        if (fullContents == null) return false;

        AbstractContainerMenu vaultMenu = getVaultMenu();
        if (vaultMenu == null) return false;

        int[] available = buildAvailability(fullContents, vaultMenu);
        for (EmiIngredient ingredient : recipe.getInputs()) {
            if (ingredient.isEmpty()) continue;
            if (!consumeOne(ingredient, available, fullContents, vaultMenu)) return false;
        }
        return true;
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<T> context) {
        AbstractContainerMenu vaultMenu = getVaultMenu();
        if (vaultMenu instanceof VaultContainerMenu m) {
            m.updateScroll(0);
            VaultNetwork.CHANNEL.sendToServer(new CPacketVaultScroll(0));
        } else if (vaultMenu instanceof VaultTerminalMenu m) {
            m.updateScroll(0);
            VaultNetwork.CHANNEL.sendToServer(new CPacketVaultScroll(0));
        }

        isCrafting = true;
        try {
            return StandardRecipeHandler.super.craft(recipe, context);
        } finally {
            isCrafting = false;
        }
    }

    private AbstractContainerMenu getVaultMenu() {
        if (Minecraft.getInstance().player == null) return null;
        AbstractContainerMenu menu = Minecraft.getInstance().player.containerMenu;
        if (menu instanceof VaultContainerMenu || menu instanceof VaultTerminalMenu) return menu;
        return null;
    }

    private ItemStack[] getFullContentsFromVault() {
        AbstractContainerMenu menu = getVaultMenu();
        if (menu instanceof VaultContainerMenu m) return m.clientCache;
        if (menu instanceof VaultTerminalMenu m) return m.clientCache;
        return null;
    }

    private int[] buildAvailability(ItemStack[] vault, AbstractContainerMenu menu) {
        int vaultSize = vault.length;
        int invStart = getPlayerSlotsStart(menu);
        int invEnd = getCraftingSlotsStart(menu);
        int invSize = Math.max(0, invEnd - invStart);

        int[] counts = new int[vaultSize + invSize];
        for (int i = 0; i < vaultSize; i++) {
            counts[i] = vault[i] != null ? vault[i].getCount() : 0;
        }
        for (int i = 0; i < invSize; i++) {
            Slot slot = menu.slots.get(invStart + i);
            counts[vaultSize + i] = slot.hasItem() ? slot.getItem().getCount() : 0;
        }
        return counts;
    }

    private boolean consumeOne(EmiIngredient ingredient, int[] available,
                               ItemStack[] vault, AbstractContainerMenu menu) {
        int vaultSize = vault.length;
        int invStart = getPlayerSlotsStart(menu);
        int invEnd = getCraftingSlotsStart(menu);
        int invSize = Math.max(0, invEnd - invStart);

        for (int i = 0; i < invSize; i++) {
            int idx = vaultSize + i;
            if (available[idx] <= 0) continue;
            Slot slot = menu.slots.get(invStart + i);
            if (!slot.hasItem()) continue;
            if (matchesIngredient(ingredient, slot.getItem())) {
                available[idx]--;
                return true;
            }
        }
        for (int i = 0; i < vaultSize; i++) {
            if (available[i] <= 0) continue;
            ItemStack s = vault[i];
            if (s == null || s.isEmpty()) continue;
            if (matchesIngredient(ingredient, s)) {
                available[i]--;
                return true;
            }
        }
        return false;
    }

    private boolean matchesIngredient(EmiIngredient ingredient, ItemStack stack) {
        for (EmiStack emiStack : ingredient.getEmiStacks()) {
            ItemStack item = emiStack.getItemStack();
            if (item != null && !item.isEmpty() && ItemStack.isSameItem(item, stack)) return true;
        }
        return false;
    }

    private int getPlayerSlotsStart(AbstractContainerMenu menu) {
        if (menu instanceof VaultContainerMenu m) return m.playerSlotsStart;
        if (menu instanceof VaultTerminalMenu m) return m.playerSlotsStart;
        return menu.slots.size();
    }

    private int getCraftingSlotsStart(AbstractContainerMenu menu) {
        if (menu instanceof VaultContainerMenu m) return m.craftingSlotsStart;
        if (menu instanceof VaultTerminalMenu m) return m.craftingSlotsStart;
        return menu.slots.size();
    }

    private static class FakeSlot extends Slot {

        private static final Container DUMMY = new SimpleContainer(1);
        private final ItemStack stack;

        public FakeSlot(ItemStack stack) {
            super(DUMMY, 0, 0, 0);
            this.stack = stack;
        }

        @Override
        public ItemStack getItem() {
            return stack;
        }

        @Override
        public boolean hasItem() {
            return !stack.isEmpty();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
