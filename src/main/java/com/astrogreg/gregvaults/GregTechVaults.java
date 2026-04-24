package com.astrogreg.gregvaults;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialRegistryEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.event.PostMaterialEvent;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.sound.SoundEntry;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderManager;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import com.astrogreg.gregvaults.client.VaultOverlayRender;
import com.astrogreg.gregvaults.config.VaultConfig;
import com.astrogreg.gregvaults.datagen.VaultDatagen;
import com.astrogreg.gregvaults.multiblock.VaultMachineDefinition;
import com.astrogreg.gregvaults.network.VaultNetwork;
import com.astrogreg.gregvaults.registry.VaultBlocks;
import com.astrogreg.gregvaults.registry.VaultMenuTypes;
import com.astrogreg.gregvaults.screen.VaultScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(GregTechVaults.MOD_ID)
@SuppressWarnings("removal")
public class GregTechVaults {

    public static final String MOD_ID = "gregtechvaults";
    public static final Logger LOGGER = LogManager.getLogger();
    public static GTRegistrate REGISTRATE = GTRegistrate.create(
            GregTechVaults.MOD_ID);

    public GregTechVaults() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        modEventBus.addGenericListener(
                GTRecipeType.class,
                this::registerRecipeTypes);
        modEventBus.addGenericListener(
                MachineDefinition.class,
                this::registerMachines);
        modEventBus.addGenericListener(SoundEntry.class, this::registerSounds);

        modEventBus.addListener(this::addMaterialRegistries);
        modEventBus.addListener(this::addMaterials);
        modEventBus.addListener(this::modifyMaterials);

        VaultMenuTypes.MENU_TYPES.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        REGISTRATE.registerRegistrate();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            DynamicRenderManager.register(
                    GregTechVaults.id("vault_overlay"),
                    VaultOverlayRender.TYPE);
            modEventBus.addListener(VaultOverlayRender::registerModel);
        }

        VaultConfig.init();
        VaultDatagen.init();
        VaultBlocks.init();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(VaultNetwork::init);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(
                    VaultMenuTypes.VAULT_MENU.get(),
                    VaultScreen::new);
        });
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    private void addMaterialRegistries(MaterialRegistryEvent event) {
        GTCEuAPI.materialManager.createRegistry(GregTechVaults.MOD_ID);
    }

    private void addMaterials(MaterialEvent event) {}

    private void modifyMaterials(PostMaterialEvent event) {}

    private void registerRecipeTypes(
                                     GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) {}

    private void registerMachines(
                                  GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        VaultMachineDefinition.init();
    }

    public void registerSounds(
                               GTCEuAPI.RegisterEvent<ResourceLocation, SoundEntry> event) {}
}
