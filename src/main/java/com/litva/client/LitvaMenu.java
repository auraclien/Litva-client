package com.litva.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class LitvaMenu extends Screen {
    private final int widthM = 480;
    private final int heightM = 280;
    private int x, y;
    private int selectedTab = 0; 
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
        this.applyBlur();
        context.fill(x, y, x + widthM, y + heightM, 0xEE000000);
        context.drawVerticalLine(x + 100, y + 30, y + heightM, 0xFF333333);
        context.drawVerticalLine(x + 300, y + 30, y + heightM, 0xFF333333);

        context.drawTextWithShadow(this.textRenderer, "Litva Client", x + 10, y + 10, 0xFF5555FF);

        // Вкладки
        drawTab(context, "Combat", x + 10, y + 40, 0, mouseX, mouseY);
        drawTab(context, "Visuals", x + 10, y + 65, 1, mouseX, mouseY);
        drawTab(context, "Interface", x + 10, y + 90, 2, mouseX, mouseY);

        if (selectedTab == 0) {
            drawModule(context, "TriggerBot", x + 110, y + 40, 0, mouseX, mouseY);
            drawModule(context, "KillAura", x + 110, y + 65, 1, mouseX, mouseY);
        } else if (selectedTab == 1) {
            drawToggle(context, "Show HUD", LitvaClient.showHud, x + 110, y + 40);
        }

        if (selectedTab == 0) {
            if (selectedModule == 0) {
                drawText(context, "TriggerBot Settings", x + 310, y + 40);
                drawToggle(context, "Включить", LitvaClient.triggerEnabled, x + 310, y + 60);
                drawToggle(context, "Бить игроков", LitvaClient.hitPlayers, x + 310, y + 85);
                drawToggle(context, "Бить мобов", LitvaClient.hitMobs, x + 310, y + 110);
                drawToggle(context, "Только оружие", LitvaClient.onlyWeapons, x + 310, y + 135);
            } else if (selectedModule == 1) {
                drawText(context, "KillAura Settings", x + 310, y + 40);
                drawToggle(context, "Включить", LitvaClient.killAuraEnabled, x + 310, y + 60);
                drawToggle(context, "Бить игроков", LitvaClient.kaHitPlayers, x + 310, y + 85);
                drawToggle(context, "Бить мобов", LitvaClient.kaHitMobs, x + 310, y + 110);
                drawToggle(context, "Только оружие", LitvaClient.kaOnlyWeapons, x + 310, y + 135);
                drawText(context, "Радиус: " + LitvaClient.kaRange, x + 310, y + 160);
            }
        }

        // --- КНОПКИ ДВИЖЕНИЯ HUD ---
        drawText(context, "Двигать HUD:", x + 310, y + 200);
        drawButton(context, "<", x + 310, y + 215, 20, 20);
        drawButton(context, ">", x + 335, y + 215, 20, 20);
        drawButton(context, "^", x + 322, y + 195, 20, 20);
        drawButton(context, "v", x + 322, y + 235, 20, 20);
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

    private void drawButton(DrawContext context, String text, int tx, int ty, int w, int h) {
        context.fill(tx, ty, tx + w, ty + h, 0xFF333333);
        context.drawTextWithShadow(this.textRenderer, text, tx + 7, ty + 6, 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= x + 10 && mouseX <= x + 90) {
            if (mouseY >= y + 40 && mouseY <= y + 60) selectedTab = 0;
            if (mouseY >= y + 65 && mouseY <= y + 85) selectedTab = 1;
            if (mouseY >= y + 90 && mouseY <= y + 110) selectedTab = 2;
        }
        if (mouseX >= x + 110 && mouseX <= x + 290 && selectedTab == 0) {
            if (mouseY >= y + 40 && mouseY <= y + 60) selectedModule = 0;
            if (mouseY >= y + 65 && mouseY <= y + 85) selectedModule = 1;
        }
        if (selectedTab == 0 && button == 0) {
            if (selectedModule == 0) {
                if (checkBox(mouseX, mouseY, x + 310, y + 60)) LitvaClient.triggerEnabled = !LitvaClient.triggerEnabled;
                if (checkBox(mouseX, mouseY, x + 310, y + 85)) LitvaClient.hitPlayers = !LitvaClient.hitPlayers;
                if (checkBox(mouseX, mouseY, x + 310, y + 110)) LitvaClient.hitMobs = !LitvaClient.hitMobs;
                if (checkBox(mouseX, mouseY, x + 310, y + 135)) LitvaClient.onlyWeapons = !LitvaClient.onlyWeapons;
            } else if (selectedModule == 1) {
                if (checkBox(mouseX, mouseY, x + 310, y + 60)) LitvaClient.killAuraEnabled = !LitvaClient.killAuraEnabled;
                if (checkBox(mouseX, mouseY, x + 310, y + 85)) LitvaClient.kaHitPlayers = !LitvaClient.kaHitPlayers;
                if (checkBox(mouseX, mouseY, x + 310, y + 110)) LitvaClient.kaHitMobs = !LitvaClient.kaHitMobs;
                if (checkBox(mouseX, mouseY, x + 310, y + 135)) LitvaClient.kaOnlyWeapons = !LitvaClient.kaOnlyWeapons;
                if (mouseX >= x + 310 && mouseX <= x + 430 && mouseY >= y + 160 && mouseY <= y + 175) {
                    LitvaClient.kaRange += 0.5f;
                    if (LitvaClient.kaRange > 6.0f) LitvaClient.kaRange = 2.0f;
                }
            }
        } else if (selectedTab == 1 && button == 0) {
             if (checkBox(mouseX, mouseY, x + 110, y + 40)) LitvaClient.showHud = !LitvaClient.showHud;
        }

        // Обработка кнопок движения HUD
        if (mouseX >= x + 310 && mouseX <= x + 330 && mouseY >= y + 215 && mouseY <= y + 235) {
            LitvaClient.hudWatermarkX -= 5; return true;
        }
        if (mouseX >= x + 335 && mouseX <= x + 355 && mouseY >= y + 215 && mouseY <= y + 235) {
            LitvaClient.hudWatermarkX += 5; return true;
        }
        if (mouseX >= x + 322 && mouseX <= x + 342 && mouseY >= y + 195 && mouseY <= y + 215) {
            LitvaClient.hudWatermarkY -= 5; return true;
        }
        if (mouseX >= x + 322 && mouseX <= x + 342 && mouseY >= y + 235 && mouseY <= y + 255) {
            LitvaClient.hudWatermarkY += 5; return true;
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
