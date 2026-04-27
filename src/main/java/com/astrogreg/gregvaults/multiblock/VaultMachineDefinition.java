package com.astrogreg.gregvaults.multiblock;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.client.model.machine.overlays.WorkableOverlays;
import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.model.generators.BlockModelBuilder;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.client.VaultOverlayRender;
import com.astrogreg.gregvaults.multiblock.VaultMachine.VaultTier;
import com.astrogreg.gregvaults.registry.VaultBlocks;

import static com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties.*;
import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;
import static com.gregtechceu.gtceu.common.data.models.GTMachineModels.*;

@SuppressWarnings("all")
public class VaultMachineDefinition {

    public static MultiblockMachineDefinition BRONZE_VAULT;
    public static MultiblockMachineDefinition STEEL_VAULT;
    public static MultiblockMachineDefinition TITANIUM_VAULT;

    private static final ResourceLocation OVERLAY_DIR = GregTechVaults.id(
            "block/multiblock");

    public static void init() {
        BRONZE_VAULT = registerVault(VaultTier.BRONZE);
        STEEL_VAULT = registerVault(VaultTier.STEEL);
        TITANIUM_VAULT = registerVault(VaultTier.TITANIUM);
    }

    private static Block[] getAllowedCores(VaultTier tier) {
        return switch (tier) {
            case BRONZE -> new Block[] { VaultBlocks.VAULT_CORE_MK1.get() };
            case STEEL -> new Block[] {
                    VaultBlocks.VAULT_CORE_MK1.get(),
                    VaultBlocks.VAULT_CORE_MK2.get(),
            };
            case TITANIUM -> new Block[] {
                    VaultBlocks.VAULT_CORE_MK1.get(),
                    VaultBlocks.VAULT_CORE_MK2.get(),
                    VaultBlocks.VAULT_CORE_MK3.get(),
            };
        };
    }

    private static MultiblockMachineDefinition registerVault(VaultTier tier) {
        var casingBlock = switch (tier) {
            case BRONZE -> GTBlocks.CASING_BRONZE_BRICKS;
            case STEEL -> GTBlocks.CASING_STEEL_SOLID;
            case TITANIUM -> GTBlocks.CASING_TITANIUM_STABLE;
        };

        String name = switch (tier) {
w            case BRONZE -> "large_bronze_vault";
            case STEEL -> "large_steel_vault";
            case TITANIUM -> "large_titanium_vault";
        };

        ResourceLocation casingTexture = switch (tier) {
            case BRONZE -> new ResourceLocation(
                    "gtceu",
                    "block/casings/solid/machine_casing_bronze_plated_bricks");
            case STEEL -> new ResourceLocation(
                    "gtceu",
                    "block/casings/solid/machine_casing_solid_steel");
            case TITANIUM -> new ResourceLocation(
                    "gtceu",
                    "block/casings/solid/machine_casing_stable_titanium");
        };

        return GregTechVaults.REGISTRATE.multiblock(name, holder -> new VaultMachine(holder, tier))
                .rotationState(RotationState.ALL)
                .appearanceBlock(casingBlock)
                .modelProperty(RECIPE_LOGIC_STATUS, RecipeLogic.Status.IDLE)
                .model((ctx, prov, builder) -> {
                    WorkableOverlays overlays = WorkableOverlays.get(
                            OVERLAY_DIR,
                            prov.getExistingFileHelper());
                    builder.forAllStates(state -> {
                        RecipeLogic.Status status = state.getValue(
                                RECIPE_LOGIC_STATUS);

                        BlockModelBuilder casingModel = prov
                                .models()
                                .nested()
                                .parent(
                                        prov
                                                .models()
                                                .getExistingFile(CUBE_ALL_SIDED_OVERLAY_MODEL))
                                .texture("all", casingTexture);

                        return addWorkableOverlays(overlays, status, casingModel);
                    });

                    builder.addTextureOverride("all", casingTexture);
                    builder.addDynamicRenderer(() -> VaultOverlayRender.INSTANCE);
                })

                .pattern(definition -> FactoryBlockPattern.start(RIGHT, UP, BACK)
                        .aisle("WWWWW", "WWWWW", "WWCWW", "WWWWW", "WWWWW")
                        .aisle("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                        .aisle("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                        .aisle("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                        .aisle("WWWWW", "WWWWW", "WWWWW", "WWWWW", "WWWWW")
                        .where(
                                'C',
                                Predicates.controller(
                                        Predicates.blocks(definition.getBlock())))
                        .where(
                                'W',
                                Predicates.blocks(casingBlock.get()).or(
                                        Predicates.abilities(PartAbility.IMPORT_ITEMS)
                                                .setMaxGlobalLimited(1)
                                                .setPreviewCount(1)))
                        .where(
                                'V',
                                Predicates.blocks(getAllowedCores(tier)).or(
                                        Predicates.air()))
                        .build())
                .shapeInfo(definition -> MultiblockShapeInfo.builder()
                        .aisle("WWWWW", "WWWWW", "WWCWW", "WWWWW", "WWWWW")
                        .aisle("WWWWW", "W   W", "W   W", "W   W", "WWWWW")
                        .aisle("WWWWW", "W   W", "W   W", "W   W", "WWWWW")
                        .aisle("WWWWW", "W   W", "W   W", "W   W", "WWWWW")
                        .aisle("WWWWW", "WWWWW", "WWWWW", "WWWWW", "WWWWW")
                        .where('C', definition, Direction.NORTH)
                        .where('W', casingBlock.getDefaultState())
                        .where(' ', Blocks.AIR.defaultBlockState())
                        .build())
                .register();
    }
}
