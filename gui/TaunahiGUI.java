package com.donut.client.gui;

import com.donut.client.macros.Macro;
import com.donut.client.macros.MacroManager;
import com.donut.client.macros.MacroManager.MacroCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * TaunahiGUI - RIGHT CLICK TO OPEN SETTINGS
 */
public class TaunahiGUI extends Screen {

    // Constants
    private static final int SIDEBAR_WIDTH = 150;
    private static final int CATEGORY_HEIGHT = 25;
    private static final int CATEGORY_SPACING = 30;
    private static final int MACRO_HEIGHT = 60;
    private static final int MACRO_SPACING = 65;
    private static final int BUTTON_SIZE = 20;
    private static final int SCROLL_SPEED = 20;
    private static final int STATUS_BAR_HEIGHT = 25;

    // Texture identifiers
    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("donutclient", "textures/gui/background.png");
    private static final Identifier BUTTON_TEXTURE = Identifier.of("donutclient", "textures/gui/button.png");
    private static final Identifier BUTTON_HOVER_TEXTURE = Identifier.of("donutclient", "textures/gui/button_hover.png");
    private static final Identifier MOD_ICON = Identifier.of("donutclient", "icon.png");

    // Colors
    private static final int BG_COLOR = 0xE0101010;
    private static final int PANEL_COLOR = 0xE0202020;
    private static final int SIDEBAR_COLOR = 0xE0151515;
    private static final int ACCENT_COLOR = 0xFF00AAFF;
    private static final int HOVER_COLOR = 0xFF303030;
    private static final int RUNNING_COLOR = 0xFF00AA00;
    private static final int STOPPED_COLOR = 0xFF555555;
    private static final int TEXT_WHITE = 0xFFFFFF;
    private static final int TEXT_GRAY = 0xAAAAAA;
    private static final int TEXT_DARK_GRAY = 0x888888;

    // Cached Text objects
    private static final Text CATEGORIES_TITLE = Text.literal("Categories").formatted(Formatting.BOLD);
    private static final Text VERSION_TEXT = Text.literal("v1.0.0").formatted(Formatting.GRAY);
    private static final Text NO_MACRO_RUNNING = Text.literal("No macro running").formatted(Formatting.GRAY);

    // Instance fields
    private final MacroManager manager;
    private MacroCategory selectedCategory;
    private int scrollOffset = 0;
    private Macro hoveredMacro = null;

    private boolean hasBackgroundTexture = true;
    private boolean hasButtonTextures = true;
    private boolean hasModIcon = true;

    // Cached category counts
    private final Map<MacroCategory, Integer> categoryCounts = new EnumMap<>(MacroCategory.class);
    private boolean countsNeedUpdate = true;

    // Cached panel dimensions
    private int panelX;
    private int panelWidth;
    private int panelHeight;

    public TaunahiGUI() {
        super(Text.literal("Taunahi Macro System"));
        this.manager = MacroManager.getInstance();
        this.selectedCategory = MacroCategory.FARMING;
    }

    @Override
    protected void init() {
        super.init();

        try {
            client.getResourceManager().getResource(BACKGROUND_TEXTURE);
        } catch (Exception e) {
            hasBackgroundTexture = false;
        }

        try {
            client.getResourceManager().getResource(BUTTON_TEXTURE);
        } catch (Exception e) {
            hasButtonTextures = false;
        }

        try {
            client.getResourceManager().getResource(MOD_ICON);
        } catch (Exception e) {
            hasModIcon = false;
        }

        updatePanelDimensions();
        updateCategoryCounts();
    }

    private void updatePanelDimensions() {
        panelX = SIDEBAR_WIDTH + 10;
        panelWidth = width - SIDEBAR_WIDTH - 20;
        panelHeight = height - STATUS_BAR_HEIGHT - 45;
    }

    private void updateCategoryCounts() {
        if (countsNeedUpdate) {
            categoryCounts.clear();
            for (MacroCategory category : MacroCategory.values()) {
                categoryCounts.put(category, manager.getMacrosByCategory(category).size());
            }
            countsNeedUpdate = false;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        if (hasBackgroundTexture) {
            renderTexturedBackground(context);
        } else {
            context.fill(0, 0, width, height, BG_COLOR);
        }

        renderModIcon(context);
        renderSidebar(context, mouseX, mouseY);
        renderMacroPanel(context, mouseX, mouseY);
        renderStatusBar(context);

        if (hoveredMacro != null) {
            renderTooltip(context, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderModIcon(DrawContext context) {
        if (!hasModIcon) return;

        int iconSize = 32;
        int iconX = 10;
        int iconY = 10;

        context.fill(iconX - 2, iconY - 2, iconX + iconSize + 2, iconY + iconSize + 2, ACCENT_COLOR);
        context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, PANEL_COLOR);

        context.drawTexture(RenderLayer::getGuiTexturedOverlay, MOD_ICON,
                iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);

        context.drawTextWithShadow(textRenderer,
                Text.literal("Donut Client").formatted(Formatting.BOLD),
                iconX + iconSize + 8, iconY + 4, ACCENT_COLOR);

        context.drawTextWithShadow(textRenderer,
                Text.literal("Macro System").formatted(Formatting.GRAY),
                iconX + iconSize + 8, iconY + 16, TEXT_GRAY);
    }

    private void renderTexturedBackground(DrawContext context) {
        int textureWidth = 512;
        int textureHeight = 256;

        int tilesX = (width + textureWidth - 1) / textureWidth;
        int tilesY = (height + textureHeight - 1) / textureHeight;

        for (int tx = 0; tx < tilesX; tx++) {
            int x = tx * textureWidth;
            int drawWidth = Math.min(textureWidth, width - x);

            for (int ty = 0; ty < tilesY; ty++) {
                int y = ty * textureHeight;
                int drawHeight = Math.min(textureHeight, height - y);

                context.drawTexture(RenderLayer::getGuiTexturedOverlay, BACKGROUND_TEXTURE,
                        x, y, 0, 0, drawWidth, drawHeight, textureWidth, textureHeight);
            }
        }
    }

    private void renderSidebar(DrawContext context, int mouseX, int mouseY) {
        context.fill(0, 0, SIDEBAR_WIDTH, height, SIDEBAR_COLOR);
        context.drawCenteredTextWithShadow(textRenderer, CATEGORIES_TITLE, SIDEBAR_WIDTH / 2, 55, TEXT_WHITE);

        int y = 80;
        boolean mouseInSidebar = mouseX < SIDEBAR_WIDTH;

        for (MacroCategory category : MacroCategory.values()) {
            Integer count = categoryCounts.get(category);
            if (count == null || count == 0) continue;

            boolean isSelected = category == selectedCategory;
            boolean isHovered = mouseInSidebar && mouseY >= y && mouseY < y + CATEGORY_HEIGHT;

            if (isSelected) {
                context.fill(5, y, SIDEBAR_WIDTH - 5, y + CATEGORY_HEIGHT, ACCENT_COLOR);
            } else if (isHovered) {
                context.fill(5, y, SIDEBAR_WIDTH - 5, y + CATEGORY_HEIGHT, HOVER_COLOR);
            }

            String text = category.icon + " " + category.name + " (" + count + ")";
            int textColor = (isSelected || isHovered) ? TEXT_WHITE : TEXT_GRAY;
            context.drawTextWithShadow(textRenderer, Text.literal(text), 10, y + 8, textColor);

            y += CATEGORY_SPACING;
        }

        context.drawTextWithShadow(textRenderer, VERSION_TEXT, 10, height - 20, TEXT_DARK_GRAY);
    }

    private void renderMacroPanel(DrawContext context, int mouseX, int mouseY) {
        context.fill(panelX, 10, panelX + panelWidth, 10 + panelHeight, PANEL_COLOR);

        Text categoryTitle = Text.literal(selectedCategory.icon + " " + selectedCategory.name + " Macros")
                .formatted(Formatting.BOLD);
        context.drawTextWithShadow(textRenderer, categoryTitle, panelX + 10, 20, ACCENT_COLOR);

        List<Macro> macros = manager.getMacrosByCategory(selectedCategory);

        if (macros.isEmpty()) {
            Text emptyMsg = Text.literal("No macros in this category").formatted(Formatting.GRAY);
            context.drawCenteredTextWithShadow(textRenderer, emptyMsg, panelX + panelWidth / 2, 100, TEXT_DARK_GRAY);
            return;
        }

        hoveredMacro = null;
        int startY = 45 - scrollOffset;
        int endY = 10 + panelHeight;

        for (Macro macro : macros) {
            int macroY = startY;
            startY += MACRO_SPACING;

            if (macroY + MACRO_HEIGHT < 40 || macroY > endY) continue;

            boolean hovered = renderMacro(context, macro, panelX + 10, macroY, panelWidth - 20, mouseX, mouseY);
            if (hovered) {
                hoveredMacro = macro;
            }
        }

        int totalHeight = macros.size() * MACRO_SPACING;
        if (totalHeight > panelHeight) {
            int scrollBarHeight = Math.max(20, panelHeight * panelHeight / totalHeight);
            int maxScroll = totalHeight - panelHeight;
            int scrollBarY = 10 + (panelHeight - scrollBarHeight) * scrollOffset / maxScroll;

            context.fill(panelX + panelWidth - 8, scrollBarY,
                    panelX + panelWidth - 5, scrollBarY + scrollBarHeight, ACCENT_COLOR);
        }
    }

    private boolean renderMacro(DrawContext context, Macro macro, int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + MACRO_HEIGHT;
        boolean running = macro.isEnabled();

        int bgColor = running ? (hovered ? 0xFF204520 : 0xFF1A3A1A) : (hovered ? HOVER_COLOR : PANEL_COLOR);
        context.fill(x, y, x + width, y + MACRO_HEIGHT, bgColor);

        int borderColor = running ? RUNNING_COLOR : STOPPED_COLOR;
        context.fill(x, y, x + 3, y + MACRO_HEIGHT, borderColor);

        Text nameText = Text.literal(macro.getName()).formatted(Formatting.BOLD);
        context.drawTextWithShadow(textRenderer, nameText, x + 10, y + 8, running ? 0x00FF00 : TEXT_WHITE);

        context.drawTextWithShadow(textRenderer, Text.literal(macro.getDescription()).formatted(Formatting.GRAY),
                x + 10, y + 22, TEXT_GRAY);

        String statusIcon = running ? "⬤ RUNNING" : "⭘ Stopped";
        int statusColor = running ? RUNNING_COLOR : TEXT_DARK_GRAY;
        context.drawTextWithShadow(textRenderer, Text.literal(statusIcon), x + 10, y + 38, statusColor);

        long runtime = macro.getRuntime();
        if (running || runtime > 0) {
            String runtimeText = "⏱ " + macro.getRuntimeFormatted();
            context.drawTextWithShadow(textRenderer, Text.literal(runtimeText), x + 120, y + 38, 0xFFAA00);
        }

        int buttonX = x + width - 30;
        int buttonY = y + 20;
        boolean buttonHovered = hovered && mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE &&
                mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE;
        renderActionButton(context, buttonX, buttonY, buttonHovered);

        double progress = macro.getProgress();
        if (progress > 0 && progress <= 1.0) {
            int barWidth = width - 20;
            int barY = y + 52;

            context.fill(x + 10, barY, x + 10 + barWidth, barY + 4, 0xFF333333);
            context.fill(x + 10, barY, x + 10 + (int)(barWidth * progress), barY + 4, ACCENT_COLOR);
        }

        return hovered;
    }

    private void renderActionButton(DrawContext context, int x, int y, boolean hovered) {
        if (hasButtonTextures) {
            Identifier texture = hovered ? BUTTON_HOVER_TEXTURE : BUTTON_TEXTURE;
            context.drawTexture(RenderLayer::getGuiTexturedOverlay, texture,
                    x, y, 0, 0, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
        } else {
            int buttonColor = hovered ? 0xFF555555 : 0xFF333333;
            context.fill(x, y, x + BUTTON_SIZE, y + BUTTON_SIZE, buttonColor);
            context.fill(x, y, x + BUTTON_SIZE, y + 1, 0xFF666666);
            context.fill(x, y, x + 1, y + BUTTON_SIZE, 0xFF666666);
            context.fill(x, y + 19, x + BUTTON_SIZE, y + BUTTON_SIZE, 0xFF111111);
            context.fill(x + 19, y, x + BUTTON_SIZE, y + BUTTON_SIZE, 0xFF111111);

            int iconX = x + 6;
            int iconY = y + 6;
            context.drawTextWithShadow(textRenderer, Text.literal("⚙"), iconX, iconY, TEXT_WHITE);
        }
    }

    private void renderStatusBar(DrawContext context) {
        int barY = height - STATUS_BAR_HEIGHT;

        context.fill(0, barY, width, height, SIDEBAR_COLOR);

        Macro activeMacro = manager.getActiveMacro();
        if (activeMacro != null) {
            String text = "⬤ Active: " + activeMacro.getName() + " | Runtime: " + activeMacro.getRuntimeFormatted();
            context.drawTextWithShadow(textRenderer, Text.literal(text), 10, barY + 8, RUNNING_COLOR);
        } else {
            context.drawTextWithShadow(textRenderer, NO_MACRO_RUNNING, 10, barY + 8, TEXT_DARK_GRAY);
        }

        String totalText = "Total Macros: " + manager.getTotalMacroCount();
        int totalWidth = textRenderer.getWidth(totalText);
        context.drawTextWithShadow(textRenderer, Text.literal(totalText),
                width - totalWidth - 10, barY + 8, TEXT_GRAY);
    }

    private void renderTooltip(DrawContext context, int mouseX, int mouseY) {
        String statusInfo = hoveredMacro.getStatusInfo();
        if (statusInfo == null || statusInfo.isEmpty()) return;

        // Add hint for right-click if macro has settings
        if (hoveredMacro.hasSettings()) {
            statusInfo += " | Right-click for settings";
        }

        int tooltipWidth = textRenderer.getWidth(statusInfo) + 8;
        int tooltipHeight = 20;
        int tooltipX = Math.min(mouseX + 10, width - tooltipWidth - 5);
        int tooltipY = Math.min(mouseY + 10, height - tooltipHeight - 5);

        context.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, 0xE0000000);
        context.fill(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 1, ACCENT_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(statusInfo), tooltipX + 4, tooltipY + 6, TEXT_WHITE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Left click
        if (button == 0) {
            if (mouseX < SIDEBAR_WIDTH) {
                return handleCategoryClick(mouseY);
            }

            if (mouseX >= SIDEBAR_WIDTH + 10) {
                return handleMacroLeftClick(mouseX, mouseY);
            }
        }

        // Right click - open settings
        if (button == 1) {
            if (mouseX >= SIDEBAR_WIDTH + 10) {
                return handleMacroRightClick(mouseX, mouseY);
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleCategoryClick(double mouseY) {
        int y = 80;
        for (MacroCategory category : MacroCategory.values()) {
            Integer count = categoryCounts.get(category);
            if (count == null || count == 0) continue;

            if (mouseY >= y && mouseY < y + CATEGORY_HEIGHT) {
                selectedCategory = category;
                scrollOffset = 0;
                return true;
            }
            y += CATEGORY_SPACING;
        }
        return false;
    }

    private boolean handleMacroLeftClick(double mouseX, double mouseY) {
        List<Macro> macros = manager.getMacrosByCategory(selectedCategory);
        int y = 45 - scrollOffset;

        for (Macro macro : macros) {
            if (mouseY >= y && mouseY <= y + MACRO_HEIGHT &&
                    mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10) {

                // Check settings button
                int buttonX = panelX + panelWidth - 30;
                int buttonY = y + 20;

                if (mouseX >= buttonX && mouseX < buttonX + BUTTON_SIZE &&
                        mouseY >= buttonY && mouseY < buttonY + BUTTON_SIZE) {
                    // Settings button clicked
                    openSettings(macro);
                    return true;
                }

                // Toggle macro
                if (macro.isEnabled()) {
                    manager.stopMacro(macro);
                } else {
                    manager.startMacro(macro);
                }
                countsNeedUpdate = true;
                return true;
            }
            y += MACRO_SPACING;
        }
        return false;
    }

    private boolean handleMacroRightClick(double mouseX, double mouseY) {
        List<Macro> macros = manager.getMacrosByCategory(selectedCategory);
        int y = 45 - scrollOffset;

        for (Macro macro : macros) {
            if (mouseY >= y && mouseY <= y + MACRO_HEIGHT &&
                    mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10) {

                // Right-click opens settings
                openSettings(macro);
                return true;
            }
            y += MACRO_SPACING;
        }
        return false;
    }

    private void openSettings(Macro macro) {
        if (macro.hasSettings()) {
            client.setScreen(new SettingsGUI(this, macro));
        } else {
            // No settings available - could show a message
            System.out.println("[TaunahiGUI] " + macro.getName() + " has no settings");
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= SIDEBAR_WIDTH) {
            scrollOffset -= (int) (verticalAmount * SCROLL_SPEED);

            List<Macro> macros = manager.getMacrosByCategory(selectedCategory);
            int maxScroll = Math.max(0, macros.size() * MACRO_SPACING - panelHeight);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}