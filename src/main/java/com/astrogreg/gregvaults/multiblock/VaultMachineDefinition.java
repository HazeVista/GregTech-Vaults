package com.astrogreg.gregvaults.multiblock;

import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.multiblock.VaultMachine.VaultTier;
import com.astrogreg.gregvaults.registry.VaultBlocks;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockShapeInfo;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

@SuppressWarnings("all")
public class VaultMachineDefinition {

    public static MultiblockMachineDefinition BRONZE_VAULT;
    public static MultiblockMachineDefinition STEEL_VAULT;
    public static MultiblockMachineDefinition TITANIUM_VAULT;

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
            case BRONZE -> "bronze_vault";
            case STEEL -> "steel_vault";
            case TITANIUM -> "titanium_vault";
        };
        
        ResourceLocation casingTexture = switch (tier) {
            case BRONZE -> new ResourceLocation(
                "gtceu",
                "block/casings/solid/machine_casing_bronze_plated_bricks"
            );
            case STEEL -> new ResourceLocation(
                "gtceu",
                "block/casings/solid/machine_casing_solid_steel"
            );
            case TITANIUM -> new ResourceLocation(
                "gtceu",
                "block/casings/solid/machine_casing_stable_titanium"
            );
        };

        return GregTechVaults.REGISTRATE.multiblock(name, holder ->
            new VaultMachine(holder, tier)
        )
            .rotationState(RotationState.NON_Y_AXIS)
            .appearanceBlock(casingBlock)
            .workableCasingModel(
                casingTexture,
                new ResourceLocation(
                    GregTechVaults.MOD_ID,
                    "block/multiblock/vault_controller"
                )
            )
            .pattern(definition ->
                FactoryBlockPattern.start(RIGHT, UP, BACK)
                    .aisle("WWWWW", "WWWWW", "WWCWW", "WWWWW", "WWWWW")
                    .aisle("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                    .aisle("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                    .aisle("WWWWW", "WVVVW", "WVVVW", "WVVVW", "WWWWW")
                    .aisle("WWWWW", "WWWWW", "WWWWW", "WWWWW", "WWWWW")
                    .where(
                        'C',
                        Predicates.controller(
                            Predicates.blocks(definition.getBlock())
                        )
                    )
                    .where(
                        'W',
                        Predicates.blocks(casingBlock.get()).or(
                            Predicates.abilities(PartAbility.IMPORT_ITEMS)
                                .setMaxGlobalLimited(1)
                                .setPreviewCount(1)
                        )
                    )
                    .where(
                        'V',
                        Predicates.blocks(getAllowedCores(tier)).or(
                            Predicates.air()
                        )
                    )
                    .build()
            )
            .shapeInfo(definition ->
                MultiblockShapeInfo.builder()
                
                    .aisle("WWWWW", "WWWWW", "WWCWW", "WWWWW", "WWWWW")
                    .aisle("WWWWW", "W   W", "W   W", "W   W", "WWWWW")
                    .aisle("WWWWW", "W   W", "W   W", "W   W", "WWWWW")
                    .aisle("WWWWW", "W   W", "W   W", "W   W", "WWWWW")
                    .aisle("WWWWW", "WWWWW", "WWWWW", "WWWWW", "WWWWW")
                    .where('C', definition, Direction.NORTH)
                    .where('W', casingBlock.getDefaultState())
                    .where(' ', Blocks.AIR.defaultBlockState())
                    .build()
            )
            .register();
    }
}
