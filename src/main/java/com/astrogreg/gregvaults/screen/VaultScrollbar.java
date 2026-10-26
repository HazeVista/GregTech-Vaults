package com.astrogreg.gregvaults.screen;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

final class VaultScrollbar {

    private static final int EDGE_SIZE = AbstractVaultScreen.VAULT_EDGE_SIZE;
    private static final int EDGE_COLOR = AbstractVaultScreen.VAULT_PANEL_COLOR;
    private static final int EDGE_OUTLINE_COLOR = AbstractVaultScreen.VAULT_EDGE_OUTLINE_COLOR;
    private static final int EDGE_LIGHT_COLOR = AbstractVaultScreen.VAULT_EDGE_LIGHT_COLOR;
    private static final int EDGE_DARK_COLOR = AbstractVaultScreen.VAULT_EDGE_DARK_COLOR;
    private static final int BORDER_COLOR = 0xFF555555;
    private static final int TRACK_COLOR = 0xFF8B8B8B;
    private static final int THUMB_COLOR = 0xFFD8D8D8;
    private static final int THUMB_COLOR_HIGHLIGHT = 0xFFFFFFFF;

    private final int visibleRows;
    private final IntSupplier maxScrollSupplier;
    private final IntConsumer scrollListener;
    private int x;
    private int y;
    private int width;
    private int height;
    private int scrollOffset;
    private boolean dragging;

    VaultScrollbar(int visibleRows, IntSupplier maxScrollSupplier, IntConsumer scrollListener) {
        this.visibleRows = visibleRows;
        this.maxScrollSupplier = maxScrollSupplier;
        this.scrollListener = scrollListener;
    }

    void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    void setScrollOffset(int scrollOffset) {
        this.scrollOffset = clamp(scrollOffset);
    }

    void render(GuiGraphics graphics) {
        int maxScroll = maxScrollSupplier.getAsInt();
        if (maxScroll == 0) return;

        int extensionLeft = x - EDGE_SIZE;
        int extensionTop = y - EDGE_SIZE;
        int extensionRight = x + width + EDGE_SIZE + 3;
        int extensionBottom = y + height + EDGE_SIZE;
        graphics.fill(extensionLeft, extensionTop, extensionRight - 4, extensionBottom - 1, EDGE_COLOR);
        graphics.fill(extensionRight - 4, extensionTop + 2,
                extensionRight - 3, extensionBottom - 2, EDGE_COLOR);
        graphics.fill(extensionRight - 3, extensionTop + 3,
                extensionRight - 2, extensionBottom - 3, EDGE_COLOR);

        graphics.fill(x, y, x + width, y + height - 1, BORDER_COLOR);
        graphics.fill(x + 1, y + 2, x + width - 1, y + height - 2, TRACK_COLOR);

        int thumbHeight = thumbHeight(maxScroll);
        int thumbY = y + Math.round((float) scrollOffset / maxScroll * (height - thumbHeight));

        int verticalInset = 2;
        int thumbTop = thumbY + verticalInset;
        int thumbBottom = thumbY + thumbHeight - verticalInset;

        graphics.fill(x + 2, thumbTop, x + width - 2, thumbBottom, THUMB_COLOR);
        graphics.fill(x + 2, thumbTop, x + 3, thumbBottom, THUMB_COLOR_HIGHLIGHT);

        graphics.fill(extensionLeft, extensionTop, extensionRight - 4,
                extensionTop + 1, EDGE_OUTLINE_COLOR);
        graphics.fill(extensionLeft, extensionTop + 1, extensionRight - 3,
                extensionTop + 3, EDGE_LIGHT_COLOR);
        graphics.fill(extensionRight - 3, extensionTop + 2,
                extensionRight - 2, extensionTop + 3, EDGE_LIGHT_COLOR);
        graphics.fill(extensionRight - 2, extensionTop + 2,
                extensionRight - 1, extensionTop + 3, EDGE_OUTLINE_COLOR);
        graphics.fill(extensionRight - 3, extensionTop + 3,
                extensionRight - 1, extensionBottom - 4, EDGE_DARK_COLOR);
        graphics.fill(extensionRight - 1, extensionTop + 3,
                extensionRight, extensionBottom - 3, EDGE_OUTLINE_COLOR);
        graphics.fill(extensionRight - 4, extensionBottom - 4,
                extensionRight - 1, extensionBottom - 3, EDGE_DARK_COLOR);
        graphics.fill(extensionLeft, extensionBottom - 3,
                extensionRight - 1, extensionBottom - 2, EDGE_DARK_COLOR);
        graphics.fill(extensionRight - 1, extensionBottom - 3,
                extensionRight, extensionBottom - 2, EDGE_OUTLINE_COLOR);
        graphics.fill(extensionLeft, extensionBottom - 2,
                extensionRight - 2, extensionBottom - 1, EDGE_DARK_COLOR);
        graphics.fill(extensionRight - 2, extensionBottom - 2,
                extensionRight - 1, extensionBottom - 1, EDGE_OUTLINE_COLOR);
        graphics.fill(extensionLeft, extensionBottom - 1,
                extensionRight - 2, extensionBottom, EDGE_OUTLINE_COLOR);
    }

    boolean mouseDragged(double mouseY, int button) {
        if (button != 0 || !dragging) return false;
        scrollFromMouse(mouseY);
        return true;
    }

    boolean mouseScrolled(double delta) {
        scrollTo(scrollOffset + (delta < 0 ? 1 : -1));
        return true;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) return false;
        dragging = true;
        scrollFromMouse(mouseY);
        return true;
    }

    boolean mouseReleased(int button) {
        if (button != 0 || !dragging) return false;
        dragging = false;
        return true;
    }

    private boolean isMouseOver(double mouseX, double mouseY) {
        return maxScrollSupplier.getAsInt() > 0 && mouseX >= x && mouseX < x + width &&
                mouseY >= y && mouseY < y + height;
    }

    private void scrollFromMouse(double mouseY) {
        int maxScroll = maxScrollSupplier.getAsInt();
        if (maxScroll == 0) return;
        int thumbHeight = thumbHeight(maxScroll);
        double ratio = (mouseY - y - thumbHeight / 2.0) / (height - thumbHeight);
        scrollTo((int) Math.round(ratio * maxScroll));
    }

    private void scrollTo(int scrollOffset) {
        int clamped = clamp(scrollOffset);
        if (clamped == this.scrollOffset) return;
        this.scrollOffset = clamped;
        scrollListener.accept(clamped);
    }

    private int clamp(int scrollOffset) {
        return Math.max(0, Math.min(maxScrollSupplier.getAsInt(), scrollOffset));
    }

    private int thumbHeight(int maxScroll) {
        return Math.max(12, height * visibleRows / (maxScroll + visibleRows));
    }
}
