package com.astrogreg.gregvaults.recipe;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.astrogreg.gregvaults.GregTechVaults;

public class VaultRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister
            .create(ForgeRegistries.RECIPE_SERIALIZERS, GregTechVaults.MOD_ID);

    public static final RegistryObject<RecipeSerializer<EmitterUpgradeRecipe>> EMITTER_UPGRADE_SERIALIZER = SERIALIZERS
            .register("emitter_upgrade", EmitterUpgradeRecipe.Serializer::new);

    public static void init() {}
}
