package com.litva.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.client.gui.widget.ButtonWidget;

public class LitvaMenu extends Screen {
    private final int widthM = 400;
    private final int heightM = 250;
    private int x, y;

    public LitvaMenu() {
        super(Text.literal("Litva Client Menu"));
    }

    @Override
    protected void init() {
        super.init();
        this.x = (this.width - widthM) / 2;
        this.y = (this.height - heightM) / 2;

        // Кнопки настроек
        int btnX = x + 180;
        int btnY = y + 40;

        // Вкл/Выкл
        this.addDrawableChild(ButtonWidget.builder(Text.literal("TriggerBot: " + (LitvaClient.enabled ? "ON" : "OFF")), b -> {
            LitvaClient.enabled = !LitvaClient.enabled;
            b.setMessage(Text.literal("TriggerBot: " + (LitvaClient.enabled ? "ON" : "OFF")));
        }).dimensions(btnX, btnY, 200, 20).build());

        // Бить игроков
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Бить игроков: " + (LitvaClient.hitPlayers ? "Да" : "Нет")), b -> {
            LitvaClient.hitPlayers = !LitvaClient.hitPlayers;
            b.setMessage(Text.literal("Бить игроков: " + (LitvaClient.hitPlayers ? "Да" : "Нет")));
        }).dimensions(btnX, btnY + 30, 200, 20).build());

        // Бить мобов
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Бить мобов: " + (LitvaClient.hitMobs ? "Да" : "Нет")), b -> {
            LitvaClient.hitMobs = !LitvaClient.hitMobs;
            b.setMessage(Text.literal("Бить мобов: " + (LitvaClient.hitMobs ? "Да" : "Нет")));
        }).dimensions(btnX, btnY + 60, 200, 20).build());

        // Только оружие
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Только оружие: " + (LitvaClient.onlyWeapons ? "Да" : "Нет")), b -> {
            LitvaClient.onlyWeapons = !LitvaClient.onlyWeapons;
            b.setMessage(Text.literal("Только оружие: " + (LitvaClient.onlyWeapons ? "Да" : "Нет")));
        }).dimensions(btnX, btnY + 90, 200, 20).build());

        // Только криты
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Только криты: " + (LitvaClient.onlyCrits ? "Да" : "Нет")), b -> {
            LitvaClient.onlyCrits = !LitvaClient.onlyCrits;
            b.setMessage(Text.literal("Только криты: " + (LitvaClient.onlyCrits ? "Да" : "Нет")));
        }).dimensions(btnX, btnY + 120, 200, 20).build());
        
        // Задержка
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Задержка: " + LitvaClient.delay + " тиков"), b -> {
            LitvaClient.delay++;
            if (LitvaClient.delay > 20) LitvaClient.delay = 1;
            b.setMessage(Text.literal("Задержка: " + LitvaClient.delay + " тиков"));
        }).dimensions(btnX, btnY + 150, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Блюр
        this.applyBlur();
        
        // Черный прямоугольник
        context.fill(x, y, x + widthM, y + heightM, 0xEE000000);
        
        // Заголовок
        context.drawTextWithShadow(this.textRenderer, "Litva Client", x + 20, y + 20, 0xFFAA0000);
        
        // Линия-разделитель
        context.drawVerticalLine(x + 170, y + 30, y + heightM - 20, 0xFF333333);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
