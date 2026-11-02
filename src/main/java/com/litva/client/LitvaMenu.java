package com.litva.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class LitvaMenu extends Screen {
    private final int widthM = 480;
    private final int heightM = 280;
    private int x, y;

    // Какая вкладка открыта: 0 - Combat, 1 - Visuals, 2 - Interface
    private int selectedTab = 0;
    // Какая функция выбрана (0 - TriggerBot, 1 - KillAura)
    private int selectedModule = 0;

    public LitvaMenu() {
        super(Text.literal("Litva Client"));
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - widthM) / 2;
        this.y = (this.height - heightM) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Блюр
        this.applyBlur();
        
        // Главный фон меню (черный)
        context.fill(x, y, x + widthM, y + heightM, 0xEE000000);

        // Линии колонок
        context.drawVerticalLine(x + 100, y + 30, y + heightM, 0xFF333333);
        context.drawVerticalLine(x + 300, y + 30, y + heightM, 0xFF333333);

        // Заголовок
        context.drawTextWithShadow(this.textRenderer, "Litva Client", x + 10, y + 10, 0xFF5555FF);

        // --- ЛЕВАЯ КОЛОНКА (Категории) ---
        drawTab(context, "Combat", x + 10, y + 40, 0, mouseX, mouseY);
        drawTab(context, "Visuals", x + 10, y + 65, 1, mouseX, mouseY);
        drawTab(context, "Interface", x + 10, y + 90, 2, mouseX, mouseY);

        // --- ЦЕНТРАЛЬНАЯ КОЛОНКА (Модули) ---
        if (selectedTab == 0) {
            drawModule(context, "TriggerBot", x + 110, y + 40, 0, mouseX, mouseY);
            drawModule(context, "KillAura", x + 110, y + 65, 1, mouseX, mouseY);
        }

        // --- ПРАВАЯ КОЛОНКА (Настройки) ---
        if (selectedTab == 0) {
            if (selectedModule == 0) {
                drawSettingText(context, "TriggerBot Settings", x + 310, y + 40);
                drawToggle(context, "Enabled", LitvaClient.triggerEnabled, x + 310, y + 60);
                drawToggle(context, "Hit Players", LitvaClient.hitPlayers, x + 310, y + 85);
                drawToggle(context, "Hit Mobs", LitvaClient.hitMobs, x + 310, y + 110);
                drawToggle(context, "Only Weapons", LitvaClient.onlyWeapons, x + 310, y + 135);
                drawToggle(context, "Only Crits", LitvaClient.onlyCrits, x + 310, y + 160);
            } else if (selectedModule == 1) {
                drawSettingText(context, "KillAura Settings", x + 310, y + 40);
                drawToggle(context, "Enabled", LitvaClient.killAuraEnabled, x + 310, y + 60);
                drawToggle(context, "Hit Players", LitvaClient.kaHitPlayers, x + 310, y + 85);
                drawToggle(context, "Hit Mobs", LitvaClient.kaHitMobs, x + 310, y + 110);
                drawSettingText(context, "Range: " + LitvaClient.kaRange, x + 310, y + 135);
            }
        }
    }

    // Отрисовка вкладки (слева)
    private void drawTab(DrawContext context, String text, int tx, int ty, int id, int mouseX, int mouseY) {
        boolean hovered = mouseX >= tx && mouseX <= tx + 80 && mouseY >= ty && mouseY <= ty + 20;
        if (selectedTab == id) {
            context.fill(tx, ty, tx + 80, ty + 20, 0xFF222222);
            context.drawTextWithShadow(this.textRenderer, text, tx + 5, ty + 6, 0xFFAA0000);
        } else {
            context.drawTextWithShadow(this.textRenderer, text, tx + 5, ty + 6, hovered ? 0xFFFFFF : 0x888888);
        }
    }

    // Отрисовка модуля (в центре)
    private void drawModule(DrawContext context, String text, int tx, int ty, int id, int mouseX, int mouseY) {
        boolean hovered = mouseX >= tx && mouseX <= tx + 180 && mouseY >= ty && mouseY <= ty + 20;
        if (selectedModule == id) {
            context.fill(tx, ty, tx + 180, ty + 20, 0xFF222222);
            context.drawTextWithShadow(this.textRenderer, text, tx + 5, ty + 6, 0xFFAA0000);
        } else {
            context.drawTextWithShadow(this.textRenderer, text, tx + 5, ty + 6, hovered ? 0xFFFFFF : 0x888888);
        }
    }

    // Отрисовка текста настроек
    private void drawSettingText(DrawContext context, String text, int tx, int ty) {
        context.drawTextWithShadow(this.textRenderer, text, tx, ty, 0xFFFFFF);
    }

    // Отрисовка переключателя (квадратик)
    private void drawToggle(DrawContext context, String text, boolean enabled, int tx, int ty) {
        context.drawTextWithShadow(this.textRenderer, text, tx, ty, 0xAAAAAA);
        int color = enabled ? 0xFFAA0000 : 0xFF333333;
        context.fill(tx + 120, ty, tx + 130, ty + 10, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Левая колонка
        if (mouseX >= x + 10 && mouseX <= x + 90) {
            if (mouseY >= y + 40 && mouseY <= y + 60) selectedTab = 0;
            if (mouseY >= y + 65 && mouseY <= y + 85) selectedTab = 1;
            if (mouseY >= y + 90 && mouseY <= y + 110) selectedTab = 2;
        }
        // Центральная колонка
        if (mouseX >= x + 110 && mouseX <= x + 290 && selectedTab == 0) {
            if (mouseY >= y + 40 && mouseY <= y + 60) selectedModule = 0;
            if (mouseY >= y + 65 && mouseY <= y + 85) selectedModule = 1;
        }

        // Правая колонка (Настройки) - ЛКМ для переключения
        if (selectedTab == 0 && button == 0) { // Левая кнопка
            if (selectedModule == 0) {
                if (checkBox(mouseX, mouseY, x + 310, y + 60)) LitvaClient.triggerEnabled = !LitvaClient.triggerEnabled;
                if (checkBox(mouseX, mouseY, x + 310, y + 85)) LitvaClient.hitPlayers = !LitvaClient.hitPlayers;
                if (checkBox(mouseX, mouseY, x + 310, y + 110)) LitvaClient.hitMobs = !LitvaClient.hitMobs;
                if (checkBox(mouseX, mouseY, x + 310, y + 135)) LitvaClient.onlyWeapons = !LitvaClient.onlyWeapons;
                if (checkBox(mouseX, mouseY, x + 310, y + 160)) LitvaClient.onlyCrits = !LitvaClient.onlyCrits;
            } else if (selectedModule == 1) {
                if (checkBox(mouseX, mouseY, x + 310, y + 60)) LitvaClient.killAuraEnabled = !LitvaClient.killAuraEnabled;
                if (checkBox(mouseX, mouseY, x + 310, y + 85)) LitvaClient.kaHitPlayers = !LitvaClient.kaHitPlayers;
                if (checkBox(mouseX, mouseY, x + 310, y + 110)) LitvaClient.kaHitMobs = !LitvaClient.kaHitMobs;
                // Изменение радиуса (по клику)
                if (mouseX >= x + 310 && mouseX <= x + 430 && mouseY >= y + 135 && mouseY <= y + 150) {
                    LitvaClient.kaRange += 0.5f;
                    if (LitvaClient.kaRange > 6.0f) LitvaClient.kaRange = 2.0f;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean checkBox(double mouseX, double mouseY, int tx, int ty) {
        return mouseX >= tx + 120 && mouseX <= tx + 135 && mouseY >= ty - 2 && mouseY <= ty + 12;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
