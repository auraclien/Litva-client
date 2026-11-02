package com.litva.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class LitvaClient implements ClientModInitializer {
    public static boolean enabled = true;
    public static boolean hitPlayers = true;
    public static boolean hitMobs = true;
    public static boolean onlyCrits = false;
    public static boolean onlyWeapons = false;
    public static int delay = 2; 

    private int clickTimer = 0;
    private boolean wasRightShiftPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Открытие меню по Right Shift (с проверкой, чтобы не спамило)
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed && client.currentScreen == null) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

            // Включение/выключение через R
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS) {
                if (clickTimer <= 0) {
                    enabled = !enabled;
                    client.player.sendMessage(Text.literal("Litva Client | TriggerBot: " + (enabled ? "ON" : "OFF")), true);
                    clickTimer = 10; 
                }
            }

            if (clickTimer > 0) {
                clickTimer--;
                return;
            }

            if (!enabled) return;

            // Логика TriggerBot
            if (client.crosshairTarget instanceof EntityHitResult hitResult) {
                Entity target = hitResult.getEntity();
                
                // Проверка на игрока
                if (target instanceof PlayerEntity && target != client.player) {
                    if (!hitPlayers) return;
                }
                // Проверка на мобов
                else if (target instanceof LivingEntity && !(target instanceof PlayerEntity)) {
                    if (!hitMobs) return;
                } else {
                    return; 
                }

                // Проверка на оружие
                if (onlyWeapons) {
                    String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                    if (!itemName.contains("sword") && !itemName.contains("axe")) {
                        return;
                    }
                }

                // Проверка на криты
                if (onlyCrits) {
                    if (client.player.fallDistance <= 0.0F || client.player.isOnGround() || client.player.isInLava() || client.player.isSubmergedInWater()) {
                        return;
                    }
                }

                // Проверка дистанции и удар
                if (client.player.distanceTo(target) <= 3.0) {
                    client.interactionManager.attackEntity(client.player, target);
                    client.player.swingHand(Hand.MAIN_HAND);
                    clickTimer = delay;
                }
            }
        });
    }
}
