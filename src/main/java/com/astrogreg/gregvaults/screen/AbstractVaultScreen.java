package com.astrogreg.gregvaults.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.astrogreg.gregvaults.network.CPacketVaultAction;
import com.astrogreg.gregvaults.network.CPacketVaultDisplayMode;
import com.astrogreg.gregvaults.network.VaultNetwork;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public abstract class AbstractVaultScreen<T extends AbstractVaultMenu>
                                         extends AbstractContainerScreen<T> {

    protected static final ResourceLocation TEXTURE = new ResourceLocation("minecraft",
            "textures/gui/container/generic_54.png");
    protected static final ResourceLocation ARROW_TEXTURE = new ResourceLocation("gregtechvaults",
            "textures/gui/overlay/crafting_table.png");
    protected static final ResourceLocation SORT_TEXTURE = new ResourceLocation("gregtechvaults",
            "textures/gui/overlay/sort_inventory.png");
    protected static final ResourceLocation STACKED_VIEW_TEXTURE = new ResourceLocation("gregtechvaults",
            "textures/gui/overlay/stacked_view.png");
    protected static final ResourceLocation SLOT_VIEW_TEXTURE = new ResourceLocation("gregtechvaults",
            "textures/gui/overlay/slot_view.png");
    protected static final ResourceLocation A_FIRST_TEXTURE = new ResourceLocation("gregtechvaults",
            "textures/gui/overlay/a_first.png");
    protected static final ResourceLocation Z_FIRST_TEXTURE = new ResourceLocation("gregtechvaults",
            "textures/gui/overlay/z_first.png");
    protected static final ResourceLocation HIGHEST_TEXTURE = new ResourceLocation("gregtechvaults",
            "textures/gui/overlay/highest_first.png");
    protected static final ResourceLocation LOWEST_TEXTURE = new ResourceLocation("gregtechvaults",
            "textures/gui/overlay/lowest_first.png");

    protected static final int TEX_W = 176;
    protected static final int TEX_TOP_H = 17;
    protected static final int TEX_ROW_H = 18;
    protected static final int TEX_PLAYER_V = 125;
    protected static final int TEX_PLAYER_H = 96;

    protected static final int SB_X = TEX_W + 2;
    protected static final int SB_W = 12;
    protected static final int SB_BTN = 12;
    private static final int C_SB_TRACK = 0xFF8B8B8B;
    private static final int C_SB_THUMB = 0xFFCCCCCC;
    private static final int C_SB_BTN = 0xFFAAAAAA;
    private static final int C_INACTIVE = 0x99111111;

    protected static final int BTN_X_OFFSET = -20;
    protected static final int BTN_SIZE = 18;
    protected static final int BTN_GAP = 2;

    private record IconBtn(int relX, int relY, int size, Component tooltip, Runnable action) {

        boolean isHovered(int screenX, int screenY, int mx, int my) {
            int ax = screenX + relX, ay = screenY + relY;
            return mx >= ax && mx < ax + size && my >= ay && my < ay + size;
        }
    }

    protected final int visibleRows;
    protected final int sbH;
    protected final int sbTrackH;
    private int btnScreenX, btnScreenY;

    protected EditBox searchBox;
    private List<IconBtn> iconButtons;
    protected int scrollOffset = 0;
    protected int sbScreenX, sbScreenTopY, sbScreenBotY;

    protected AbstractVaultScreen(T menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.visibleRows = menu.visibleRows;
        this.sbH = visibleRows * TEX_ROW_H;
        this.sbTrackH = sbH - 2 * SB_BTN;
        this.imageWidth = TEX_W + 2 + SB_W;
        this.imageHeight = menu.hotbarY + AbstractVaultMenu.SLOT_SIZE + 4;
    }

    @Override
    protected void init() {
        super.init();

        sbScreenX = leftPos + SB_X;
        sbScreenTopY = topPos + TEX_TOP_H;
        sbScreenBotY = sbScreenTopY + sbH - SB_BTN;
        btnScreenX = leftPos;
        btnScreenY = topPos;

        searchBox = new EditBox(font,
                leftPos + AbstractVaultMenu.SLOTS_X, topPos + 4,
                TEX_W - AbstractVaultMenu.SLOTS_X * 2 - 40, 10,
                Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setBordered(false);
        searchBox.setResponder(query -> {
            scrollOffset = 0;
            menu.updateSearch(query);
            menu.updateScroll(0);
            VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.search(query));
            onSearch(query);
        });
        addRenderableWidget(searchBox);

        iconButtons = new ArrayList<>();
        iconButtons.add(new IconBtn(BTN_X_OFFSET, 3 + (BTN_SIZE + BTN_GAP) * 0, BTN_SIZE,
                Component.literal("Organize"),
                () -> VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.organize())));
        iconButtons.add(new IconBtn(BTN_X_OFFSET, 3 + (BTN_SIZE + BTN_GAP) * 1, BTN_SIZE,
                Component.literal("Stacked / Slot view"),
                () -> {
                    VaultDisplayMode next = menu.getDisplayMode().next();
                    menu.setDisplayMode(next);
                    VaultNetwork.CHANNEL.sendToServer(new CPacketVaultDisplayMode(next));
                }));
        iconButtons.add(new IconBtn(BTN_X_OFFSET, 3 + (BTN_SIZE + BTN_GAP) * 2, BTN_SIZE,
                Component.literal("Sort: Name"),
                () -> {
                    boolean reversed = menu.getSortMode() == VaultSortMode.NAME && !menu.isSortReversed();
                    menu.setSortMode(VaultSortMode.NAME);
                    menu.setSortReversed(reversed);
                    VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.sort(VaultSortMode.NAME, reversed));
                }));
        iconButtons.add(new IconBtn(BTN_X_OFFSET, 3 + (BTN_SIZE + BTN_GAP) * 3, BTN_SIZE,
                Component.literal("Sort: Amount"),
                () -> {
                    boolean reversed = menu.getSortMode() == VaultSortMode.COUNT && !menu.isSortReversed();
                    menu.setSortMode(VaultSortMode.COUNT);
                    menu.setSortReversed(reversed);
                    VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.sort(VaultSortMode.COUNT, reversed));
                }));

        VaultScreenState.State state = VaultScreenState.get();
        menu.setDisplayMode(state.displayMode);
        menu.setSortMode(state.sortMode);
        menu.setSortReversed(state.sortReversed);
        if (state.searchQuery != null && !state.searchQuery.isEmpty()) {
            searchBox.setValue(state.searchQuery);
        }

        onInit();
    }

    protected void onInit() {}

    protected void onSearch(String query) {}

    protected int maxScroll() {
        return Math.max(0, menu.getTotalFilteredRows() - visibleRows);
    }

    protected void applyScroll(int newScroll) {
        newScroll = Math.max(0, Math.min(maxScroll(), newScroll));
        if (newScroll == scrollOffset) return;
        scrollOffset = newScroll;
        menu.updateScroll(scrollOffset);
        VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.scroll(scrollOffset));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderStackedCounts(g);
        renderButtonIcons(g);
        renderExtras(g, mx, my, pt);
        for (IconBtn btn : iconButtons) {
            if (btn.isHovered(btnScreenX, btnScreenY, (int) mx, (int) my)) {
                g.renderTooltip(font, btn.tooltip(), (int) mx, (int) my);
                break;
            }
        }
        renderTooltip(g, mx, my);
    }

    private void renderStackedCounts(GuiGraphics g) {
        if (menu.getDisplayMode() != VaultDisplayMode.STACKED) return;

        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        float scale = 0.75f;
        float inv = 1f / scale;

        int count = menu.getVisibleSlotCount();
        for (int i = 0; i < count; i++) {
            Slot slot = menu.slots.get(i);
            if (!(slot instanceof VaultSlot vaultSlot)) continue;
            if (!slot.isActive() || !vaultSlot.isAggregated()) continue;

            AggregatedStack aggregated = vaultSlot.getAggregatedStack();
            if (aggregated == null || aggregated.displayStack.isEmpty()) continue;

            long total = aggregated.totalCount();
            if (total <= 1) continue;

            String label = formatStackedCount(total);
            float labelW = font.width(label) * scale;
            float x = btnScreenX + slot.x + 17 - labelW;
            float y = btnScreenY + slot.y + 10;

            g.pose().pushPose();
            g.pose().translate(x, y, 300);
            g.pose().scale(scale, scale, 1f);
            g.drawString(font, label, 0, 0, 0xFFFFFF, true);
            g.pose().popPose();
        }

        g.flush();
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
    }

    private static String formatStackedCount(long count) {
        if (count < 1_000) return Long.toString(count);
        if (count < 1_000_000) {
            long whole = count / 1_000;
            long decimal = (count % 1_000) / 100;
            return decimal == 0 ? whole + "k" : whole + "." + decimal + "k";
        }
        if (count < 1_000_000_000L) {
            long whole = count / 1_000_000;
            long decimal = (count % 1_000_000) / 100_000;
            return decimal == 0 ? whole + "m" : whole + "." + decimal + "m";
        }
        long whole = count / 1_000_000_000L;
        long decimal = (count % 1_000_000_000L) / 100_000_000L;
        return decimal == 0 ? whole + "b" : whole + "." + decimal + "b";
    }

    private void renderButtonIcons(GuiGraphics g) {
        ResourceLocation[] icons = {
                SORT_TEXTURE,
                menu.getDisplayMode() == VaultDisplayMode.STACKED ? STACKED_VIEW_TEXTURE : SLOT_VIEW_TEXTURE,
                menu.getSortMode() == VaultSortMode.NAME && menu.isSortReversed() ? Z_FIRST_TEXTURE : A_FIRST_TEXTURE,
                menu.getSortMode() == VaultSortMode.COUNT && menu.isSortReversed() ? LOWEST_TEXTURE : HIGHEST_TEXTURE
        };
        for (int i = 0; i < iconButtons.size(); i++) {
            IconBtn btn = iconButtons.get(i);
            g.blit(icons[i], btnScreenX + btn.relX(), btnScreenY + btn.relY(), 0, 0, BTN_SIZE, BTN_SIZE, BTN_SIZE,
                    BTN_SIZE);
        }
    }

    protected void renderExtras(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos;
        int S = AbstractVaultMenu.SLOT_SIZE;

        g.blit(TEXTURE, x, y, 0, 0, TEX_W, TEX_TOP_H);
        for (int row = 0; row < visibleRows; row++) {
            g.blit(TEXTURE, x, y + TEX_TOP_H + row * TEX_ROW_H, 0, 17, TEX_W, TEX_ROW_H);
        }

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
                        x + menu.craftGridX + col * S - 1,
                        craftGY + row * S - 1,
                        7, 17, S, S);
            }
        }
        g.blit(ARROW_TEXTURE,
                x + menu.craftGridX + 3 * S + 1,
                craftGY + S + (S - 15) / 2,
                0, 0, 22, 15, 22, 15);
        g.blit(TEXTURE, x + menu.craftOutX - 1, y + menu.craftOutY - 1, 7, 17, S, S);
        g.blit(TEXTURE, x, y + menu.playerY - 15, 0, TEX_PLAYER_V, TEX_W, TEX_PLAYER_H);

        renderScrollbar(g);
        renderInactiveSlotOverlays(g);
    }

    private void renderScrollbar(GuiGraphics g) {
        int sbX = sbScreenX, sbY = sbScreenTopY;
        g.fill(sbX, sbY, sbX + SB_W, sbY + sbH, C_SB_TRACK);
        g.fill(sbX, sbY, sbX + SB_W, sbY + SB_BTN, C_SB_BTN);
        g.drawString(font, "\u25b2", sbX + 2, sbY + 2, 0x333333, false);
        g.fill(sbX, sbScreenBotY, sbX + SB_W, sbScreenBotY + SB_BTN, C_SB_BTN);
        g.drawString(font, "\u25bc", sbX + 2, sbScreenBotY + 2, 0x333333, false);

        int maxRows = maxScroll();
        if (maxRows > 0) {
            int thumbH = Math.max(10, sbTrackH * visibleRows / (maxRows + visibleRows));
            int thumbY = (int) ((float) scrollOffset / maxRows * (sbTrackH - thumbH));
            g.fill(sbX + 1, sbY + SB_BTN + thumbY, sbX + SB_W - 1, sbY + SB_BTN + thumbY + thumbH, C_SB_THUMB);
        } else {
            g.fill(sbX + 1, sbY + SB_BTN, sbX + SB_W - 1, sbScreenBotY, C_SB_THUMB);
        }
    }

    private void renderInactiveSlotOverlays(GuiGraphics g) {
        int count = menu.getVisibleSlotCount();
        for (int i = 0; i < count; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.isActive()) {
                g.fill(leftPos + slot.x, topPos + slot.y,
                        leftPos + slot.x + 16, topPos + slot.y + 16, C_INACTIVE);
            }
        }
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (button == 0) {
            int ix = (int) mx;
            if (ix >= sbScreenX && ix < sbScreenX + SB_W) {
                int trackTop = sbScreenTopY + SB_BTN;
                int trackH = sbScreenBotY - trackTop;
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
            for (IconBtn btn : iconButtons) {
                if (btn.isHovered(btnScreenX, btnScreenY, ix, iy)) {
                    btn.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public void onClose() {
        VaultScreenState.save(
                menu.getDisplayMode(),
                menu.getSortMode(),
                menu.isSortReversed(),
                searchBox != null ? searchBox.getValue() : "");
        super.onClose();
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
