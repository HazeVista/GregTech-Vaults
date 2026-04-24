package com.astrogreg.gregvaults.registry;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.blocks.VaultCoreBlock;
import com.astrogreg.gregvaults.blocks.VaultCoreBlock.CoreTier;
import com.tterrag.registrate.util.entry.BlockEntry;

@SuppressWarnings("all")
public class VaultBlocks {

    public static final BlockEntry<VaultCoreBlock> VAULT_CORE_MK1 = GregTechVaults.REGISTRATE
            .block("vault_core_mk1", props -> new VaultCoreBlock(CoreTier.MK1, props))
            .properties(p -> p.strength(3f).sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> {})
            .simpleItem()
            .lang("Vault Core MK1")
            .register();

    public static final BlockEntry<VaultCoreBlock> VAULT_CORE_MK2 = GregTechVaults.REGISTRATE
            .block("vault_core_mk2", props -> new VaultCoreBlock(CoreTier.MK2, props))
            .properties(p -> p.strength(3f).sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> {})
            .simpleItem()
            .lang("Vault Core MK2")
            .register();

    public static final BlockEntry<VaultCoreBlock> VAULT_CORE_MK3 = GregTechVaults.REGISTRATE
            .block("vault_core_mk3", props -> new VaultCoreBlock(CoreTier.MK3, props))
            .properties(p -> p.strength(3f).sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> {})
            .simpleItem()
            .lang("Vault Core MK3")
            .register();

    public static void init() {}
}
