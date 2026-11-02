package com.litva.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class LitvaMenu extends Screen {
    private final int widthM = 480;
    private final int heightM = 280;
    private int x, y;

    private int selectedTab = 0; // 0 - Бой, 1 - Визуал, 2 - Интерфейс
    private int selectedModule = 0; // 0 - TriggerBot, 1 - KillAura

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
        this.applyBlur();
        context.fill(x, y, x + widthM, y + heightM, 0xEE000000);

        // Разделители (серые полоски)
        context.drawVerticalLine(x + 100, y + 30, y + heightM, 0xFF333333);
        context.drawVerticalLine(x + 300, y + 30, y + heightM, 0xFF333333);

        context.drawTextWithShadow(this.textRenderer, "Litva Client", x + 10, y + 10, 0xFF5555FF);

        // Левая колонка
        drawTab(context, "Бой", x + 10, y + 40, 0, mouseX, mouseY);
        drawTab(context, "Визуал", x + 10, y + 65, 1, mouseX, mouseY);
        drawTab(context, "Интерфейс", x + 10, y + 90, 2, mouseX, mouseY);

        // Центральная колонка
        if (selectedTab == 0) {
            drawModule(context, "ТриггерБот", x + 110, y + 40, 0, mouseX, mouseY);
            drawModule(context, "КиллАура", x + 110, y + 65, 1, mouseX, mouseY);
        } else if (selectedTab == 1) {
            drawToggle(context, "Показывать HUD", LitvaClient.showHud, x + 110, y + 40);
        }

        // Правая колонка
        if (selectedTab == 0) {
            if (selectedModule == 0) {
                drawText(context, "Настройки ТриггерБота", x + 310, y + 40);
                drawToggle(context, "Включен", LitvaClient.triggerEnabled, x + 310, y + 60);
                drawToggle(context, "Бить игроков", LitvaClient.hitPlayers, x + 310, y + 85);
                drawToggle(context, "Бить мобов", LitvaClient.hitMobs, x + 310, y + 110);
                drawToggle(context, "Только оружие", LitvaClient.onlyWeapons, x + 310, y + 135);
                drawToggle(context, "Только криты", LitvaClient.onlyCrits, x + 310, y + 160);
            } else if (selectedModule == 1) {
                drawText(context, "Настройки КиллАуры", x + 310, y + 40);
                drawToggle(context, "Включена", LitvaClient.killAuraEnabled, x + 310, y + 60);
                drawToggle(context, "Бить игроков", LitvaClient.kaHitPlayers, x + 310, y + 85);
                drawToggle(context, "Бить мобов", LitvaClient.kaHitMobs, x + 310, y + 110);
                drawToggle(context, "Только оружие", LitvaClient.kaOnlyWeapons, x + 310, y + 135);
                drawToggle(context, "Только криты", LitvaClient.kaOnlyCrits, x + 310, y + 160);
                drawText(context, "Радиус: " + LitvaClient.kaRange, x + 310, y + 185);
            }
        }
    }

    private void drawTab(DrawContext context, String text, int tx, int ty, int id, int mouseX, int mouseY) {
        boolean hovered = mouseX >= tx && mouseX <= tx + 80 && mouseY >= ty && mouseY <= ty + 20;
        if (selectedTab == id) {
            context.fill(tx, ty, tx + 80, ty + 20, 0xFF222222);
            context.drawTextWithShadow(this.textRenderer, text, tx + 5, ty + 6, 0xFFAA0000);
        } else {
            context.drawTextWithShadow(this.textRenderer, text, tx + 5, ty + 6, hovered ? 0xFFFFFF : 0x888888);
        }
    }

    private void drawModule(DrawContext context, String text, int tx, int ty, int id, int mouseX, int mouseY) {
        boolean hovered = mouseX >= tx && mouseX <= tx + 180 && mouseY >= ty && mouseY <= ty + 20;
        if (selectedModule == id) {
            context.fill(tx, ty, tx + 180, ty + 20, 0xFF222222);
            context.drawTextWithShadow(this.textRenderer, text, tx + 5, ty + 6, 0xFFAA0000);
        } else {
            context.drawTextWithShadow(this.textRenderer, text, tx + 5, ty + 6, hovered ? 0xFFFFFF : 0x888888);
        }
    }

    private void drawText(DrawContext context, String text, int tx, int ty) {
        context.drawTextWithShadow(this.textRenderer, text, tx, ty, 0xFFFFFF);
    }

    private void drawToggle(DrawContext context, String text, boolean enabled, int tx, int ty) {
        context.drawTextWithShadow(this.textRenderer, text, tx, ty, 0xAAAAAA);
        int color = enabled ? 0xFFAA0000 : 0xFF333333;
        context.fill(tx + 120, ty, tx + 130, ty + 10, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Вкладки
        if (mouseX >= x + 10 && mouseX <= x + 90) {
            if (mouseY >= y + 40 && mouseY <= y + 60) selectedTab = 0;
            if (mouseY >= y + 65 && mouseY <= y + 85) selectedTab = 1;
            if (mouseY >= y + 90 && mouseY <= y + 110) selectedTab = 2;
        }
        // Модули
        if (mouseX >= x + 110 && mouseX <= x + 290 && selectedTab == 0) {
            if (mouseY >= y + 40 && mouseY <= y + 60) selectedModule = 0;
            if (mouseY >= y + 65 && mouseY <= y + 85) selectedModule = 1;
        }
        // Настройки
        if (selectedTab == 0 && button == 0) {
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
                if (checkBox(mouseX, mouseY, x + 310, y + 135)) LitvaClient.kaOnlyWeapons = !LitvaClient.kaOnlyWeapons;
                if (checkBox(mouseX, mouseY, x + 310, y + 160)) LitvaClient.kaOnlyCrits = !LitvaClient.kaOnlyCrits;
                if (mouseX >= x + 310 && mouseX <= x + 430 && mouseY >= y + 185 && mouseY <= y + 200) {
                    LitvaClient.kaRange += 0.5f;
                    if (LitvaClient.kaRange > 6.0f) LitvaClient.kaRange = 2.0f;
                }
            }
        } else if (selectedTab == 1 && button == 0) {
             if (checkBox(mouseX, mouseY, x + 110, y + 40)) LitvaClient.showHud = !LitvaClient.showHud;
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
