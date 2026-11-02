package com.litva.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class LitvaMenu extends Screen {
    private final int widthM = 480;
    private final int heightM = 280;
    private int x, y;
    private int selectedTab = 0; 
    private int selectedModule = 0; 

    // Переменные для перетаскивания HUD
    private boolean draggingWatermark = false, draggingInfo = false, draggingTarget = false;
    private double dragOffsetX = 0, dragOffsetY = 0;

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
        // Если открыт чат, мы рисуем HUD и даем его двигать
        if (this instanceof ChatScreen) {
            LitvaHud.render(context);
            drawHudGuides(context);
            super.render(context, mouseX, mouseY, delta);
            return;
        }

        // Обычное меню
        this.applyBlur();
        context.fill(x, y, x + widthM, y + heightM, 0xEE000000);
        context.drawVerticalLine(x + 100, y + 30, y + heightM, 0xFF333333);
        context.drawVerticalLine(x + 300, y + 30, y + heightM, 0xFF333333);

        context.drawTextWithShadow(this.textRenderer, "Litva Client", x + 10, y + 10, 0xFF5555FF);

        // Вкладки (Английский)
        drawTab(context, "Combat", x + 10, y + 40, 0, mouseX, mouseY);
        drawTab(context, "Visuals", x + 10, y + 65, 1, mouseX, mouseY);
        drawTab(context, "Interface", x + 10, y + 90, 2, mouseX, mouseY);

        if (selectedTab == 0) {
            drawModule(context, "TriggerBot", x + 110, y + 40, 0, mouseX, mouseY);
            drawModule(context, "KillAura", x + 110, y + 65, 1, mouseX, mouseY);
        } else if (selectedTab == 1) {
            drawToggle(context, "Показывать HUD", LitvaClient.showHud, x + 110, y + 40);
        }

        if (selectedTab == 0) {
            if (selectedModule == 0) {
                drawText(context, "Настройки ТриггерБота", x + 310, y + 40);
                drawToggle(context, "Включить", LitvaClient.triggerEnabled, x + 310, y + 60);
                drawToggle(context, "Бить игроков", LitvaClient.hitPlayers, x + 310, y + 85);
                drawToggle(context, "Бить мобов", LitvaClient.hitMobs, x + 310, y + 110);
                drawToggle(context, "Только оружие", LitvaClient.onlyWeapons, x + 310, y + 135);
                drawToggle(context, "Только криты", LitvaClient.onlyCrits, x + 310, y + 160);
            } else if (selectedModule == 1) {
                drawText(context, "Настройки КиллАуры", x + 310, y + 40);
                drawToggle(context, "Включить", LitvaClient.killAuraEnabled, x + 310, y + 60);
                drawToggle(context, "Бить игроков", LitvaClient.kaHitPlayers, x + 310, y + 85);
                drawToggle(context, "Бить мобов", LitvaClient.kaHitMobs, x + 310, y + 110);
                drawToggle(context, "Только оружие", LitvaClient.kaOnlyWeapons, x + 310, y + 135);
                drawToggle(context, "Только криты", LitvaClient.kaOnlyCrits, x + 310, y + 160);
                drawText(context, "Радиус: " + LitvaClient.kaRange, x + 310, y + 185);
            }
        }
    }

    // Отрисовка белых рамок вокруг HUD (для понимания, что двигать)
    private void drawHudGuides(DrawContext context) {
        int w = 100; 
        context.drawBorder(LitvaClient.hudWatermarkX - 2, LitvaClient.hudWatermarkY - 2, w, 15, 0xFFFFFFFF);
        context.drawBorder(LitvaClient.hudInfoX - 2, LitvaClient.hudInfoY - 2, w, 25, 0xFFFFFFFF);
        if (LitvaClient.targetEntity != null) {
            context.drawBorder(LitvaClient.hudTargetX, LitvaClient.hudTargetY, 130, 40, 0xFFFFFFFF);
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

    // --- ЛОГИКА ПЕРЕТАСКИВАНИЯ HUD ---
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this instanceof ChatScreen) {
            if (button == 0) {
                // Проверка клика по зонам HUD
                if (checkHit(mouseX, mouseY, LitvaClient.hudWatermarkX, LitvaClient.hudWatermarkY, 100, 15)) {
                    draggingWatermark = true;
                    dragOffsetX = mouseX - LitvaClient.hudWatermarkX;
                    dragOffsetY = mouseY - LitvaClient.hudWatermarkY;
                    return true;
                }
                if (checkHit(mouseX, mouseY, LitvaClient.hudInfoX, LitvaClient.hudInfoY, 100, 25)) {
                    draggingInfo = true;
                    dragOffsetX = mouseX - LitvaClient.hudInfoX;
                    dragOffsetY = mouseY - LitvaClient.hudInfoY;
                    return true;
                }
                if (LitvaClient.targetEntity != null && checkHit(mouseX, mouseY, LitvaClient.hudTargetX, LitvaClient.hudTargetY, 130, 40)) {
                    draggingTarget = true;
                    dragOffsetX = mouseX - LitvaClient.hudTargetX;
                    dragOffsetY = mouseY - LitvaClient.hudTargetY;
                    return true;
                }
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // Логика меню
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

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this instanceof ChatScreen && button == 0) {
            if (draggingWatermark) {
                LitvaClient.hudWatermarkX = (int) (mouseX - dragOffsetX);
                LitvaClient.hudWatermarkY = (int) (mouseY - dragOffsetY);
                return true;
            }
            if (draggingInfo) {
                LitvaClient.hudInfoX = (int) (mouseX - dragOffsetX);
                LitvaClient.hudInfoY = (int) (mouseY - dragOffsetY);
                return true;
            }
            if (draggingTarget) {
                LitvaClient.hudTargetX = (int) (mouseX - dragOffsetX);
                LitvaClient.hudTargetY = (int) (mouseY - dragOffsetY);
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWatermark = false;
        draggingInfo = false;
        draggingTarget = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean checkHit(double mx, double my, int tx, int ty, int w, int h) {
        return mx >= tx && mx <= tx + w && my >= ty && my <= ty + h;
    }

    private boolean checkBox(double mouseX, double mouseY, int tx, int ty) {
        return mouseX >= tx + 120 && mouseX <= tx + 135 && mouseY >= ty - 2 && mouseY <= ty + 12;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
            }
