package com.donut.client.gui;

import com.donut.client.macros.Macro;
import com.donut.client.macros.MacroSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings GUI for individual macros
 */
public class SettingsGUI extends Screen {

    private final Screen parent;
    private final Macro macro;
    private final MacroSettings settings;

    private static final int PANEL_COLOR = 0xE0202020;
    private static final int BG_COLOR = 0xE0101010;
    private static final int ACCENT_COLOR = 0xFF00AAFF;
    private static final int HOVER_COLOR = 0xFF303030;
    private static final int TEXT_WHITE = 0xFFFFFF;
    private static final int TEXT_GRAY = 0xAAAAAA;

    private int scrollOffset = 0;
    private List<TextFieldWidget> textFields = new ArrayList<>();

    public SettingsGUI(Screen parent, Macro macro) {
        super(Text.literal(macro.getName() + " Settings"));
        this.parent = parent;
        this.macro = macro;
        this.settings = macro.getSettings();
    }

    @Override
    protected void init() {
        super.init();

        textFields.clear();

        // Add done button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> {
            close();
        }).dimensions(width / 2 - 50, height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        // Background
        context.fill(0, 0, width, height, BG_COLOR);

        // Panel
        int panelX = width / 2 - 200;
        int panelY = 50;
        int panelWidth = 400;
        int panelHeight = height - 120;

        context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL_COLOR);

        // Title
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(macro.getName() + " Settings").formatted(Formatting.BOLD),
                width / 2, 30, ACCENT_COLOR);

        // Settings
        if (settings == null || !macro.hasSettings()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("No settings available").formatted(Formatting.GRAY),
                    width / 2, panelY + panelHeight / 2, TEXT_GRAY);
        } else {
            renderSettings(context, panelX, panelY, panelWidth, panelHeight, mouseX, mouseY);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderSettings(DrawContext context, int panelX, int panelY, int panelWidth, int panelHeight, int mouseX, int mouseY) {
        int y = panelY + 20 - scrollOffset;
        int settingHeight = 40;

        for (MacroSettings.Setting<?> setting : settings.getAllSettings()) {
            if (y > panelY && y < panelY + panelHeight - 10) {
                renderSetting(context, setting, panelX + 10, y, panelWidth - 20, mouseX, mouseY);
            }
            y += settingHeight;
        }

        // Scroll bar
        int totalHeight = settings.getAllSettings().size() * settingHeight;
        if (totalHeight > panelHeight) {
            int scrollBarHeight = Math.max(20, panelHeight * panelHeight / totalHeight);
            int maxScroll = totalHeight - panelHeight;
            int scrollBarY = panelY + (panelHeight - scrollBarHeight) * scrollOffset / Math.max(1, maxScroll);

            context.fill(panelX + panelWidth - 5, scrollBarY,
                    panelX + panelWidth - 2, scrollBarY + scrollBarHeight, ACCENT_COLOR);
        }
    }

    private void renderSetting(DrawContext context, MacroSettings.Setting<?> setting, int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 35;

        // Background
        if (hovered) {
            context.fill(x, y, x + width, y + 35, HOVER_COLOR);
        }

        // Setting name
        context.drawTextWithShadow(textRenderer,
                Text.literal(setting.getName()).formatted(Formatting.BOLD),
                x + 5, y + 5, TEXT_WHITE);

        // Setting description
        context.drawTextWithShadow(textRenderer,
                Text.literal(setting.getDescription()).formatted(Formatting.GRAY),
                x + 5, y + 18, TEXT_GRAY);

        // Setting value/control
        int controlX = x + width - 100;
        int controlY = y + 5;

        switch (setting.getType()) {
            case BOOLEAN:
                renderBooleanControl(context, (MacroSettings.BooleanSetting) setting, controlX, controlY, mouseX, mouseY);
                break;
            case INT:
                renderIntControl(context, (MacroSettings.IntSetting) setting, controlX, controlY);
                break;
            case FLOAT:
                renderFloatControl(context, (MacroSettings.FloatSetting) setting, controlX, controlY);
                break;
            case STRING:
                renderStringControl(context, (MacroSettings.StringSetting) setting, controlX, controlY);
                break;
            case ENUM:
                renderEnumControl(context, (MacroSettings.EnumSetting<?>) setting, controlX, controlY, mouseX, mouseY);
                break;
        }
    }

    private void renderBooleanControl(DrawContext context, MacroSettings.BooleanSetting setting, int x, int y, int mouseX, int mouseY) {
        boolean value = setting.getValue();
        boolean hovered = mouseX >= x && mouseX <= x + 60 && mouseY >= y && mouseY <= y + 20;

        int color = value ? 0xFF00AA00 : 0xFF555555;
        if (hovered) {
            color = value ? 0xFF00DD00 : 0xFF777777;
        }

        context.fill(x, y, x + 60, y + 20, color);

        String text = value ? "ON" : "OFF";
        int textX = x + 30 - textRenderer.getWidth(text) / 2;
        context.drawTextWithShadow(textRenderer, Text.literal(text), textX, y + 6, TEXT_WHITE);
    }

    private void renderIntControl(DrawContext context, MacroSettings.IntSetting setting, int x, int y) {
        // Minus button
        context.fill(x, y, x + 20, y + 20, 0xFF333333);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("-"), x + 10, y + 6, TEXT_WHITE);

        // Value
        context.fill(x + 22, y, x + 58, y + 20, PANEL_COLOR);
        String value = String.valueOf(setting.getValue());
        int valueX = x + 40 - textRenderer.getWidth(value) / 2;
        context.drawTextWithShadow(textRenderer, Text.literal(value), valueX, y + 6, TEXT_WHITE);

        // Plus button
        context.fill(x + 60, y, x + 80, y + 20, 0xFF333333);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("+"), x + 70, y + 6, TEXT_WHITE);
    }

    private void renderFloatControl(DrawContext context, MacroSettings.FloatSetting setting, int x, int y) {
        // Minus button
        context.fill(x, y, x + 20, y + 20, 0xFF333333);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("-"), x + 10, y + 6, TEXT_WHITE);

        // Value
        context.fill(x + 22, y, x + 68, y + 20, PANEL_COLOR);
        String value = String.format("%.1f", setting.getValue());
        int valueX = x + 45 - textRenderer.getWidth(value) / 2;
        context.drawTextWithShadow(textRenderer, Text.literal(value), valueX, y + 6, TEXT_WHITE);

        // Plus button
        context.fill(x + 70, y, x + 90, y + 20, 0xFF333333);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("+"), x + 80, y + 6, TEXT_WHITE);
    }

    private void renderStringControl(DrawContext context, MacroSettings.StringSetting setting, int x, int y) {
        context.fill(x, y, x + 150, y + 20, PANEL_COLOR);
        String value = setting.getValue();
        if (value.length() > 20) {
            value = value.substring(0, 17) + "...";
        }
        context.drawTextWithShadow(textRenderer, Text.literal(value), x + 5, y + 6, TEXT_WHITE);
    }

    private void renderEnumControl(DrawContext context, MacroSettings.EnumSetting<?> setting, int x, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX <= x + 100 && mouseY >= y && mouseY <= y + 20;

        int color = hovered ? HOVER_COLOR : 0xFF333333;
        context.fill(x, y, x + 100, y + 20, color);

        String value = setting.getValueString();
        int valueX = x + 50 - textRenderer.getWidth(value) / 2;
        context.drawTextWithShadow(textRenderer, Text.literal(value), valueX, y + 6, TEXT_WHITE);

        // Arrows
        context.drawTextWithShadow(textRenderer, Text.literal("<"), x + 5, y + 6, TEXT_GRAY);
        context.drawTextWithShadow(textRenderer, Text.literal(">"), x + 90, y + 6, TEXT_GRAY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        if (settings == null) return super.mouseClicked(mouseX, mouseY, button);

        int panelX = width / 2 - 200;
        int panelY = 50;
        int panelWidth = 400;
        int y = panelY + 20 - scrollOffset;
        int settingHeight = 40;

        for (MacroSettings.Setting<?> setting : settings.getAllSettings()) {
            if (mouseY >= y && mouseY <= y + 35) {
                handleSettingClick(setting, mouseX, mouseY, panelX, y, panelWidth);
                return true;
            }
            y += settingHeight;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void handleSettingClick(MacroSettings.Setting<?> setting, double mouseX, double mouseY, int panelX, int y, int panelWidth) {
        int controlX = panelX + panelWidth - 90;
        int controlY = y + 5;

        switch (setting.getType()) {
            case BOOLEAN:
                if (mouseX >= controlX && mouseX <= controlX + 60 && mouseY >= controlY && mouseY <= controlY + 20) {
                    ((MacroSettings.BooleanSetting) setting).toggle();
                }
                break;
            case INT:
                MacroSettings.IntSetting intSetting = (MacroSettings.IntSetting) setting;
                if (mouseX >= controlX && mouseX <= controlX + 20) {
                    intSetting.increment(-1);
                } else if (mouseX >= controlX + 60 && mouseX <= controlX + 80) {
                    intSetting.increment(1);
                }
                break;
            case FLOAT:
                MacroSettings.FloatSetting floatSetting = (MacroSettings.FloatSetting) setting;
                if (mouseX >= controlX && mouseX <= controlX + 20) {
                    floatSetting.increment(-0.1f);
                } else if (mouseX >= controlX + 70 && mouseX <= controlX + 90) {
                    floatSetting.increment(0.1f);
                }
                break;
            case ENUM:
                if (mouseX >= controlX && mouseX <= controlX + 100 && mouseY >= controlY && mouseY <= controlY + 20) {
                    ((MacroSettings.EnumSetting<?>) setting).cycle();
                }
                break;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int panelX = width / 2 - 200;
        int panelWidth = 400;

        if (mouseX >= panelX && mouseX <= panelX + panelWidth) {
            scrollOffset -= (int) (verticalAmount * 20);

            int totalHeight = settings.getAllSettings().size() * 40;
            int maxScroll = Math.max(0, totalHeight - (height - 120));
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}