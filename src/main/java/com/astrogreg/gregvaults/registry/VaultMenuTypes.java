package com.astrogreg.gregvaults.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.screen.VaultContainerMenu;
import com.astrogreg.gregvaults.screen.VaultTerminalMenu;

public class VaultMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(
            ForgeRegistries.MENU_TYPES,
            GregTechVaults.MOD_ID);

    public static final RegistryObject<MenuType<VaultContainerMenu>> VAULT_MENU = MENU_TYPES.register("vault_menu",
            () -> IForgeMenuType.create(VaultContainerMenu::new));

    public static final RegistryObject<MenuType<VaultTerminalMenu>> VAULT_TERMINAL_MENU = MENU_TYPES.register(
            "vault_terminal_menu",
            () -> IForgeMenuType.create(VaultTerminalMenu::new));

    public static void init() {}
}
