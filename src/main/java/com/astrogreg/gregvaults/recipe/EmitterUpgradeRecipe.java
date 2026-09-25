package com.astrogreg.gregvaults.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import com.astrogreg.gregvaults.items.WirelessTerminalItem;
import com.astrogreg.gregvaults.items.WirelessTerminalItem.EmitterTier;
import com.astrogreg.gregvaults.registry.VaultRegistry;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class EmitterUpgradeRecipe extends CustomRecipe {

    private final EmitterTier tier;
    private final Item emitterItem;

    public EmitterUpgradeRecipe(ResourceLocation id, EmitterTier tier,
                                Item emitterItem) {
        super(id, CraftingBookCategory.MISC);
        this.tier = tier;
        this.emitterItem = emitterItem;
    }

    @Override
    public boolean matches(CraftingContainer inv, @NotNull Level level) {
        int terminalCount = 0;
        int emitterCount = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof WirelessTerminalItem) {
                terminalCount++;
                if (terminalCount > 1 || WirelessTerminalItem.getEmitterTier(stack).level != tier.level - 1) {
                    return false;
                }
            } else if (stack.getItem() == emitterItem) {
                emitterCount++;
                if (emitterCount > 1) return false;
            } else {
                return false;
            }
        }

        return terminalCount == 1 && emitterCount == 1;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer inv, @NotNull RegistryAccess registryAccess) {
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
    public @NotNull ItemStack getResultItem(@NotNull RegistryAccess registryAccess) {
        ItemStack result = new ItemStack(VaultRegistry.WIRELESS_VAULT_TERMINAL.get());
        WirelessTerminalItem.setEmitterTier(result, tier);
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return w * h >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return VaultRecipes.EMITTER_UPGRADE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<EmitterUpgradeRecipe> {

        @Override
        public @NotNull EmitterUpgradeRecipe fromJson(@NotNull ResourceLocation id, JsonObject json) {
            int tierLevel = json.get("tier").getAsInt();
            EmitterTier tier = EmitterTier.fromLevel(tierLevel);
            ResourceLocation itemId = new ResourceLocation(json.get("emitter").getAsString());
            net.minecraft.world.item.Item item = Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(itemId));
            return new EmitterUpgradeRecipe(id, tier, item);
        }

        @Override
        public @Nullable EmitterUpgradeRecipe fromNetwork(@NotNull ResourceLocation id, FriendlyByteBuf buf) {
            EmitterTier tier = EmitterTier.fromLevel(buf.readInt());
            ResourceLocation itemId = buf.readResourceLocation();
            net.minecraft.world.item.Item item = ForgeRegistries.ITEMS.getValue(itemId);
            return new EmitterUpgradeRecipe(id, tier, item);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, EmitterUpgradeRecipe recipe) {
            buf.writeInt(recipe.tier.level);
            buf.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(recipe.emitterItem)));
        }
    }
}
