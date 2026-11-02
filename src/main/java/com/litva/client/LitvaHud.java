package com.litva.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

public class LitvaHud {
    // Убрали tickDelta
    public static void render(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int y = 10;

        context.drawTextWithShadow(mc.textRenderer, "Litva Client", 5, 5, 0xFFAA0000);

        if (LitvaClient.triggerEnabled) {
            context.drawTextWithShadow(mc.textRenderer, "TriggerBot", screenWidth - 70, y, 0xFFFFFFFF);
            y += 12;
        }
        if (LitvaClient.killAuraEnabled) {
            context.drawTextWithShadow(mc.textRenderer, "KillAura", screenWidth - 60, y, 0xFFFFFFFF);
            y += 12;
        }

        String coords = String.format("XYZ: %.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ());
        String fps = "FPS: " + mc.getCurrentFps();
        
        context.drawTextWithShadow(mc.textRenderer, coords, 5, mc.getWindow().getScaledHeight() - 25, 0xFFFFFFFF);
        context.drawTextWithShadow(mc.textRenderer, fps, 5, mc.getWindow().getScaledHeight() - 15, 0xFFFFFFFF);
    }
}
