package com.astrogreg.gregvaults.config;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.blocks.VaultCoreBlock.CoreTier;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.ConfigHolder;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

@SuppressWarnings("all")
@Config(id = GregTechVaults.MOD_ID)
public class VaultConfig {

    public static VaultConfig INSTANCE;
    public static ConfigHolder<VaultConfig> CONFIG_HOLDER;

    public static void init() {
        CONFIG_HOLDER = Configuration.registerConfig(VaultConfig.class, ConfigFormats.yaml());
        INSTANCE = CONFIG_HOLDER.getConfigInstance();
    }

    @Configurable
    public CoreValues coreValues = new CoreValues();

    @Configurable
    public VaultValues vaultValues = new VaultValues();

    public static class CoreValues {

        @Configurable
        public int mk1SlotValue = 100;

        @Configurable
        public int mk2SlotValue = 200;

        @Configurable
        public int mk3SlotValue = 500;
    }

    public static class VaultValues {

        @Configurable
        public BronzeVault bronzeVault = new BronzeVault();

        @Configurable
        public SteelVault steelVault = new SteelVault();

        @Configurable
        public TitaniumVault titaniumVault = new TitaniumVault();

        public static class BronzeVault {

            @Configurable
            public int maxLayers = 27;
        }

        public static class SteelVault {

            @Configurable
            public int maxLayers = 27;
        }

        public static class TitaniumVault {

            @Configurable
            public int maxLayers = 27;
        }
    }

    public static int getSlotValue(CoreTier tier) {
        return switch (tier) {
            case MK1 -> INSTANCE.coreValues.mk1SlotValue;
            case MK2 -> INSTANCE.coreValues.mk2SlotValue;
            case MK3 -> INSTANCE.coreValues.mk3SlotValue;
        };
    }
}
