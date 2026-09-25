package com.astrogreg.gregvaults.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

import com.astrogreg.gregvaults.GregTechVaults;
import com.tterrag.registrate.util.entry.RegistryEntry;

public class VaultRecipes {

    public static final RegistryEntry<RecipeSerializer<EmitterUpgradeRecipe>> EMITTER_UPGRADE_SERIALIZER = GregTechVaults.REGISTRATE
            .simple("emitter_upgrade", Registries.RECIPE_SERIALIZER,
                    EmitterUpgradeRecipe.Serializer::new);

    public static void init() {}
}
