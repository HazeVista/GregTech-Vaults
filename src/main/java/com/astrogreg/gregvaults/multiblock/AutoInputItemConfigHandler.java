package com.astrogreg.gregvaults.multiblock;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.widget.directional.IDirectionalConfigHandler;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.utils.BlockPosFace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AutoInputItemConfigHandler implements IDirectionalConfigHandler {

    private static final IGuiTexture TEXTURE_OFF = new GuiTextureGroup(
            GuiTextures.VANILLA_BUTTON,
            GuiTextures.IO_CONFIG_ITEM_MODES_BUTTON.getSubTexture(0, 0, 1, 1 / 3f));
    private static final IGuiTexture TEXTURE_INPUT = new GuiTextureGroup(
            GuiTextures.VANILLA_BUTTON,
            GuiTextures.IO_CONFIG_ITEM_MODES_BUTTON.getSubTexture(0, 1 / 3f, 1, 1 / 3f));
    private static final IGuiTexture TEXTURE_OUTPUT = new GuiTextureGroup(
            GuiTextures.VANILLA_BUTTON,
            GuiTextures.BUTTON_ITEM_OUTPUT);
    private static final IGuiTexture TEXTURE_AUTO = new GuiTextureGroup(
            GuiTextures.VANILLA_BUTTON,
            GuiTextures.IO_CONFIG_ITEM_MODES_BUTTON.getSubTexture(0, 2 / 3f, 1, 1 / 3f));

    private final VaultInterfacePart machine;
    private Direction side;
    private ButtonWidget ioModeButton;
    private ButtonWidget autoTransferButton;

    public AutoInputItemConfigHandler(VaultInterfacePart machine) {
        this.machine = machine;
    }

    @Override
    public Widget getSideSelectorWidget(SceneWidget scene, FancyMachineUIWidget machineUI) {
        WidgetGroup group = new WidgetGroup(0, 0, 36, 18);

        group.addWidget(ioModeButton = new ButtonWidget(0, 0, 18, 18, this::onIOModePressed) {

            @Override
            public void updateScreen() {
                super.updateScreen();
                if (side == null || machine.getItemFacing() != side ||
                        machine.getItemIoMode() == VaultInterfacePart.ItemIoMode.DISABLED) {
                    setButtonTexture(TEXTURE_OFF);
                    setHoverTooltips(Component.literal("Vault Interface: Disabled"));
                } else if (machine.getItemIoMode() == VaultInterfacePart.ItemIoMode.INPUT) {
                    setButtonTexture(TEXTURE_INPUT);
                    setHoverTooltips(Component.literal("Vault Interface: Input"));
                } else {
                    setButtonTexture(TEXTURE_OUTPUT);
                    setHoverTooltips(Component.literal("Vault Interface: Output"));
                }
            }
        });

        group.addWidget(autoTransferButton = new ButtonWidget(18, 0, 18, 18, this::onAutoTransferPressed) {

            @Override
            public void updateScreen() {
                super.updateScreen();
                boolean selected = side != null && machine.getItemFacing() == side &&
                        machine.getItemIoMode() != VaultInterfacePart.ItemIoMode.DISABLED;
                if (!selected) {
                    setButtonTexture(TEXTURE_OFF);
                    setHoverTooltips(Component.literal("No Vault Interface side selected"));
                } else if (machine.isAutoTransferItems()) {
                    setButtonTexture(TEXTURE_AUTO);
                    setHoverTooltips(Component.literal("Auto Transfer: Enabled"));
                } else {
                    setButtonTexture(TEXTURE_INPUT);
                    setHoverTooltips(Component.literal("Auto Transfer: Disabled"));
                }
            }
        });

        return group;
    }

    private void onIOModePressed(ClickData cd) {
        if (side == null) return;
        machine.cycleItemMode(side);
    }

    private void onAutoTransferPressed(ClickData cd) {
        if (side == null) return;
        if (machine.getItemFacing() == side && machine.getItemIoMode() != VaultInterfacePart.ItemIoMode.DISABLED) {
            machine.setAutoTransferItems(!machine.isAutoTransferItems());
        }
    }

    @Override
    public void onSideSelected(BlockPos pos, Direction side) {
        this.side = side;
    }

    @Override
    public ScreenSide getScreenSide() {
        return ScreenSide.LEFT;
    }

    @Override
    public void handleClick(ClickData cd, Direction direction) {
        if (cd.button == 0) {
            machine.cycleItemMode(direction);
        } else if (cd.button == 1 && machine.getItemFacing() == direction &&
                machine.getItemIoMode() != VaultInterfacePart.ItemIoMode.DISABLED) {
                    machine.setAutoTransferItems(!machine.isAutoTransferItems());
                }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderOverlay(SceneWidget sceneWidget, BlockPosFace blockPosFace) {
        if (machine.getItemFacing() != blockPosFace.facing ||
                machine.getItemIoMode() == VaultInterfacePart.ItemIoMode.DISABLED)
            return;

        int color = switch (machine.getItemIoMode()) {
            case INPUT -> machine.isAutoTransferItems() ? 0xff00aa00 : 0x8f00aa00;
            case OUTPUT -> machine.isAutoTransferItems() ? 0xffff5500 : 0x8fff5500;
            case DISABLED -> 0x00000000;
        };
        sceneWidget.drawFacingBorder(new PoseStack(), blockPosFace, color, 1);
    }

    @Override
    public void addAdditionalUIElements(WidgetGroup parent) {
        LabelWidget text = new LabelWidget(4, 14, "Vault Interface") {

            @Override
            public boolean isVisible() {
                return machine.getItemFacing() != null &&
                        machine.getItemIoMode() != VaultInterfacePart.ItemIoMode.DISABLED;
            }
        };
        text.setTextColor(0xff00aa00).setDropShadow(false);
        parent.addWidget(text);
    }
}
