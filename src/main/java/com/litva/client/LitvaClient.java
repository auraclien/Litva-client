package com.litva.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class LitvaClient implements ClientModInitializer {
    public static boolean triggerEnabled = true;
    public static boolean hitPlayers = true;
    public static boolean hitMobs = true;
    public static boolean onlyWeapons = true; // По умолчанию бьем только оружием (как в TriggerBot)
    
    public static boolean killAuraEnabled = false;
    public static boolean kaHitPlayers = true;
    public static boolean kaHitMobs = true;
    public static float kaRange = 3.0f;

    public static boolean showHud = true;
    public static Entity targetEntity = null;

    public static int hudWatermarkX = 5, hudWatermarkY = 5;
    public static int hudInfoX = 5, hudInfoY = 300;
    public static int hudTargetX = 200, hudTargetY = 200;

    private boolean wasRightShiftPressed = false;
    private int attackDelay = 0; // Задержка между ударами (как в их коде)

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            if (client.currentScreen != null) return;

            // Открытие меню
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

            if (attackDelay > 0) attackDelay--;

            // --- TRIGGERBOT ---
            if (triggerEnabled && attackDelay <= 0) {
                if (client.crosshairTarget instanceof EntityHitResult hitResult) {
                    Entity target = hitResult.getEntity();
                    targetEntity = target;
                    
                    if (isValidTarget(client.player, target, hitPlayers, hitMobs)) {
                        // Проверка на оружие (как в их коде)
                        if (onlyWeapons) {
                            String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                            if (!itemName.contains("sword") && !itemName.contains("axe") && !itemName.contains("trident")) {
                                return;
                            }
                        }
                        
                        // Проверка кулдауна (готовность меча)
                        float cooldown = client.player.getAttackCooldownProgress(0.5f);
                        if (cooldown >= 0.9f) {
                            // Если мы на земле, или падаем (для крита) - бьем
                            if (client.player.isOnGround() || client.player.fallDistance > 0.0f) {
                                client.interactionManager.attackEntity(client.player, target);
                                client.player.swingHand(Hand.MAIN_HAND);
                                attackDelay = 10; // Задержка 10 тиков (как у них), чтобы не закликивало
                            }
                        }
                    }
                } else {
                    targetEntity = null;
                }
            }

            // --- KILLAURA (Упрощенная версия) ---
            if (killAuraEnabled && attackDelay <= 0) {
                LivingEntity closestTarget = null;
                double closestDist = kaRange;

                for (Entity entity : client.world.getEntities()) {
                    if (isValidTarget(client.player, entity, kaHitPlayers, kaHitMobs)) {
                        double dist = client.player.distanceTo(entity);
                        if (dist <= closestDist) {
                            closestDist = dist;
                            closestTarget = (LivingEntity) entity;
                        }
                    }
                }

                if (closestTarget != null) {
                    targetEntity = closestTarget;
                    
                    // Наводимся
                    aimSmooth(client.player, closestTarget);

                    // Бьем
                    float cooldown = client.player.getAttackCooldownProgress(0.5f);
                    if (cooldown >= 0.9f && (client.player.isOnGround() || client.player.fallDistance > 0.0f)) {
                        client.interactionManager.attackEntity(client.player, closestTarget);
                        client.player.swingHand(Hand.MAIN_HAND);
                        attackDelay = 10;
                    }
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

    private void aimSmooth(ClientPlayerEntity player, LivingEntity target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double dy = (target.getY() + target.getHeight() / 2) - (player.getY() + player.getEyeHeight(player.getPose()));

        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, distXZ));

        player.setYaw(MathHelper.lerp(0.3f, player.getYaw(), targetYaw));
        player.setPitch(MathHelper.lerp(0.3f, player.getPitch(), targetPitch));
    }

    private boolean isValidTarget(ClientPlayerEntity player, Entity target, boolean hPlayers, boolean hMobs) {
        if (target == player) return false;
        if (target instanceof PlayerEntity) return hPlayers;
        if (target instanceof LivingEntity) return hMobs;
        return false;
    }
}
