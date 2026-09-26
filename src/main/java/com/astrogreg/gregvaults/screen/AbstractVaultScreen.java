package com.astrogreg.gregvaults.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import com.astrogreg.gregvaults.GregTechVaults;
import com.astrogreg.gregvaults.network.CPacketVaultAction;
import com.astrogreg.gregvaults.network.CPacketVaultCatalogClick;
import com.astrogreg.gregvaults.network.VaultNetwork;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractVaultScreen<T extends AbstractVaultMenu> extends AbstractContainerScreen<T> {

    protected static final ResourceLocation TEXTURE = new ResourceLocation("minecraft",
            "textures/gui/container/generic_54.png");
    protected static final ResourceLocation ARROW_TEXTURE = new ResourceLocation(GregTechVaults.MOD_ID,
            "textures/gui/overlay/crafting_table.png");
    protected static final ResourceLocation A_FIRST_TEXTURE = new ResourceLocation(GregTechVaults.MOD_ID,
            "textures/gui/overlay/a_first.png");
    protected static final ResourceLocation Z_FIRST_TEXTURE = new ResourceLocation(GregTechVaults.MOD_ID,
            "textures/gui/overlay/z_first.png");
    protected static final ResourceLocation HIGHEST_TEXTURE = new ResourceLocation(GregTechVaults.MOD_ID,
            "textures/gui/overlay/highest_first.png");
    protected static final ResourceLocation LOWEST_TEXTURE = new ResourceLocation(GregTechVaults.MOD_ID,
            "textures/gui/overlay/lowest_first.png");

    protected static final int TEX_W = 176;
    protected static final int TEX_TOP_H = 17;
    protected static final int TEX_ROW_H = 18;
    protected static final int TEX_PLAYER_V = 125;
    protected static final int TEX_PLAYER_H = 96;

    private static final int SEARCH_X = 44;
    private static final int SEARCH_Y = 4;
    private static final int SEARCH_W = TEX_W - SEARCH_X - AbstractVaultMenu.SLOTS_X;
    private static final int SEARCH_H = 12;
    private static final int C_SEARCH_EDGE_DARK = 0xFF555555;
    private static final int C_SEARCH_EDGE_LIGHT = 0xFFFFFFFF;
    private static final int C_SEARCH_NORMAL = 0xFF606060;
    private static final int C_SEARCH_FOCUSED = 0xFFA8A8A8;
    private static final int SB_JOIN_X = TEX_W - 3;
    private static final int SB_X = TEX_W - 1;
    private static final int SB_W = 9;
    private static final int SB_EXTENSION_TOP = 2;
    private static final int SB_BOTTOM_MARGIN = 2;
    private static final int C_SB_PANEL = 0xFFC6C6C6;
    private static final int C_SB_RECESS = 0xFF555555;
    private static final int C_SB_TRACK = 0xFF8B8B8B;
    private static final int C_SB_THUMB = 0xFFD8D8D8;
    private static final int C_SB_HIGHLIGHT = 0xFFFFFFFF;
    private static final int BTN_X_OFFSET = -20;
    private static final int BTN_SIZE = 18;
    private static final int BTN_GAP = 2;

    private record IconButton(int relX, int relY, Component tooltip, Runnable action) {

        boolean isHovered(int screenX, int screenY, int mouseX, int mouseY) {
            int x = screenX + relX;
            int y = screenY + relY;
            return mouseX >= x && mouseX < x + BTN_SIZE && mouseY >= y && mouseY < y + BTN_SIZE;
        }
    }

    protected final int visibleRows;
    private final int scrollbarHeight;

    protected EditBox searchBox;
    private List<IconButton> iconButtons = List.of();
    private int scrollOffset;
    private int scrollbarScreenX;
    private int scrollbarScreenTopY;
    private boolean draggingScrollbar;
    private int buttonScreenX;
    private int buttonScreenY;

    private VaultSortMode currentSortMode = VaultSortMode.NAME;
    private boolean currentSortReversed;
    private boolean nameSortReversed;
    private boolean amountSortReversed;

    protected AbstractVaultScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.visibleRows = menu.visibleRows;
        this.scrollbarHeight = visibleRows * TEX_ROW_H - SB_BOTTOM_MARGIN;
        this.imageWidth = TEX_W;
        this.imageHeight = menu.hotbarY + AbstractVaultMenu.SLOT_SIZE + 4;
    }

    @Override
    protected void init() {
        super.init();
        scrollbarScreenX = leftPos + SB_X;
        scrollbarScreenTopY = topPos + TEX_TOP_H;
        buttonScreenX = leftPos;
        buttonScreenY = topPos;

        searchBox = new EditBox(font, leftPos + SEARCH_X + 3, topPos + SEARCH_Y + 2,
                SEARCH_W - 6, SEARCH_H - 4, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setResponder(query -> {
            scrollOffset = 0;
            VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.search(query));
        });
        addRenderableWidget(searchBox);

        List<IconButton> buttons = new ArrayList<>();
        buttons.add(new IconButton(BTN_X_OFFSET, 3,
                Component.literal("Sort: Name"), () -> {
                    nameSortReversed = !nameSortReversed;
                    currentSortMode = VaultSortMode.NAME;
                    currentSortReversed = nameSortReversed;
                    scrollOffset = 0;
                    VaultNetwork.CHANNEL.sendToServer(
                            CPacketVaultAction.sort(VaultSortMode.NAME, nameSortReversed));
                }));
        buttons.add(new IconButton(BTN_X_OFFSET, 3 + BTN_SIZE + BTN_GAP,
                Component.literal("Sort: Amount"), () -> {
                    amountSortReversed = !amountSortReversed;
                    currentSortMode = VaultSortMode.COUNT;
                    currentSortReversed = amountSortReversed;
                    scrollOffset = 0;
                    VaultNetwork.CHANNEL.sendToServer(
                            CPacketVaultAction.sort(VaultSortMode.COUNT, amountSortReversed));
                }));
        iconButtons = List.copyOf(buttons);

        VaultScreenState.State state = VaultScreenState.get();
        currentSortMode = state.sortMode;
        currentSortReversed = state.sortReversed;
        if (state.sortMode == VaultSortMode.NAME) nameSortReversed = state.sortReversed;
        else amountSortReversed = state.sortReversed;

        if (state.sortMode != VaultSortMode.NAME || state.sortReversed) {
            VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.sort(state.sortMode, state.sortReversed));
        }
        if (state.searchQuery != null && !state.searchQuery.isEmpty()) {
            searchBox.setValue(state.searchQuery);
        }
        onInit();
    }

    protected void onInit() {}

    protected void renderExtras(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    protected int maxScroll() {
        return Math.max(0, menu.getTotalCatalogRows() - visibleRows);
    }

    public void syncScrollFromServer(int scrollRow) {
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollRow));
    }

    protected void applyScroll(int newScroll) {
        int clamped = Math.max(0, Math.min(maxScroll(), newScroll));
        if (clamped == scrollOffset) return;
        scrollOffset = clamped;
        VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.scroll(scrollOffset));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderCatalog(graphics, mouseX, mouseY);
        renderButtonIcons(graphics);
        renderExtras(graphics, mouseX, mouseY, partialTick);

        for (IconButton button : iconButtons) {
            if (button.isHovered(buttonScreenX, buttonScreenY, mouseX, mouseY)) {
                graphics.renderTooltip(font, button.tooltip(), mouseX, mouseY);
                break;
            }
        }
        renderTooltip(graphics, mouseX, mouseY);
        renderCatalogTooltip(graphics, mouseX, mouseY);
    }

    private void renderCatalog(GuiGraphics graphics, int mouseX, int mouseY) {
        List<VaultCatalogEntry> entries = menu.getCatalogPage();
        int hovered = catalogIndexAt(mouseX, mouseY);
        for (int index = 0; index < entries.size(); index++) {
            int x = leftPos + AbstractVaultMenu.SLOTS_X + index % AbstractVaultMenu.COLS * TEX_ROW_H;
            int y = topPos + AbstractVaultMenu.SLOTS_Y + index / AbstractVaultMenu.COLS * TEX_ROW_H;
            if (index == hovered) graphics.fill(x, y, x + 16, y + 16, 0x80FFFFFF);

            VaultCatalogEntry entry = entries.get(index);
            graphics.renderItem(entry.stack(), x, y);
            if (entry.count() > 1) renderCount(graphics, x, y, entry.count());
        }
    }

    private void renderCount(GuiGraphics graphics, int x, int y, long count) {
        String text = formatCount(count);
        float scale = 0.5f;
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 200);
        graphics.pose().scale(scale, scale, scale);
        int textX = (int) ((16f - font.width(text) * scale) / scale);
        int textY = (int) ((16f - 7f * scale) / scale);
        graphics.drawString(font, text, textX, textY, 0xFFFFFF, true);
        graphics.pose().popPose();
    }

    private void renderCatalogTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int index = catalogIndexAt(mouseX, mouseY);
        if (index < 0 || index >= menu.getCatalogPage().size()) return;

        VaultCatalogEntry entry = menu.getCatalogPage().get(index);
        TooltipFlag flag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED :
                TooltipFlag.Default.NORMAL;
        List<Component> lines = new ArrayList<>(entry.stack().getTooltipLines(minecraft.player, flag));
        lines.add(1, Component.literal(Long.toString(entry.count())).withStyle(ChatFormatting.GRAY));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private static String formatCount(long count) {
        if (count < 10_000L) return Long.toString(count);
        if (count < 1_000_000L) return (count / 1_000L) + "k";
        if (count < 1_000_000_000L) return (count / 1_000_000L) + "m";
        return (count / 1_000_000_000L) + "b";
    }

    private void renderButtonIcons(GuiGraphics graphics) {
        ResourceLocation[] icons = {
                nameSortReversed ? Z_FIRST_TEXTURE : A_FIRST_TEXTURE,
                amountSortReversed ? LOWEST_TEXTURE : HIGHEST_TEXTURE
        };
        for (int index = 0; index < iconButtons.size(); index++) {
            IconButton button = iconButtons.get(index);
            graphics.blit(icons[index], buttonScreenX + button.relX(), buttonScreenY + button.relY(),
                    0, 0, BTN_SIZE, BTN_SIZE, BTN_SIZE, BTN_SIZE);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        int slotSize = AbstractVaultMenu.SLOT_SIZE;

        graphics.blit(TEXTURE, x, y, 0, 0, TEX_W, TEX_TOP_H);
        renderSearchBoxBackground(graphics, x + SEARCH_X, y + SEARCH_Y);
        for (int row = 0; row < visibleRows; row++) {
            graphics.blit(TEXTURE, x, y + TEX_TOP_H + row * TEX_ROW_H,
                    0, 17, TEX_W, TEX_ROW_H);
        }

        int craftSectionY = y + menu.craftSectionY;
        int craftSectionHeight = 4 * TEX_ROW_H;
        graphics.fill(x, craftSectionY, x + TEX_W, craftSectionY + craftSectionHeight, 0xFFC6C6C6);
        graphics.fill(x, craftSectionY, x + 1, craftSectionY + craftSectionHeight, 0xFF000000);
        graphics.fill(x + 1, craftSectionY, x + 3, craftSectionY + craftSectionHeight, 0xFFFFFFFE);
        graphics.fill(x + TEX_W - 1, craftSectionY, x + TEX_W,
                craftSectionY + craftSectionHeight, 0xFF000000);
        graphics.fill(x + TEX_W - 3, craftSectionY, x + TEX_W - 1,
                craftSectionY + craftSectionHeight, 0xFF4F4F4F);

        int craftingY = y + menu.craftGridY;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                graphics.blit(TEXTURE, x + menu.craftGridX + col * slotSize - 1,
                        craftingY + row * slotSize - 1, 7, 17, slotSize, slotSize);
            }
        }
        graphics.blit(ARROW_TEXTURE, x + menu.craftGridX + 3 * slotSize + 1,
                craftingY + slotSize + (slotSize - 15) / 2,
                0, 0, 22, 15, 22, 15);
        graphics.blit(TEXTURE, x + menu.craftOutX - 1, y + menu.craftOutY - 1,
                7, 17, slotSize, slotSize);
        graphics.blit(TEXTURE, x, y + menu.playerY - 15, 0, TEX_PLAYER_V, TEX_W, TEX_PLAYER_H);
        renderScrollbar(graphics);
    }

    private void renderSearchBoxBackground(GuiGraphics graphics, int x, int y) {
        int fillColor = searchBox != null && searchBox.isFocused() ? C_SEARCH_FOCUSED : C_SEARCH_NORMAL;
        graphics.fill(x, y, x + SEARCH_W, y + SEARCH_H, C_SEARCH_EDGE_DARK);
        graphics.fill(x + 1, y + 1, x + SEARCH_W - 1, y + SEARCH_H - 1, fillColor);
        graphics.fill(x + SEARCH_W - 1, y + 1, x + SEARCH_W, y + SEARCH_H, C_SEARCH_EDGE_LIGHT);
        graphics.fill(x + 1, y + SEARCH_H - 1, x + SEARCH_W, y + SEARCH_H, C_SEARCH_EDGE_LIGHT);
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int maxScroll = maxScroll();
        if (maxScroll == 0) return;

        int x = scrollbarScreenX;
        int y = scrollbarScreenTopY;
        int extensionLeft = leftPos + SB_JOIN_X;
        int extensionTop = y - SB_EXTENSION_TOP;
        int extensionRight = leftPos + TEX_W + SB_W + 1;
        int extensionBottom = y + visibleRows * TEX_ROW_H;
        graphics.fill(extensionLeft, extensionTop, extensionRight, extensionBottom, C_SB_PANEL);
        graphics.fill(extensionLeft, extensionTop, extensionRight, extensionTop + 1, C_SB_HIGHLIGHT);
        graphics.fill(extensionRight - 1, extensionTop + 1, extensionRight, extensionBottom, C_SB_RECESS);
        graphics.fill(extensionLeft, extensionBottom - 1, extensionRight, extensionBottom, C_SB_RECESS);

        graphics.fill(x, y, x + SB_W, y + scrollbarHeight, C_SB_RECESS);
        graphics.fill(x + 1, y + 1, x + SB_W - 1, y + scrollbarHeight - 1, C_SB_TRACK);

        int thumbHeight = scrollbarThumbHeight(maxScroll);
        int thumbY = y + Math.round((float) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
        graphics.fill(x + 1, thumbY, x + SB_W - 1, thumbY + thumbHeight, C_SB_THUMB);
        graphics.fill(x + 1, thumbY, x + 2, thumbY + thumbHeight, C_SB_HIGHLIGHT);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && draggingScrollbar) {
            scrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        applyScroll(scrollOffset + (delta < 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            scrollFromMouse(mouseY);
            return true;
        }

        if (button == 0 || button == 1) {
            int catalogIndex = catalogIndexAt(mouseX, mouseY);
            if (catalogIndex >= 0) {
                ItemStack stack = catalogIndex < menu.getCatalogPage().size() ?
                        menu.getCatalogPage().get(catalogIndex).stack() : ItemStack.EMPTY;
                if (!stack.isEmpty() || !menu.getCarried().isEmpty()) {
                    VaultNetwork.CHANNEL.sendToServer(
                            new CPacketVaultCatalogClick(stack, button, hasShiftDown()));
                    return true;
                }
            }
        }

        if (button == 0) {
            int x = (int) mouseX;
            int y = (int) mouseY;
            for (IconButton iconButton : iconButtons) {
                if (iconButton.isHovered(buttonScreenX, buttonScreenY, x, y)) {
                    iconButton.action().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingScrollbar) {
            draggingScrollbar = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return maxScroll() > 0 &&
                mouseX >= scrollbarScreenX && mouseX < scrollbarScreenX + SB_W &&
                mouseY >= scrollbarScreenTopY && mouseY < scrollbarScreenTopY + scrollbarHeight;
    }

    private void scrollFromMouse(double mouseY) {
        int maxScroll = maxScroll();
        if (maxScroll == 0) return;
        int thumbHeight = scrollbarThumbHeight(maxScroll);
        int travel = scrollbarHeight - thumbHeight;
        double ratio = (mouseY - scrollbarScreenTopY - thumbHeight / 2.0) / travel;
        applyScroll((int) Math.round(ratio * maxScroll));
    }

    private int scrollbarThumbHeight(int maxScroll) {
        return Math.max(12, scrollbarHeight * visibleRows / (maxScroll + visibleRows));
    }

    private int catalogIndexAt(double mouseX, double mouseY) {
        int relativeX = (int) mouseX - leftPos - AbstractVaultMenu.SLOTS_X;
        int relativeY = (int) mouseY - topPos - AbstractVaultMenu.SLOTS_Y;
        if (relativeX < 0 || relativeY < 0 ||
                relativeX >= AbstractVaultMenu.COLS * TEX_ROW_H || relativeY >= visibleRows * TEX_ROW_H) {
            return -1;
        }
        return relativeY / TEX_ROW_H * AbstractVaultMenu.COLS + relativeX / TEX_ROW_H;
    }

    @Override
    public void onClose() {
        VaultScreenState.save(currentSortMode, currentSortReversed,
                searchBox != null ? searchBox.getValue() : "");
        super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            onClose();
            return true;
        }
        if (searchBox.isFocused()) return searchBox.keyPressed(keyCode, scanCode, modifiers);
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (searchBox.isFocused()) return searchBox.charTyped(character, modifiers);
        return super.charTyped(character, modifiers);
    }
}
