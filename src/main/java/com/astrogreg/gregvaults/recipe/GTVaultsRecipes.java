package com.astrogreg.gregvaults.recipe;

import com.astrogreg.gregvaults.registry.VaultBlocks;
import com.astrogreg.gregvaults.registry.VaultItems;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import net.minecraft.data.recipes.FinishedRecipe;

import com.astrogreg.gregvaults.GregTechVaults;

import java.util.function.Consumer;

import static com.astrogreg.gregvaults.multiblock.VaultMachineDefinition.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class GTVaultsRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {
        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("large_bronze_vault"),
                BRONZE_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Bronze),
                'B', CustomTags.ULV_CIRCUITS, 'C', new MaterialEntry(rod, Iron), 'D', GTMachines.BRONZE_CRATE.asStack(),
                'E', new MaterialEntry(plateDouble, Iron));

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("large_steel_vault"),
                STEEL_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Steel),
                'B', CustomTags.LV_CIRCUITS, 'C', new MaterialEntry(rod, Iron), 'D', GTMachines.STEEL_CRATE.asStack(),
                'E', new MaterialEntry(plateDouble, Iron));

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("large_titanium_vault"),
                TITANIUM_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Titanium),
                'B', CustomTags.HV_CIRCUITS, 'C', new MaterialEntry(rod, StainlessSteel), 'D',
                GTMachines.TITANIUM_CRATE.asStack(), 'E', new MaterialEntry(plateDouble, StainlessSteel));

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("mk1_core"),
                VaultBlocks.VAULT_CORE_MK1.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Bronze),
                'B', new MaterialEntry(rodLong, Bronze), 'C', GTMachines.BRONZE_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("mk2_core"),
                VaultBlocks.VAULT_CORE_MK2.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Steel),
                'B', new MaterialEntry(rodLong, Steel), 'C', GTMachines.STEEL_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("mk3_core"),
                VaultBlocks.VAULT_CORE_MK3.asStack(), "AhA", "BCB", "AwA", 'A', new MaterialEntry(plate, Titanium),
                'B', new MaterialEntry(rodLong, Titanium), 'C', GTMachines.TITANIUM_CRATE.asStack());

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("wireless_vault_terminal"),
                VaultItems.WIRELESS_VAULT_TERMINAL.asStack(), "AAA", "CBD", "EdE", 'A',
                new MaterialEntry(plate, Steel), 'B', new MaterialEntry(plate, Glass), 'C', GTItems.EMITTER_LV,
                'D', GTItems.SENSOR_LV, 'E', new MaterialEntry(screw, Steel));
    }
}
