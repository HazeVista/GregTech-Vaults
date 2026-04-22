package com.astrogreg.gregvaults.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class VaultCoreBlock extends Block {

    public enum CoreTier {
        MK1(1),
        MK2(2),
        MK3(3);

        public final int level;

        CoreTier(int level) {
            this.level = level;
        }
    }

    private final CoreTier tier;

    public VaultCoreBlock(CoreTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CoreTier getTier() {
        return tier;
    }


    public int getSlotValue() {
        return 0;
    }
}