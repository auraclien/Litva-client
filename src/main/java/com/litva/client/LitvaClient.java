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
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.Random;

public class LitvaClient implements ClientModInitializer {
    public static boolean triggerEnabled = true;
    public static boolean onlyCrits = true; 
    public static boolean killAuraEnabled = false;
    public static boolean kaHitPlayers = true;
    public static boolean kaHitMobs = true;
    public static boolean kaOnlyWeapons = false;
    public static float kaRange = 3.0f;

    public static boolean showHud = true;
    public static Entity targetEntity = null;

    public static int hudWatermarkX = 5, hudWatermarkY = 5;
    public static int hudInfoX = 5, hudInfoY = 300;
    public static int hudTargetX = 200, hudTargetY = 200;

    private boolean wasRightShiftPressed = false;
    private int attackTimer = 0;
    private int jumpTimer = 0;
    private final Random random = new Random();

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

            if (attackTimer > 0) attackTimer--;
            if (jumpTimer > 0) jumpTimer--;

            // --- TRIGGERBOT (НОВАЯ ЛОГИКА) ---
            if (triggerEnabled) {
                Entity closestTarget = null;
                double closestDist = kaRange;

                // Ищем цель в радиусе (3 блока)
                for (Entity entity : client.world.getEntities()) {
                    if (entity instanceof LivingEntity le && entity != client.player && le.isAlive()) {
                        if (entity instanceof PlayerEntity && !kaHitPlayers) continue;
                        if (entity instanceof MobEntity && !kaHitMobs) continue;
                        
                        double dist = client.player.distanceTo(entity);
                        if (dist <= closestDist) {
                            // Проверяем, смотрим ли мы на него (чтобы не бить сквозь стены и за спиной)
                            if (isLookingAt(client.player, le)) {
                                closestDist = dist;
                                closestTarget = le;
                            }
                        }
                    }
                }

                if (closestTarget != null) {
                    targetEntity = closestTarget;
                    handleAttack(client, (LivingEntity) closestTarget);
                } else {
                    if (!killAuraEnabled) targetEntity = null;
                }
            }

            // --- KILLAURA (Бьет всех в радиусе) ---
            if (killAuraEnabled) {
                LivingEntity closestTarget = null;
                double closestDist = kaRange;

                for (Entity entity : client.world.getEntities()) {
                    if (entity instanceof LivingEntity le && entity != client.player && le.isAlive()) {
                        if (entity instanceof PlayerEntity && !kaHitPlayers) continue;
                        if (entity instanceof MobEntity && !kaHitMobs) continue;
                        
                        double dist = client.player.distanceTo(entity);
                        if (dist <= closestDist) {
                            closestDist = dist;
                            closestTarget = le;
                        }
                    }
                }

                if (closestTarget != null) {
                    targetEntity = closestTarget;
                    aimSmooth(client.player, closestTarget);
                    handleAttack(client, closestTarget);
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

    // Проверка, смотрит ли игрок на цель (с погрешностью 10 градусов)
    private boolean isLookingAt(ClientPlayerEntity player, Entity target) {
        Vec3d eyePos = player.getEyePos();
        Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2, 0);
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d toTargetVec = targetPos.subtract(eyePos).normalize();
        
        double dot = lookVec.dotProduct(toTargetVec);
        return dot > 0.98; // Чем ближе к 1.0, тем точнее нужно смотреть
    }

    private void handleAttack(MinecraftClient client, LivingEntity target) {
        if (attackTimer > 0) return;

        if (kaOnlyWeapons) {
            String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
            if (!itemName.contains("sword") && !itemName.contains("axe")) return;
        }

        // Если включены криты, прыгаем и ждем падения
        if (onlyCrits) {
            if (client.player.isOnGround() && jumpTimer == 0) {
                client.player.jump();
                jumpTimer = 10; 
                attackTimer = 2; 
                return;
            }

            if (!client.player.isOnGround() && client.player.fallDistance > 0.0F) {
                client.interactionManager.attackEntity(client.player, target);
                client.player.swingHand(Hand.MAIN_HAND);
                attackTimer = 10; 
            }
        } else {
            // Обычный удар
            client.interactionManager.attackEntity(client.player, target);
            client.player.swingHand(Hand.MAIN_HAND);
            attackTimer = 10;
        }
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
}
