package com.astrogreg.gregvaults.screen;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

final class VaultScrollbar {

    private static final int PANEL_MARGIN = 2;
    private static final int PANEL_COLOR = 0xFFC6C6C6;
    private static final int RECESS_COLOR = 0xFF555555;
    private static final int TRACK_COLOR = 0xFF8B8B8B;
    private static final int THUMB_COLOR = 0xFFD8D8D8;
    private static final int HIGHLIGHT_COLOR = 0xFFFFFFFF;

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

        int extensionLeft = x - PANEL_MARGIN;
        int extensionTop = y - PANEL_MARGIN;
        int extensionRight = x + width + PANEL_MARGIN;
        int extensionBottom = y + height + PANEL_MARGIN;
        graphics.fill(extensionLeft, extensionTop, extensionRight, extensionBottom, PANEL_COLOR);
        graphics.fill(extensionLeft, extensionTop, extensionRight, extensionTop + 1, HIGHLIGHT_COLOR);
        graphics.fill(extensionRight - 1, extensionTop + 1, extensionRight, extensionBottom, RECESS_COLOR);
        graphics.fill(extensionLeft, extensionBottom - 1, extensionRight, extensionBottom, RECESS_COLOR);

        graphics.fill(x, y, x + width, y + height, RECESS_COLOR);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, TRACK_COLOR);

        int thumbHeight = thumbHeight(maxScroll);
        int thumbY = y + Math.round((float) scrollOffset / maxScroll * (height - thumbHeight));
        graphics.fill(x + 1, thumbY, x + width - 1, thumbY + thumbHeight, THUMB_COLOR);
        graphics.fill(x + 1, thumbY, x + 2, thumbY + thumbHeight, HIGHLIGHT_COLOR);
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