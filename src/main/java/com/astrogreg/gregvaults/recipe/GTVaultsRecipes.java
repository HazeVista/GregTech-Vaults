package com.astrogreg.gregvaults.recipe;

import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.items.WirelessTerminalItem.EmitterTier;
import com.astrogreg.gregvaults.registry.VaultBlocks;
import com.astrogreg.gregvaults.registry.VaultItems;
import com.astrogreg.gregvaults.registry.VaultMachines;
import com.google.gson.JsonObject;

import java.util.function.Consumer;

import static com.astrogreg.gregvaults.multiblock.VaultMachineDefinition.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

@SuppressWarnings("all")
public class GTVaultsRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, true, GregTechVaults.id("large_bronze_vault"),
                BRONZE_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Bronze),
                'B', CustomTags.ULV_CIRCUITS, 'C', new MaterialEntry(rod, Iron), 'D', GTMachines.BRONZE_CRATE.asStack(),
                'E', new MaterialEntry(plateDouble, Iron));

        VanillaRecipeHelper.addShapedRecipe(provider, true, GregTechVaults.id("large_steel_vault"),
                STEEL_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Steel),
                'B', CustomTags.LV_CIRCUITS, 'C', new MaterialEntry(rod, Iron), 'D', GTMachines.STEEL_CRATE.asStack(),
                'E', new MaterialEntry(plateDouble, Iron));

        VanillaRecipeHelper.addShapedRecipe(provider, true, GregTechVaults.id("large_titanium_vault"),
                TITANIUM_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Titanium),
                'B', CustomTags.HV_CIRCUITS, 'C', new MaterialEntry(rod, StainlessSteel), 'D',
                GTMachines.TITANIUM_CRATE.asStack(), 'E', new MaterialEntry(plateDouble, StainlessSteel));

        VanillaRecipeHelper.addShapedRecipe(provider, true, GregTechVaults.id("mk1_core"),
                VaultBlocks.VAULT_CORE_MK1.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Bronze),
                'B', new MaterialEntry(rodLong, Bronze), 'C', GTMachines.BRONZE_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, GregTechVaults.id("mk2_core"),
                VaultBlocks.VAULT_CORE_MK2.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Steel),
                'B', new MaterialEntry(rodLong, Steel), 'C', GTMachines.STEEL_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, GregTechVaults.id("mk3_core"),
                VaultBlocks.VAULT_CORE_MK3.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Titanium),
                'B', new MaterialEntry(rodLong, Titanium), 'C', GTMachines.TITANIUM_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, true, GregTechVaults.id("wireless_vault_terminal"),
                VaultItems.WIRELESS_VAULT_TERMINAL.asStack(), "AAA", "CBD", "EdE", 'A',
                new MaterialEntry(plate, Steel), 'B', new MaterialEntry(plate, Glass), 'C', GTItems.EMITTER_LV,
                'D', GTItems.SENSOR_LV, 'E', new MaterialEntry(screw, Steel));

        VanillaRecipeHelper.addShapedRecipe(provider, true, GregTechVaults.id("vault_interface"),
                VaultMachines.VAULT_INTERFACE.asStack(), "w", "B", "A", 'A', GTBlocks.BRONZE_HULL,
                'B', new MaterialEntry(pipeNormalFluid, Bronze));

        addEmitterUpgrade(provider, EmitterTier.LV, GTItems.EMITTER_LV.get());
        addEmitterUpgrade(provider, EmitterTier.MV, GTItems.EMITTER_MV.get());
        addEmitterUpgrade(provider, EmitterTier.HV, GTItems.EMITTER_HV.get());
        addEmitterUpgrade(provider, EmitterTier.EV, GTItems.EMITTER_EV.get());
        addEmitterUpgrade(provider, EmitterTier.IV, GTItems.EMITTER_IV.get());
        addEmitterUpgrade(provider, EmitterTier.LUV, GTItems.EMITTER_LuV.get());
        addEmitterUpgrade(provider, EmitterTier.ZPM, GTItems.EMITTER_ZPM.get());
        addEmitterUpgrade(provider, EmitterTier.UV, GTItems.EMITTER_UV.get());
    }

    private static void addEmitterUpgrade(Consumer<FinishedRecipe> provider,
                                          EmitterTier tier, net.minecraft.world.item.Item emitterItem) {
        ResourceLocation recipeId = GregTechVaults.id("terminal_emitter_" + tier.name().toLowerCase());
        ResourceLocation emitterId = ForgeRegistries.ITEMS.getKey(emitterItem);

        provider.accept(new FinishedRecipe() {

            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("tier", tier.level);
                json.addProperty("emitter", emitterId.toString());
            }

            @Override
            public ResourceLocation getId() {
                return recipeId;
            }

            @Override
            public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
                return VaultRecipes.EMITTER_UPGRADE_SERIALIZER.get();
            }

            @Override
            public @org.jetbrains.annotations.Nullable JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public @org.jetbrains.annotations.Nullable ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }
}
