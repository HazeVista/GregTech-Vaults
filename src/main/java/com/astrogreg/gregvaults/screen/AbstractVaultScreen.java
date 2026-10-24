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
import org.jetbrains.annotations.NotNull;

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
    private static final int SCROLLBAR_WIDTH = 10;
    private static final int SCROLLBAR_BOTTOM_MARGIN = 2;
    private static final int BTN_X_OFFSET = -20;
    private static final int BTN_SIZE = 18;
    private static final int BTN_GAP = 2;
    protected final int visibleRows;
    private final VaultScrollbar scrollbar;
    protected EditBox searchBox;
    private List<IconButton> iconButtons = List.of();
    private int buttonScreenX;
    private int buttonScreenY;
    private VaultSortMode currentSortMode = VaultSortMode.NAME;
    private boolean currentSortReversed;
    private boolean nameSortReversed;
    private boolean amountSortReversed;

    protected AbstractVaultScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.visibleRows = menu.visibleRows;
        this.scrollbar = new VaultScrollbar(visibleRows, this::maxScroll,
                scrollRow -> VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.scroll(scrollRow)));
        this.imageWidth = TEX_W;
        this.imageHeight = menu.hotbarY + AbstractVaultMenu.SLOT_SIZE + 4;
    }

    private static String formatCount(long count) {
        if (count < 10_000L) return Long.toString(count);
        if (count < 1_000_000L) return (count / 1_000L) + "k";
        if (count < 1_000_000_000L) return (count / 1_000_000L) + "m";
        return (count / 1_000_000_000L) + "b";
    }

    @Override
    protected void init() {
        super.init();
        scrollbar.setBounds(leftPos + TEX_W - 1, topPos + TEX_TOP_H,
                SCROLLBAR_WIDTH, visibleRows * TEX_ROW_H - SCROLLBAR_BOTTOM_MARGIN);
        buttonScreenX = leftPos;
        buttonScreenY = topPos;

        searchBox = new EditBox(font, leftPos + SEARCH_X + 3, topPos + SEARCH_Y + 2,
                SEARCH_W - 6, SEARCH_H - 4, Component.empty());
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setBordered(false);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setResponder(query -> {
            scrollbar.setScrollOffset(0);
            VaultNetwork.CHANNEL.sendToServer(CPacketVaultAction.search(query));
        });
        addRenderableWidget(searchBox);

        List<IconButton> buttons = createButtons();
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

    private @NotNull List<IconButton> createButtons() {
        List<IconButton> buttons = new ArrayList<>();
        buttons.add(new IconButton(BTN_X_OFFSET, 3,
                Component.literal("Sort: Name"), () -> {
                    nameSortReversed = !nameSortReversed;
                    currentSortMode = VaultSortMode.NAME;
                    currentSortReversed = nameSortReversed;
                    scrollbar.setScrollOffset(0);
                    VaultNetwork.CHANNEL.sendToServer(
                            CPacketVaultAction.sort(VaultSortMode.NAME, nameSortReversed));
                }));
        buttons.add(new IconButton(BTN_X_OFFSET, 3 + BTN_SIZE + BTN_GAP,
                Component.literal("Sort: Amount"), () -> {
                    amountSortReversed = !amountSortReversed;
                    currentSortMode = VaultSortMode.COUNT;
                    currentSortReversed = amountSortReversed;
                    scrollbar.setScrollOffset(0);
                    VaultNetwork.CHANNEL.sendToServer(
                            CPacketVaultAction.sort(VaultSortMode.COUNT, amountSortReversed));
                }));
        return buttons;
    }

    protected void onInit() {}

    protected void renderExtras(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    protected int maxScroll() {
        return Math.max(0, menu.getTotalCatalogRows() - visibleRows);
    }

    public void syncScrollFromServer(int scrollRow) {
        scrollbar.setScrollOffset(scrollRow);
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
        scrollbar.render(graphics);
    }

    private void renderSearchBoxBackground(GuiGraphics graphics, int x, int y) {
        int fillColor = searchBox != null && searchBox.isFocused() ? C_SEARCH_FOCUSED : C_SEARCH_NORMAL;
        graphics.fill(x, y, x + SEARCH_W, y + SEARCH_H, C_SEARCH_EDGE_DARK);
        graphics.fill(x + 1, y + 1, x + SEARCH_W - 1, y + SEARCH_H - 1, fillColor);
        graphics.fill(x + SEARCH_W - 1, y + 1, x + SEARCH_W, y + SEARCH_H, C_SEARCH_EDGE_LIGHT);
        graphics.fill(x + 1, y + SEARCH_H - 1, x + SEARCH_W, y + SEARCH_H, C_SEARCH_EDGE_LIGHT);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbar.mouseDragged(mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return scrollbar.mouseScrolled(delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (scrollbar.mouseClicked(mouseX, mouseY, button)) return true;

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
        if (scrollbar.mouseReleased(button)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
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

    private record IconButton(int relX, int relY, Component tooltip, Runnable action) {

        boolean isHovered(int screenX, int screenY, int mouseX, int mouseY) {
            int x = screenX + relX;
            int y = screenY + relY;
            return mouseX >= x && mouseX < x + BTN_SIZE && mouseY >= y && mouseY < y + BTN_SIZE;
        }
    }
}
