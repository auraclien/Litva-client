package com.litva.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public class LitvaClient implements ClientModInitializer {
    public static boolean triggerEnabled = true;
    public static boolean killAuraEnabled = false;
    public static boolean showHud = true;
    public static Entity targetEntity = null;

    public static int hudWatermarkX = 5, hudWatermarkY = 5;
    public static int hudInfoX = 5, hudInfoY = 300;
    public static int hudTargetX = 200, hudTargetY = 200;

    private boolean wasRightShiftPressed = false;
    
    // Переменные для критов (из кода Cat)
    private boolean attackedThisJump = false;
    private double lastVelocityY = 0;
    private boolean peakReached = false;
    private int postPeakTicks = 0;
    private static final int ATTACK_DELAY_TICKS = 2;
    private static final double REACH = 3.0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            if (client.currentScreen != null) return;

            // Меню
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

            // Сброс физики при приземлении
            if (client.player.isOnGround()) {
                attackedThisJump = false;
                lastVelocityY = 0;
                peakReached = false;
                postPeakTicks = 0;
            }

            // --- TRIGGERBOT ---
            if (triggerEnabled) {
                LivingEntity target = findTarget(client);
                if (target != null) {
                    targetEntity = target;
                    executeCritAttack(client, target);
                } else {
                    if (!killAuraEnabled) targetEntity = null;
                }
            }

            // --- KILLAURA ---
            if (killAuraEnabled) {
                LivingEntity target = findClosestTarget(client);
                if (target != null) {
                    targetEntity = target;
                    aimSmooth(client.player, target);
                    executeCritAttack(client, target);
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (showHud && MinecraftClient.getInstance().player != null) {
                LitvaHud.render(drawContext); 
            }
        });
    }

    // --- ЛОГИКА КРИТОВ ОТ CAT ---
    private void executeCritAttack(MinecraftClient client, LivingEntity target) {
        if (client.player == null || client.interactionManager == null) return;

        // Проверка кулдауна
        float cooldown = client.player.getAttackCooldownProgress(0.5F);
        if (cooldown < 0.9F) return;

        // Проверка дистанции
        double distanceSq = client.player.squaredDistanceTo(target);
        if (distanceSq > REACH * REACH) return;

        // Если стоим на земле - прыгаем и НЕ БЬЕМ
        boolean canCrit = !client.player.isOnGround() &&
                          !client.player.isTouchingWater() &&
                          !client.player.isClimbing();

        if (!canCrit) {
            if (client.player.isOnGround()) {
                client.player.jump();
            }
            return;
        }

        if (attackedThisJump) return;

        // Логика пика прыжка
        double currentVelY = client.player.getVelocity().y;
        boolean isPeak = lastVelocityY > 0 && currentVelY <= 0;

        if (isPeak) {
            peakReached = true;
            postPeakTicks = 0;
        }

        if (peakReached) {
            postPeakTicks++;
        }

        lastVelocityY = currentVelY;

        // Ждем 2 тика после пика и бьем
        if (!peakReached || postPeakTicks < ATTACK_DELAY_TICKS) return;

        // Наводимся в момент удара (чтобы попасть)
        aimInstant(client.player, target);
        
        client.interactionManager.attackEntity(client.player, target);
        client.player.swingHand(Hand.MAIN_HAND);
        
        attackedThisJump = true;
        peakReached = false;
        postPeakTicks = 0;
    }

    // --- ПОИСК ЦЕЛИ (как в CritTrigger) ---
    private LivingEntity findTarget(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return null;

        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0f);
        Box searchBox = player.getBoundingBox().expand(REACH);
        
        List<LivingEntity> targets = client.world.getEntitiesByClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStandEntity));

        return targets.stream()
                .filter(e -> {
                    // FOV (смотрим ли мы на него)
                    Vec3d toTarget = e.getEyePos().subtract(eye).normalize();
                    double dot = look.dotProduct(toTarget);
                    double angle = Math.toDegrees(Math.acos(MathHelper.clamp(dot, -1.0, 1.0)));
                    if (angle > 60.0 / 2.0) return false; // FOV 60 градусов

                    // Проверка стен (рейкаст)
                    Vec3d hit = e.getPos().add(0, e.getHeight() * 0.5, 0);
                    var blockHit = client.world.raycast(new RaycastContext(
                            eye, hit,
                            RaycastContext.ShapeType.COLLIDER,
                            RaycastContext.FluidHandling.NONE,
                            player));
                    return blockHit.getType() == HitResult.Type.MISS;
                })
                .min(Comparator.comparingDouble(e -> {
                    Vec3d to = e.getEyePos().subtract(eye).normalize();
                    return -look.dotProduct(to);
                }))
                .orElse(null);
    }

    // --- KillAura поиск ---
    private LivingEntity findClosestTarget(MinecraftClient client) {
        LivingEntity closest = null;
        double closestDist = REACH;
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof LivingEntity le && entity != client.player && le.isAlive()) {
                double dist = client.player.distanceTo(entity);
                if (dist <= closestDist) {
                    closestDist = dist;
                    closest = le;
                }
            }
        }
        return closest;
    }

    private void aimInstant(ClientPlayerEntity player, LivingEntity target) {
        Vec3d eye = player.getEyePos();
        Vec3d aimPoint = target.getPos().add(0, target.getHeight() * 0.85, 0);
        Vec3d diff = aimPoint.subtract(eye);
        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distXZ));

        player.setYaw(yaw);
        player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    private void aimSmooth(ClientPlayerEntity player, LivingEntity target) {
        Vec3d eye = player.getEyePos();
        Vec3d aimPoint = target.getPos().add(0, target.getHeight() * 0.85, 0);
        Vec3d diff = aimPoint.subtract(eye);
        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);

        float targetYaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float targetPitch = (float) -Math.toDegrees(Math.atan2(diff.y, distXZ));

        player.setYaw(MathHelper.lerp(0.3f, player.getYaw(), targetYaw));
        player.setPitch(MathHelper.lerp(0.3f, player.getPitch(), targetPitch));
    }
}
