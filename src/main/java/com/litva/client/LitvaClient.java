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
    public static boolean triggerEnabled = true;
    public static boolean hitPlayers = true;
    public static boolean hitMobs = true;
    public static boolean onlyWeapons = false;
    public static boolean onlyCrits = false;
    
    public static boolean killAuraEnabled = false;
    public static boolean kaHitPlayers = true;
    public static boolean kaHitMobs = true;
    public static boolean kaOnlyWeapons = false;
    public static boolean kaOnlyCrits = false;
    public static float kaRange = 3.0f;

    public static boolean showHud = true;

    private boolean wasRightShiftPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed && client.currentScreen == null) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

            boolean isCooldownReady = client.player.getAttackCooldownProgress(0.5f) >= 1.0f;

            if (triggerEnabled && isCooldownReady) {
                if (client.crosshairTarget instanceof EntityHitResult hitResult) {
                    Entity target = hitResult.getEntity();
                    if (isValidTarget(client.player, target, hitPlayers, hitMobs)) {
                        if (onlyWeapons) {
                            String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                            if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                        }
                        
                        if (onlyCrits) {
                            if (client.player.fallDistance > 0.1f && !client.player.isOnGround()) {
                                client.interactionManager.attackEntity(client.player, target);
                                client.player.swingHand(Hand.MAIN_HAND);
                            } else if (client.player.isOnGround()) {
                                client.player.jump();
                            }
                        } else {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                    }
                }
            }

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
                                    client.interactionManager.attackEntity(client.player, entity);
                                    client.player.swingHand(Hand.MAIN_HAND);
                                } else if (client.player.isOnGround()) {
                                    client.player.jump();
                                }
                            } else {
                                client.interactionManager.attackEntity(client.player, entity);
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                            break; 
                        }
                    }
                }
            }
        });

        // Исправленный HUD (убрали tickDelta)
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (showHud && MinecraftClient.getInstance().player != null) {
                LitvaHud.render(drawContext); 
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
