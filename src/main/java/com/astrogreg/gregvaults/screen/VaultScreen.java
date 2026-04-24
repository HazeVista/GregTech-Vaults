package com.astrogreg.gregvaults.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.astrogreg.gregvaults.network.CPacketVaultScroll;
import com.astrogreg.gregvaults.network.VaultNetwork;

public class VaultScreen extends AbstractContainerScreen<VaultContainerMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
            "minecraft",
            "textures/gui/container/generic_54.png");

    private static final int TEX_W = 176;
    private static final int TEX_TOP_H = 17;
    private static final int TEX_ROW_H = 18;
    private static final int TEX_MID_H = 7;
    private static final int TEX_PLAYER_H = 96;
    private static final int TEX_PLAYER_V = 125;

    private static final int COLS = VaultContainerMenu.COLS;
    private static final int SLOT_SIZE = VaultContainerMenu.SLOT_SIZE;
    private static final int SLOTS_X = VaultContainerMenu.SLOTS_X;
    private static final int SLOTS_Y = VaultContainerMenu.SLOTS_Y;

    private static final int INACTIVE_SLOT_COLOR = 0x99111111;

    private EditBox searchBox;

    private VaultContainerMenu vaultMenu() {
        return (VaultContainerMenu) menu;
    }

    private int scrollOffset = 0;
    private final int visibleRows;

    public VaultScreen(
                       VaultContainerMenu menu,
                       Inventory playerInv,
                       Component title) {
        super(menu, playerInv, title);
        this.visibleRows = vaultMenu().visibleRows;
        this.imageWidth = TEX_W;
        this.imageHeight = TEX_TOP_H + visibleRows * TEX_ROW_H + TEX_MID_H + TEX_PLAYER_H;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(
                font,
                leftPos + SLOTS_X,
                topPos + 4,
                TEX_W - SLOTS_X * 2 - 40,
                10,
                Component.literal(""));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setBordered(false);
        searchBox.setResponder(query -> {
            scrollOffset = 0;
            vaultMenu().updateSearch(query);
            vaultMenu().updateScroll(0);
            VaultNetwork.CHANNEL.sendToServer(new CPacketVaultScroll(0));
        });
        addRenderableWidget(searchBox);
    }

    private int maxScrollRows() {
        return Math.max(0, vaultMenu().getTotalFilteredRows() - visibleRows);
    }

    private void applyScroll() {
        vaultMenu().updateScroll(scrollOffset);
        VaultNetwork.CHANNEL.sendToServer(new CPacketVaultScroll(scrollOffset));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos,
                y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, TEX_W, TEX_TOP_H);

        for (int row = 0; row < visibleRows; row++) {
            g.blit(
                    TEXTURE,
                    x,
                    y + TEX_TOP_H + row * TEX_ROW_H,
                    0,
                    17,
                    TEX_W,
                    TEX_ROW_H);
        }

        g.blit(
                TEXTURE,
                x,
                y + TEX_TOP_H + visibleRows * TEX_ROW_H,
                0,
                TEX_PLAYER_V,
                TEX_W,
                TEX_PLAYER_H);

        int vaultSlotCount = vaultMenu().getVisibleSlotCount();
        for (int i = 0; i < vaultSlotCount; i++) {
            Slot slot = vaultMenu().slots.get(i);
            if (!slot.isActive()) {
                g.fill(
                        leftPos + slot.x,
                        topPos + slot.y,
                        leftPos + slot.x + 16,
                        topPos + slot.y + 16,
                        INACTIVE_SLOT_COLOR);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        int labelY = TEX_TOP_H + visibleRows * TEX_ROW_H + TEX_MID_H - 2;
        g.drawString(
                font,
                Component.translatable("container.inventory"),
                SLOTS_X,
                labelY,
                0x404040,
                false);
        String info = vaultMenu().totalSlots + " slots";
        g.drawString(
                font,
                info,
                imageWidth - font.width(info) - SLOTS_X,
                5,
                0x404040,
                false);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        int newScroll = (int) Math.max(
                0,
                Math.min(maxScrollRows(), scrollOffset - delta));
        if (newScroll != scrollOffset) {
            scrollOffset = newScroll;
            applyScroll();
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }

        if (searchBox.isFocused()) {
            return searchBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (searchBox.isFocused()) {
            return searchBox.charTyped(c, modifiers);
        }
        return super.charTyped(c, modifiers);
    }
}
