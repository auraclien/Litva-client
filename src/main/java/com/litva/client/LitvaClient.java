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
    public static Entity targetEntity = null;

    // Координаты HUD
    public static int hudWatermarkX = 5, hudWatermarkY = 5;
    public static int hudInfoX = 5, hudInfoY = 300;
    public static int hudTargetX = 200, hudTargetY = 200;

    private boolean wasRightShiftPressed = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Открытие меню на Right Shift
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed && client.currentScreen == null) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

            boolean isCooldownReady = client.player.getAttackCooldownProgress(0.5f) >= 1.0f;

            // --- TRIGGERBOT ---
            if (triggerEnabled && isCooldownReady) {
                if (client.crosshairTarget instanceof EntityHitResult hitResult) {
                    Entity target = hitResult.getEntity();
                    targetEntity = target;
                    
                    if (isValidTarget(client.player, target, hitPlayers, hitMobs)) {
                        if (onlyWeapons) {
                            String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                            if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                        }
                        
                        client.interactionManager.attackEntity(client.player, target);
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                } else {
                    targetEntity = null;
                }
            }

            // --- KILLAURA ---
            if (killAuraEnabled && isCooldownReady) {
                Entity closestTarget = null;
                double closestDist = kaRange;

                for (Entity entity : client.world.getEntities()) {
                    if (isValidTarget(client.player, entity, kaHitPlayers, kaHitMobs)) {
                        double dist = client.player.distanceTo(entity);
                        if (dist <= closestDist) {
                            closestDist = dist;
                            closestTarget = entity;
                        }
                    }
                }

                if (closestTarget != null) {
                    targetEntity = closestTarget;
                    
                    if (kaOnlyWeapons) {
                        String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                        if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                    }

                    client.interactionManager.attackEntity(client.player, closestTarget);
                    client.player.swingHand(Hand.MAIN_HAND);
                } else {
                    if (!triggerEnabled) targetEntity = null;
                }
            }
        });

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
