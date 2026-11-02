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

        // WATERMARK
        context.drawTextWithShadow(mc.textRenderer, "Litva Client", LitvaClient.hudWatermarkX, LitvaClient.hudWatermarkY, 0xFFAA0000);

        // СПИСОК МОДУЛЕЙ
        int moduleY = LitvaClient.hudWatermarkY + 15;
        if (LitvaClient.triggerEnabled) {
            context.drawTextWithShadow(mc.textRenderer, "TriggerBot", LitvaClient.hudWatermarkX, moduleY, 0xFFFFFFFF);
            moduleY += 12;
        }
        if (LitvaClient.killAuraEnabled) {
            context.drawTextWithShadow(mc.textRenderer, "KillAura", LitvaClient.hudWatermarkX, moduleY, 0xFFFFFFFF);
            moduleY += 12;
        }

        // КООРДИНАТЫ И FPS
        String coords = String.format("XYZ: %.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());
        String fps = "FPS: " + mc.getCurrentFps();
        context.drawTextWithShadow(mc.textRenderer, coords, LitvaClient.hudInfoX, LitvaClient.hudInfoY, 0xFFFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, fps, LitvaClient.hudInfoX, LitvaClient.hudInfoY + 10, 0xFFFFFFFF);

        // TARGET HUD
        if (LitvaClient.targetEntity instanceof LivingEntity target) {
            int width = 130;
            int height = 40;
            int x = LitvaClient.hudTargetX;
            int targetY = LitvaClient.hudTargetY;

            context.fill(x, targetY, x + width, targetY + height, 0x88000000);

            // Иконка (заглушка)
            if (target instanceof PlayerEntity) {
                context.fill(x + 5, targetY + 5, x + 29, targetY + 29, 0xFFAA0000);
            } else {
                context.fill(x + 5, targetY + 5, x + 29, targetY + 29, 0xFF00AA00);
            }

            String name = target.getName().getString();
            context.drawTextWithShadow(mc.textRenderer, name, x + 35, targetY + 5, 0xFFFFFFFF);

            float health = target.getHealth();
            float maxHealth = target.getMaxHealth();
            float healthPercent = Math.max(0, Math.min(1, health / maxHealth));
            
            context.fill(x + 35, targetY + 20, x + width - 5, targetY + 28, 0xFF333333);
            context.fill(x + 35, targetY + 20, x + 35 + (int)((width - 40) * healthPercent), targetY + 28, 0xFFFF0000);
            
            String hpText = String.format("%.1f", health);
            context.drawTextWithShadow(mc.textRenderer, hpText, x + width - 25, targetY + 20, 0xFFFFFFFF);
        }
    }
}
