package com.litva.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

public class LitvaClient implements ClientModInitializer {
    public static boolean triggerBotEnabled = true;
    private boolean wasPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Включаем/выключаем TriggerBot на клавишу "R"
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS) {
                if (!wasPressed) {
                    triggerBotEnabled = !triggerBotEnabled;
                    client.player.sendMessage(net.minecraft.text.Text.literal("Litva Client | TriggerBot: " + (triggerBotEnabled ? "ON" : "OFF")), true);
                    wasPressed = true;
                }
            } else {
                wasPressed = false;
            }

            // Логика TriggerBot
            if (triggerBotEnabled && client.crosshairTarget instanceof EntityHitResult hitResult) {
                Entity target = hitResult.getEntity();
                if (target instanceof LivingEntity && target != client.player) {
                    if (client.player.distanceTo(target) <= 3.0) {
                        client.interactionManager.attackEntity(client.player, target);
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                }
            }
        });
    }
}
