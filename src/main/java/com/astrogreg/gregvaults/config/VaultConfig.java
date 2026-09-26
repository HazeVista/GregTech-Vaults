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

    @Configurable
    public WirelessTerminal wirelessTerminal = new WirelessTerminal();

    public static class CoreValues {

        @Configurable
        @Configurable.Range(min = 0, max = 10000)
        @Configurable.Comment({ "The number of item slots added by the Vault Core MK I", "Default: 100" })
        public int mk1SlotValue = 100;

        @Configurable
        @Configurable.Range(min = 0, max = 10000)
        @Configurable.Comment({ "The number of item slots added by the Vault Core MK II", "Default: 200" })
        public int mk2SlotValue = 200;

        @Configurable
        @Configurable.Range(min = 0, max = 10000)
        @Configurable.Comment({ "The number of item slots added by the Vault Core MK III", "Default: 500" })
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
            @Configurable.Range(min = 0, max = 100000)
            @Configurable.Comment({ "Base number of item slots for the Large Bronze Vault", "Default: 36" })
            public int bronzeBaseSlots = 36;

            @Configurable
            @Configurable.Range(min = 0, max = 25)
            @Configurable.Comment({ "Maximum number of interfaces for the Large Bronze Vault", "Default: 2" })
            public int bronzeInterfaceLimit = 2;

            @Configurable
            @Configurable.Comment({ "Whether wireless terminals can connect to the Large  Vault", "Default: true" })
            public boolean bronzeWireless = true;
        }

        public static class SteelVault {

            @Configurable
            @Configurable.Range(min = 0, max = 100000)
            @Configurable.Comment({ "Base number of item slots for the Large Steel Vault", "Default: 72" })
            public int steelBaseSlots = 72;

            @Configurable
            @Configurable.Range(min = 0, max = 25)
            @Configurable.Comment({ "Maximum number of interfaces for the Large  Vault", "Default: 4" })
            public int steelInterfaceLimit = 4;

            @Configurable
            @Configurable.Comment({ "Whether wireless terminals can connect to the Large Steel Vault",
                    "Default: true" })
            public boolean steelWireless = true;
        }

        public static class TitaniumVault {

            @Configurable
            @Configurable.Range(min = 0, max = 100000)
            @Configurable.Comment({ "Base number of item slots for the Large Titanium Vault", "Default: 108" })
            public int titaniumBaseSlots = 108;

            @Configurable
            @Configurable.Range(min = 0, max = 25)
            @Configurable.Comment({ "Maximum number of interfaces for the Large Titanium Vault", "Default: 8" })
            public int titaniumInterfaceLimit = 8;

            @Configurable
            @Configurable.Comment({ "Whether wireless terminals can connect to the Large Titanium Vault",
                    "Default: true" })
            public boolean titaniumWireless = true;
        }
    }

    public static class WirelessTerminal {

        @Configurable
        @Configurable.Range(min = 0, max = 1000000)
        @Configurable.Comment({ "Connection distance in blocks for the base LV wireless terminal",
                "Default: 64" })
        public int connectionDistance = 64;

        @Configurable
        @Configurable.Comment({
                "Whether infinite range is enabled for the wireless terminal, also enables cross-dimension connection",
                "If true, connectionDistance will be ignored entirely", "Default: false" })
        public boolean infiniteRange = false;

        @Configurable
        @Configurable.DecimalRange(min = 0.0, max = 1024.0)
        @Configurable.Comment({ "The range multiplier applied by the MV emitter", "Default: 2.0" })
        public double mvEmitterBonus = 2.0;

        @Configurable
        @Configurable.DecimalRange(min = 0.0, max = 1024.0)
        @Configurable.Comment({ "The range multiplier applied by the HV emitter", "Default: 3.0" })
        public double hvEmitterBonus = 3.0;

        @Configurable
        @Configurable.DecimalRange(min = 0.0, max = 1024.0)
        @Configurable.Comment({ "The range multiplier applied by the EV emitter", "Default: 4.0" })
        public double evEmitterBonus = 4.0;

        @Configurable
        @Configurable.DecimalRange(min = 0.0, max = 1024.0)
        @Configurable.Comment({ "The range multiplier applied by the IV emitter", "Default: 5.0" })
        public double ivEmitterBonus = 5.0;

        @Configurable
        @Configurable.DecimalRange(min = 0.0, max = 1024.0)
        @Configurable.Comment({ "The range multiplier applied by the LuV emitter", "Default: 6.0" })
        public double luvEmitterBonus = 6.0;

        @Configurable
        @Configurable.DecimalRange(min = 0.0, max = 1024.0)
        @Configurable.Comment({ "The range multiplier applied by the ZPM emitter", "Default: 7.0" })
        public double zpmEmitterBonus = 7.0;

        @Configurable
        @Configurable.DecimalRange(min = 0.0, max = 1024.0)
        @Configurable.Comment({ "The range multiplier applied by the UV emitter", "Default: 8.0" })
        public double uvEmitterBonus = 8.0;
    }

    public static int getSlotValue(CoreTier tier) {
        return switch (tier) {
            case MK1 -> INSTANCE.coreValues.mk1SlotValue;
            case MK2 -> INSTANCE.coreValues.mk2SlotValue;
            case MK3 -> INSTANCE.coreValues.mk3SlotValue;
        };
    }
}
