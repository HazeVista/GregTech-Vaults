package com.astrogreg.gregvaults.mixin;

import com.astrogreg.gregvaults.screen.AggregatedStack;
import com.astrogreg.gregvaults.screen.AbstractVaultScreen;
import com.astrogreg.gregvaults.screen.VaultSlot;
import com.astrogreg.gregvaults.network.CPacketStackedPickup;
import com.astrogreg.gregvaults.network.VaultNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@Mixin(AbstractContainerScreen.class)
public abstract class MixinAbstractContainerScreen<T extends AbstractContainerMenu> {

    @Shadow protected Slot hoveredSlot;

    private AbstractVaultScreen<?> asVaultScreen() {
        Object self = this;
        return self instanceof AbstractVaultScreen<?> v ? v : null;
    }

    // Draw stacked count label after vanilla draws the item
    @Inject(method = "renderSlot", at = @At("TAIL"))
    private void gregvaults$renderStackedCount(GuiGraphics g, Slot slot, CallbackInfo ci) {
        AbstractVaultScreen<?> screen = asVaultScreen();
        if (screen == null || !screen.isStackedMode()) return;
        if (!(slot instanceof VaultSlot vs) || !slot.isActive() || !vs.isAggregated()) return;

        AggregatedStack agg = vs.getAggregatedStack();
        if (agg == null || agg.displayStack.isEmpty() || agg.totalCount() <= 1) return;

        screen.renderStackedCountLabel(g, slot, agg.totalCount());
    }

    // Intercept pickup clicks on stacked slots it's send custom packet instead of vanilla
    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void gregvaults$handleStackedPickup(Slot slot, int slotIdx, int mouseButton,
                                                ClickType clickType, CallbackInfo ci) {
        if (clickType != ClickType.PICKUP) return;
        AbstractVaultScreen<?> screen = asVaultScreen();
        if (screen == null || !screen.isStackedMode()) return;
        if (!(slot instanceof VaultSlot vs) || !vs.isAggregated()) return;
        if (slot.getItem().isEmpty()) return;

        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        net.minecraft.world.item.ItemStack carried = self.getMenu().getCarried();

        if (!carried.isEmpty() && !net.minecraft.world.item.ItemStack.isSameItemSameTags(carried, slot.getItem()))
            return;

        int slotIndex = screen.getSlotIndex(slot);
        boolean half = mouseButton == 1;

        AggregatedStack agg = vs.getAggregatedStack();
        screen.applyOptimisticPickup(agg, half);

        VaultNetwork.CHANNEL.sendToServer(new CPacketStackedPickup(slotIndex, half));
        ci.cancel();
    }

    // Show real total count in tooltip
    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            at = @At("HEAD"), cancellable = true)
    private void gregvaults$renderStackedTooltip(GuiGraphics g, int x, int y, CallbackInfo ci) {
        AbstractVaultScreen<?> screen = asVaultScreen();
        if (screen == null) return;
        if (!(hoveredSlot instanceof VaultSlot vs) || !vs.isAggregated()) return;

        AggregatedStack agg = vs.getAggregatedStack();
        if (agg == null || agg.displayStack.isEmpty()) return;

        if (screen.renderStackedTooltip(g, agg, x, y)) ci.cancel();
    }
}