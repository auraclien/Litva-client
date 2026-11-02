package com.litva.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class LitvaHud {
    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        int y = 10;

        // Водяной знак (Watermark)
        context.drawTextWithShadow(mc.textRenderer, "Litva Client", 5, 5, 0xFFAA0000);

        // Список активных модулей (справа)
        if (LitvaClient.triggerEnabled) {
            context.drawTextWithShadow(mc.textRenderer, "TriggerBot", screenWidth - 70, y, 0xFFFFFFFF);
            y += 12;
        }
        if (LitvaClient.killAuraEnabled) {
            context.drawTextWithShadow(mc.textRenderer, "KillAura", screenWidth - 60, y, 0xFFFFFFFF);
            y += 12;
        }

        // Координаты и FPS (слева снизу)
        String coords = String.format("XYZ: %.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());
        String fps = "FPS: " + mc.getCurrentFps();
        context.drawTextWithShadow(mc.textRenderer, coords, 5, screenHeight - 25, 0xFFFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, fps, 5, screenHeight - 15, 0xFFFFFFFF);

        // Target HUD (Информация о цели по центру снизу)
        if (LitvaClient.targetEntity instanceof LivingEntity target) {
            int width = 120;
            int height = 35;
            int x = (screenWidth - width) / 2;
            int targetY = screenHeight - 60; // Позиция над хотбаром

            // Фон
            context.fill(x, targetY, x + width, targetY + height, 0x88000000);

            // Имя цели
            String name = target.getName().getString();
            context.drawTextWithShadow(mc.textRenderer, name, x + 5, targetY + 5, 0xFFFFFFFF);

            // Полоска здоровья
            float health = target.getHealth();
            float maxHealth = target.getMaxHealth();
            float healthPercent = Math.max(0, Math.min(1, health / maxHealth));
            
            // Красная полоска (здоровье)
            context.fill(x + 5, targetY + 20, x + width - 5, targetY + 28, 0xFF333333);
            context.fill(x + 5, targetY + 20, x + 5 + (int)((width - 10) * healthPercent), targetY + 28, 0xFFFF0000);
            
            // Текст ХП (например 20.0)
            String hpText = String.format("%.1f", health);
            context.drawTextWithShadow(mc.textRenderer, hpText, x + width - 25, targetY + 20, 0xFFFFFFFF);
        }
    }
}
