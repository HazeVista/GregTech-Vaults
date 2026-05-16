// package com.astrogreg.gregvaults.mixin;
//
// import com.astrogreg.gregvaults.screen.VaultSlot;
// import net.minecraft.client.gui.GuiGraphics;
// import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
// import net.minecraft.world.inventory.Slot;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
// import net.minecraftforge.api.distmarker.Dist;
// import net.minecraftforge.api.distmarker.OnlyIn;
//
// @OnlyIn(Dist.CLIENT)
// @Mixin(AbstractContainerScreen.class)
// public class MixinAbstractContainerScreen {
//
// @Inject(
// method = "renderSlot",
// at = @At(
// value = "INVOKE",
// target =
// "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"
// ),
// cancellable = true)
// private void gregvaults$suppressVaultSlotDecorations(GuiGraphics guiGraphics, Slot slot,
// CallbackInfo ci) {
// if (slot instanceof VaultSlot vaultSlot && vaultSlot.isAggregated()) {
// ci.cancel();
// }
// }
// }
