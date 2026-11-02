package com.litva.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class LitvaClient implements ClientModInitializer {
    // Настройки TriggerBot
    public static boolean triggerEnabled = true;
    public static boolean hitPlayers = true;
    public static boolean hitMobs = true;
    public static boolean onlyWeapons = false;
    public static boolean onlyCrits = false;
    
    // Настройки KillAura
    public static boolean killAuraEnabled = false;
    public static boolean kaHitPlayers = true;
    public static boolean kaHitMobs = true;
    public static float kaRange = 3.0f;

    private int triggerTimer = 0;
    private int killAuraTimer = 0;
    private boolean wasRightShiftPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Открытие меню по Right Shift
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed && client.currentScreen == null) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

            // Таймеры для задержки (защита от закликивания)
            if (triggerTimer > 0) triggerTimer--;
            if (killAuraTimer > 0) killAuraTimer--;

            // --- ЛОГИКА TRIGGERBOT ---
            if (triggerEnabled && triggerTimer <= 0) {
                if (client.crosshairTarget instanceof EntityHitResult hitResult) {
                    Entity target = hitResult.getEntity();
                    if (isValidTarget(client.player, target, hitPlayers, hitMobs)) {
                        if (onlyWeapons) {
                            String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                            if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                        }
                        // Имитация критов (прыжок перед ударом, если включено)
                        if (onlyCrits) {
                            if (client.player.isOnGround()) {
                                client.player.jump();
                            } else if (client.player.fallDistance > 0.1f) {
                                client.interactionManager.attackEntity(client.player, target);
                                client.player.swingHand(Hand.MAIN_HAND);
                                triggerTimer = 5; // Фиксированная задержка 5 тиков (0.25 сек), чтобы не закликивало
                            }
                        } else {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(Hand.MAIN_HAND);
                            triggerTimer = 5;
                        }
                    }
                }
            }

            // --- ЛОГИКА KILLAURA (Только базовая) ---
            if (killAuraEnabled && killAuraTimer <= 0) {
                for (Entity entity : client.world.getEntities()) {
                    if (isValidTarget(client.player, entity, kaHitPlayers, kaHitMobs)) {
                        if (client.player.distanceTo(entity) <= kaRange) {
                            client.interactionManager.attackEntity(client.player, entity);
                            client.player.swingHand(Hand.MAIN_HAND);
                            killAuraTimer = 10; // Задержка удара KillAura
                            break; // Бьем только одну цель
                        }
                    }
                }
            }
        });
    }

    private boolean isValidTarget(net.minecraft.client.network.ClientPlayerEntity player, Entity target, boolean hPlayers, boolean hMobs) {
        if (target == player) return false;
        if (target instanceof PlayerEntity) return hPlayers;
        if (target instanceof LivingEntity) return hMobs;
        return false;
    }
}
