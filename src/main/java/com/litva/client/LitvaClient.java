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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public class LitvaClient implements ClientModInitializer {
    // Настройки TriggerBot
    public static boolean triggerEnabled = true;
    public static boolean hitPlayers = true;
    public static boolean hitMobs = true;
    public static boolean onlyWeapons = false;
    public static boolean onlyCrits = false;
    
    // Настройки KillAura (всегда включена, бьет критами)
    public static boolean killAuraEnabled = false;
    public static boolean kaHitPlayers = true;
    public static boolean kaHitMobs = true;
    public static boolean kaOnlyWeapons = false;
    public static float kaRange = 3.0f;

    public static boolean showHud = true;
    public static Entity targetEntity = null;

    // Координаты HUD
    public static int hudWatermarkX = 5, hudWatermarkY = 5;
    public static int hudInfoX = 5, hudInfoY = 300;
    public static int hudTargetX = 200, hudTargetY = 200;

    private boolean wasRightShiftPressed = false;
    private static int jumpCooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            if (client.currentScreen != null) return; // Не работаем, если открыто меню/чат

            // Открытие меню на Right Shift
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

            // --- ЛОГИКА TRIGGERBOT ---
            if (triggerEnabled) {
                boolean isCooldownReady = client.player.getAttackCooldownProgress(0.5f) >= 1.0f;
                if (isCooldownReady) {
                    if (client.crosshairTarget instanceof EntityHitResult hitResult) {
                        Entity target = hitResult.getEntity();
                        targetEntity = target;
                        
                        if (isValidTarget(client.player, target, hitPlayers, hitMobs)) {
                            if (onlyWeapons) {
                                String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                                if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                            }
                            
                            if (onlyCrits) {
                                if (client.player.fallDistance > 0.0f && !client.player.isOnGround()) {
                                    client.interactionManager.attackEntity(client.player, target);
                                    client.player.swingHand(Hand.MAIN_HAND);
                                } else if (client.player.isOnGround() && jumpCooldown == 0) {
                                    client.player.jump();
                                    jumpCooldown = 10;
                                }
                            } else {
                                client.interactionManager.attackEntity(client.player, target);
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                        }
                    } else {
                        targetEntity = null;
                    }
                }
            }

            // --- ЛОГИКА KILLAURA (Твоя новая) ---
            if (killAuraEnabled) {
                if (jumpCooldown > 0) jumpCooldown--;
                
                Box searchBox = client.player.getBoundingBox().expand(8.0); // Ищем в радиусе 8 блоков
                List<LivingEntity> targets = client.world.getEntitiesByClass(LivingEntity.class, searchBox,
                        e -> e != client.player && e.isAlive() && !e.isSpectator()
                                && ((e instanceof PlayerEntity && kaHitPlayers) || (e instanceof MobEntity && kaHitMobs)));

                LivingEntity target = targets.stream()
                        .min(Comparator.comparingDouble(e -> e.squaredDistanceTo(client.player)))
                        .orElse(null);

                if (target != null) {
                    targetEntity = target;
                    double distSq = client.player.squaredDistanceTo(target);
                    double attackRangeSq = kaRange * kaRange;

                    // Наводка (Aim) - доворачиваем камеру
                    Vec3d aimPoint = target.getPos().add(0, target.getHeight() * 0.85, 0);
                    Vec3d diff = aimPoint.subtract(client.player.getEyePos());
                    double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
                    float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
                    float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distXZ));
                    client.player.setYaw(yaw);
                    client.player.setPitch(MathHelper.clamp(pitch, -90f, 90f));

                    // Бежим за целью
                    if (distSq > attackRangeSq) {
                        followTarget(client.player, target);
                    } else {
                        client.player.setForwardSpeed(0f);
                        client.player.setSidewaysSpeed(0f);
                    }

                    // Атака только критом
                    boolean canCrit = client.player.fallDistance > 0.0f
                            && !client.player.isOnGround()
                            && !client.player.isClimbing()
                            && !client.player.isTouchingWater()
                            && !client.player.hasVehicle();

                    if (distSq <= attackRangeSq) {
                        if (canCrit) {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(Hand.MAIN_HAND);
                            client.player.fallDistance = 0f; // Сбрасываем, чтобы не закликивало
                        } else if (client.player.isOnGround() && jumpCooldown == 0) {
                            client.player.jump();
                            jumpCooldown = 10;
                        }
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

    private static void followTarget(ClientPlayerEntity player, LivingEntity target) {
        Vec3d toTarget = target.getPos().subtract(player.getPos());
        double dist = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
        if (dist < 0.01) return;

        double dirX = toTarget.x / dist;
        double dirZ = toTarget.z / dist;

        float yawRad = (float) Math.toRadians(player.getYaw());
        double forwardX = -MathHelper.sin(yawRad);
        double forwardZ = MathHelper.cos(yawRad);
        double rightX = MathHelper.cos(yawRad);
        double rightZ = MathHelper.sin(yawRad);

        float forward = (float) (dirX * forwardX + dirZ * forwardZ);
        float sideways = (float) (dirX * rightX + dirZ * rightZ);

        player.setForwardSpeed(forward > 0 ? 1.0f : 0f);
        player.setSidewaysSpeed(sideways);

        if (player.horizontalCollision && player.isOnGround()) {
            player.jump();
        }
    }

    private boolean isValidTarget(ClientPlayerEntity player, Entity target, boolean hPlayers, boolean hMobs) {
        if (target == player) return false;
        if (target instanceof PlayerEntity) return hPlayers;
        if (target instanceof LivingEntity) return hMobs;
        return false;
    }
}
