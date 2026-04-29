package com.astrogreg.gregvaults.blocks;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.astrogreg.gregvaults.config.VaultConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level,
                                List<Component> tooltip, TooltipFlag flag) {
        int slots = VaultConfig.getSlotValue(this.tier);
        tooltip.add(Component.translatable(
                "tooltip.gregtechvaults.vault_core_" + tier.name().toLowerCase(),
                slots));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public VaultCoreBlock(CoreTier tier, BlockBehaviour.Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CoreTier getTier() {
        return tier;
    }
}
