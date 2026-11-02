package com.litva.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
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
    public static boolean kaOnlyWeapons = false;
    public static boolean kaOnlyCrits = false;
    public static float kaRange = 3.0f;

    // Настройки HUD
    public static boolean showHud = true;

    private boolean wasRightShiftPressed = false;

    @Override
    public void onInitializeClient() {
        // Логика ударов
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Открытие меню
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed && client.currentScreen == null) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

            // Проверка кулдауна (перезарядки меча)
            boolean isCooldownReady = client.player.getAttackCooldownProgress(0.5f) >= 1.0f;

            // --- ЛОГИКА TRIGGERBOT ---
            if (triggerEnabled && isCooldownReady) {
                if (client.crosshairTarget instanceof EntityHitResult hitResult) {
                    Entity target = hitResult.getEntity();
                    if (isValidTarget(client.player, target, hitPlayers, hitMobs)) {
                        if (onlyWeapons) {
                            String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                            if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                        }
                        
                        if (onlyCrits) {
                            // Умные криты: бьем только когда летим вниз
                            if (client.player.fallDistance > 0.1f && !client.player.isOnGround()) {
                                attack(client.player, target);
                            } else if (client.player.isOnGround()) {
                                client.player.jump(); // Прыгаем, чтобы подготовить крит
                            }
                        } else {
                            attack(client.player, target);
                        }
                    }
                }
            }

            // --- ЛОГИКА KILLAURA ---
            if (killAuraEnabled && isCooldownReady) {
                for (Entity entity : client.world.getEntities()) {
                    if (isValidTarget(client.player, entity, kaHitPlayers, kaHitMobs)) {
                        if (client.player.distanceTo(entity) <= kaRange) {
                            if (kaOnlyWeapons) {
                                String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                                if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                            }

                            if (kaOnlyCrits) {
                                if (client.player.fallDistance > 0.1f && !client.player.isOnGround()) {
                                    attack(client.player, entity);
                                } else if (client.player.isOnGround()) {
                                    client.player.jump();
                                }
                            } else {
                                attack(client.player, entity);
                            }
                            break; 
                        }
                    }
                }
            }
        });

        // Логика отрисовки HUD
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (showHud && MinecraftClient.getInstance().player != null) {
                LitvaHud.render(drawContext, tickDelta);
            }
        });
    }

    private void attack(net.minecraft.client.network.ClientPlayerEntity player, Entity target) {
        player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket(target.getId(), false));
        player.swingHand(Hand.MAIN_HAND);
    }

    private boolean isValidTarget(net.minecraft.client.network.ClientPlayerEntity player, Entity target, boolean hPlayers, boolean hMobs) {
        if (target == player) return false;
        if (target instanceof PlayerEntity) return hPlayers;
        if (target instanceof LivingEntity) return hMobs;
        return false;
    }
}
