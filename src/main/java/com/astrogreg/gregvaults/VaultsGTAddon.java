package com.astrogreg.gregvaults;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.data.recipes.FinishedRecipe;

import com.astrogreg.gregvaults.recipe.GTVaultsRecipes;

import java.util.function.Consumer;

@SuppressWarnings("unused")
@GTAddon
public class VaultsGTAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return GregTechVaults.REGISTRATE;
    }

    @Override
    public void initializeAddon() {}

    @Override
    public String addonModId() {
        return GregTechVaults.MOD_ID;
    }

    @Override
    public void registerTagPrefixes() {
        // CustomTagPrefixes.init();
    }

    @Override
    public void addRecipes(Consumer<FinishedRecipe> provider) {
        GTVaultsRecipes.init(provider);
    }

    @Override
    public void registerElements() {
        // CustomElements.init();
    }

    // If you have custom ingredient types, uncomment this & change to match your capability.
    // KubeJS WILL REMOVE YOUR RECIPES IF THESE ARE NOT REGISTERED.
    /*
     * public static final ContentJS<Double> PRESSURE_IN = new ContentJS<>(NumberComponent.ANY_DOUBLE,
     * CustomRecipeCapabilities.PRESSURE, false);
     * public static final ContentJS<Double> PRESSURE_OUT = new ContentJS<>(NumberComponent.ANY_DOUBLE,
     * CustomRecipeCapabilities.PRESSURE, true);
     * 
     * @Override
     * public void registerRecipeKeys(KJSRecipeKeyEvent event) {
     * event.registerKey(CustomRecipeCapabilities.PRESSURE, Pair.of(PRESSURE_IN, PRESSURE_OUT));
     * }
     */
}
