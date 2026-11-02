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
    public static boolean triggerEnabled = true;
    public static boolean hitPlayers = true;
    public static boolean hitMobs = true;
    public static boolean onlyWeapons = false;
    public static boolean onlyCrits = false;
    
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
    private static int jumpCooldown = 0;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            if (client.currentScreen != null) return;

            if (GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS) {
                if (!wasRightShiftPressed) {
                    client.setScreen(new LitvaMenu());
                    wasRightShiftPressed = true;
                }
            } else {
                wasRightShiftPressed = false;
            }

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

            if (killAuraEnabled) {
                if (jumpCooldown > 0) jumpCooldown--;
                
                Box searchBox = client.player.getBoundingBox().expand(8.0);
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

                    // Наводка
                    Vec3d aimPoint = target.getPos().add(0, target.getHeight() * 0.85, 0);
                    Vec3d diff = aimPoint.subtract(client.player.getEyePos());
                    double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
                    float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
                    float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distXZ));
                    client.player.setYaw(yaw);
                    client.player.setPitch(MathHelper.clamp(pitch, -90f, 90f));

                    // Имитация нажатия W (идем вперед)
                    if (distSq > attackRangeSq) {
                        client.options.forwardKey.setPressed(true);
                    } else {
                        client.options.forwardKey.setPressed(false);
                    }

                    // Атака
                    boolean canCrit = client.player.fallDistance > 0.0f
                            && !client.player.isOnGround()
                            && !client.player.isClimbing()
                            && !client.player.isTouchingWater()
                            && !client.player.hasVehicle();

                    if (distSq <= attackRangeSq) {
                        if (canCrit) {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(Hand.MAIN_HAND);
                            client.player.fallDistance = 0f;
                        } else if (client.player.isOnGround() && jumpCooldown == 0) {
                            client.player.jump();
                            jumpCooldown = 10;
                        }
                    }
                } else {
                    client.options.forwardKey.setPressed(false);
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

    private boolean isValidTarget(ClientPlayerEntity player, Entity target, boolean hPlayers, boolean hMobs) {
        if (target == player) return false;
        if (target instanceof PlayerEntity) return hPlayers;
        if (target instanceof LivingEntity) return hMobs;
        return false;
    }
}
