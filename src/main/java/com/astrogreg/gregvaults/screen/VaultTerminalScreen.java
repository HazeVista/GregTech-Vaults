package com.astrogreg.gregvaults.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.astrogreg.gregvaults.network.CPacketVaultScroll;
import com.astrogreg.gregvaults.network.CPacketVaultSearch;
import com.astrogreg.gregvaults.network.VaultNetwork;

public class VaultTerminalScreen extends AbstractContainerScreen<VaultTerminalMenu> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft",
            "textures/gui/container/generic_54.png");

    private static final int TEX_W = 176;
    private static final int TEX_TOP_H = 17;
    private static final int TEX_ROW_H = 18;
    private static final int TEX_PLAYER_V = 125;
    private static final int TEX_PLAYER_H = 96;
    private static final int SB_X = TEX_W + 2;
    private static final int SB_W = 12;
    private static final int SB_BTN = 12;
    private static final int C_ARROW = 0xFF555555;
    private static final int C_SB_TRACK = 0xFF8B8B8B;
    private static final int C_SB_THUMB = 0xFFCCCCCC;
    private static final int C_SB_BTN = 0xFFAAAAAA;
    private static final int C_INACTIVE = 0x99111111;

    private final int visibleRows;
    private final int sbH;
    private final int sbTrackH;

    private EditBox searchBox;
    private int scrollOffset = 0;
    private int sbScreenX, sbScreenTopY, sbScreenBotY;

    public VaultTerminalScreen(VaultTerminalMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.visibleRows = menu.visibleRows;
        this.sbH = visibleRows * TEX_ROW_H;
        this.sbTrackH = sbH - 2 * SB_BTN;

        this.imageWidth = TEX_W + 2 + SB_W;
        this.imageHeight = menu.hotbarY + VaultTerminalMenu.SLOT_SIZE + 4;
    }

    @Override
    protected void init() {
        super.init();

        sbScreenX = leftPos + SB_X;
        sbScreenTopY = topPos + TEX_TOP_H;
        sbScreenBotY = sbScreenTopY + sbH - SB_BTN;

        searchBox = new EditBox(font,
                leftPos + VaultTerminalMenu.SLOTS_X + 2, topPos + 5,
                TEX_W - VaultTerminalMenu.SLOTS_X * 2 - 4, 10,
                Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setBordered(false);
        searchBox.setResponder(query -> {
            scrollOffset = 0;
            menu.updateSearch(query);
            menu.updateScroll(0);
            VaultNetwork.CHANNEL.sendToServer(new CPacketVaultSearch(query));
            VaultNetwork.CHANNEL.sendToServer(new CPacketVaultScroll(0));
        });
        addRenderableWidget(searchBox);
    }

    private int maxScroll() {
        return Math.max(0, menu.getTotalFilteredRows() - visibleRows);
    }

    private void applyScroll(int newScroll) {
        newScroll = Math.max(0, Math.min(maxScroll(), newScroll));
        if (newScroll == scrollOffset) return;
        scrollOffset = newScroll;
        menu.updateScroll(scrollOffset);
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
        int x = leftPos, y = topPos;
        g.drawString(font, "Vault Terminal", leftPos + VaultTerminalMenu.SLOTS_X, topPos - 12, 0xFFEEEEEE, true);

        // Top bar
        g.blit(TEXTURE, x, y, 0, 0, TEX_W, TEX_TOP_H);

        // Vault rows
        for (int row = 0; row < visibleRows; row++) {
            g.blit(TEXTURE, x, y + TEX_TOP_H + row * TEX_ROW_H, 0, 17, TEX_W, TEX_ROW_H);
        }

        // Crafting section
        int craftSecY = y + menu.craftSectionY;
        int craftSecH = 4 * TEX_ROW_H;
        g.fill(x, craftSecY, x + TEX_W, craftSecY + craftSecH, 0xFFC6C6C6);
        g.fill(x, craftSecY, x + 1, craftSecY + craftSecH, 0xFF000000);
        g.fill(x + 1, craftSecY, x + 3, craftSecY + craftSecH, 0xFFFFFFFE);
        g.fill(x + TEX_W - 1, craftSecY, x + TEX_W, craftSecY + craftSecH, 0xFF000000);
        g.fill(x + TEX_W - 3, craftSecY, x + TEX_W - 1, craftSecY + craftSecH, 0xFF4F4F4F);

        int craftGY = y + menu.craftGridY;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                g.blit(TEXTURE,
                        x + menu.craftGridX + col * VaultTerminalMenu.SLOT_SIZE - 1,
                        craftGY + row * VaultTerminalMenu.SLOT_SIZE - 1,
                        7, 17, VaultTerminalMenu.SLOT_SIZE, VaultTerminalMenu.SLOT_SIZE);
            }
        }
        g.blit(TEXTURE,
                x + menu.craftOutX - 1, y + menu.craftOutY - 1,
                7, 17, VaultTerminalMenu.SLOT_SIZE, VaultTerminalMenu.SLOT_SIZE);

        // Player inventory
        g.blit(TEXTURE, x, y + menu.playerY - 15, 0, TEX_PLAYER_V, TEX_W, TEX_PLAYER_H);

        // Scrollbar
        int sbX = sbScreenX, sbY = sbScreenTopY;
        g.fill(sbX, sbY, sbX + SB_W, sbY + sbH, C_SB_TRACK);

        g.fill(sbX, sbY, sbX + SB_W, sbY + SB_BTN, C_SB_BTN);
        g.drawString(font, "▲", sbX + 2, sbY + 2, 0x333333, false);

        g.fill(sbX, sbScreenBotY, sbX + SB_W, sbScreenBotY + SB_BTN, C_SB_BTN);
        g.drawString(font, "▼", sbX + 2, sbScreenBotY + 2, 0x333333, false);

        // Thumb
        int maxRows = maxScroll();
        if (maxRows > 0) {
            int thumbH = Math.max(10, sbTrackH * visibleRows / (maxRows + visibleRows));
            int thumbY = (int) ((float) scrollOffset / maxRows * (sbTrackH - thumbH));
            g.fill(sbX + 1, sbY + SB_BTN + thumbY,
                    sbX + SB_W - 1, sbY + SB_BTN + thumbY + thumbH, C_SB_THUMB);
        } else {
            g.fill(sbX + 1, sbY + SB_BTN, sbX + SB_W - 1, sbScreenBotY, C_SB_THUMB);
        }

        int vaultSlots = menu.getVisibleSlotCount();
        int[] filtered = menu.getFilteredIndices();
        for (int i = 0; i < vaultSlots; i++) {
            Slot slot = menu.slots.get(i);
            int absolute = scrollOffset * VaultTerminalMenu.COLS + i;
            int realIdx = (filtered != null) ? (absolute < filtered.length ? filtered[absolute] : -1) : absolute;
            if (realIdx >= 0 && realIdx < menu.totalSlots && !slot.isActive()) {
                g.fill(leftPos + slot.x, topPos + slot.y,
                        leftPos + slot.x + 16, topPos + slot.y + 16,
                        C_INACTIVE);
            }
        }
    }

    private void drawArrow(GuiGraphics g, int ax, int ay) {
        g.fill(ax, ay + 3, ax + 9, ay + 5, C_ARROW);
        g.fill(ax + 6, ay + 1, ax + 9, ay + 3, C_ARROW);
        g.fill(ax + 6, ay + 5, ax + 9, ay + 7, C_ARROW);
        g.fill(ax + 8, ay, ax + 9, ay + 1, C_ARROW);
        g.fill(ax + 8, ay + 7, ax + 9, ay + 8, C_ARROW);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        String slotInfo = menu.totalSlots + " slots";
        g.drawString(font, slotInfo,
                TEX_W - font.width(slotInfo) - VaultTerminalMenu.SLOTS_X, 6, 0x404040, false);

        g.drawString(font, "Crafting Terminal",
                VaultTerminalMenu.SLOTS_X, menu.craftSectionY + 4, 0x404040, false);

        g.drawString(font, Component.translatable("container.inventory"),
                VaultTerminalMenu.SLOTS_X, menu.playerY - 11, 0x404040, false);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0) {
            int ix = (int) mx;
            if (ix >= sbScreenX && ix < sbScreenX + SB_W) {
                int trackTop = sbScreenTopY + SB_BTN;
                int trackBot = sbScreenBotY;
                int trackH = trackBot - trackTop;
                if (trackH > 0) {
                    float ratio = (float) ((int) my - trackTop) / trackH;
                    applyScroll(Math.round(ratio * maxScroll()));
                    return true;
                }
            }
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        applyScroll(scrollOffset + (delta < 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            int ix = (int) mx, iy = (int) my;
            if (ix >= sbScreenX && ix < sbScreenX + SB_W) {
                if (iy >= sbScreenTopY && iy < sbScreenTopY + SB_BTN) {
                    applyScroll(scrollOffset - 1);
                    return true;
                }
                if (iy >= sbScreenBotY && iy < sbScreenBotY + SB_BTN) {
                    applyScroll(scrollOffset + 1);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        if (searchBox.isFocused()) return searchBox.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (searchBox.isFocused()) return searchBox.charTyped(c, modifiers);
        return super.charTyped(c, modifiers);
    }
}
