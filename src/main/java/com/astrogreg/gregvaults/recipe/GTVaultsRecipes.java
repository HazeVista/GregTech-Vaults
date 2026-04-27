package com.astrogreg.gregvaults.recipe;

import com.astrogreg.gregvaults.GregTechVaults;
import com.gregtechceu.gtceu.api.data.chemical.material.stack.MaterialEntry;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

import static com.astrogreg.gregvaults.multiblock.VaultMachineDefinition.*;
import static com.gregtechceu.gtceu.api.data.tag.TagPrefix.*;
import static com.gregtechceu.gtceu.common.data.GTMaterials.*;

public class GTVaultsRecipes {

    public static void init(Consumer<FinishedRecipe> provider) {

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("large_bronze_vault"),
                BRONZE_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Bronze),
                'B', CustomTags.ULV_CIRCUITS, 'C', new MaterialEntry(rod, Iron), 'D', GTMachines.BRONZE_CRATE,
                'E', new MaterialEntry(plateDouble, Iron));

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("large_steel_vault"),
                STEEL_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Steel),
                'B', CustomTags.LV_CIRCUITS, 'C', new MaterialEntry(rod, Iron), 'D', GTMachines.STEEL_CRATE,
                'E', new MaterialEntry(plateDouble, Iron));

        VanillaRecipeHelper.addShapedRecipe(provider, false, GregTechVaults.id("large_titanium_vault"),
                TITANIUM_VAULT.asStack(), "ABA", "CDC", "AEA", 'A', new MaterialEntry(plate, Titanium),
                'B', CustomTags.HV_CIRCUITS, 'C', new MaterialEntry(rod, StainlessSteel), 'D', GTMachines.TITANIUM_CRATE,
                'E', new MaterialEntry(plateDouble, StainlessSteel));
    }

}
