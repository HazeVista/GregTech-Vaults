package com.astrogreg.gregvaults.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import com.astrogreg.gregvaults.items.WirelessTerminalItem;
import com.astrogreg.gregvaults.items.WirelessTerminalItem.EmitterTier;
import com.astrogreg.gregvaults.registry.VaultItems;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public class EmitterUpgradeRecipe extends CustomRecipe {

    private final EmitterTier tier;
    private final net.minecraft.world.item.Item emitterItem;

    public EmitterUpgradeRecipe(ResourceLocation id, EmitterTier tier,
                                net.minecraft.world.item.Item emitterItem) {
        super(id, CraftingBookCategory.MISC);
        this.tier = tier;
        this.emitterItem = emitterItem;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        boolean foundTerminal = false;
        boolean foundEmitter = false;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof WirelessTerminalItem) {

                if (WirelessTerminalItem.getEmitterTier(stack).level == tier.level - 1) {
                    foundTerminal = true;
                } else {
                    return false;
                }
            } else if (stack.getItem() == emitterItem) {
                foundEmitter = true;
            } else {
                return false;
            }
        }

        return foundTerminal && foundEmitter;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        // Buscar el terminal y copiar su NBT completo (preserva linkedVault)
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() instanceof WirelessTerminalItem) {
                ItemStack result = stack.copy();
                WirelessTerminalItem.setEmitterTier(result, tier);
                result.setCount(1);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        ItemStack result = new ItemStack(VaultItems.WIRELESS_VAULT_TERMINAL.get());
        WirelessTerminalItem.setEmitterTier(result, tier);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return VaultRecipes.EMITTER_UPGRADE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<EmitterUpgradeRecipe> {

        @Override
        public EmitterUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            int tierLevel = json.get("tier").getAsInt();
            EmitterTier tier = EmitterTier.fromLevel(tierLevel);
            ResourceLocation itemId = new ResourceLocation(json.get("emitter").getAsString());
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return new EmitterUpgradeRecipe(id, tier, item);
        }

        @Override
        public @Nullable EmitterUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            EmitterTier tier = EmitterTier.fromLevel(buf.readInt());
            ResourceLocation itemId = buf.readResourceLocation();
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return new EmitterUpgradeRecipe(id, tier, item);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, EmitterUpgradeRecipe recipe) {
            buf.writeInt(recipe.tier.level);
            buf.writeResourceLocation(ForgeRegistries.ITEMS.getKey(recipe.emitterItem));
        }
    }
}
