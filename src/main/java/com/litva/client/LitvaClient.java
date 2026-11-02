package com.litva.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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
    
    // Переменные из кода OpenCode
    private int attackDelay = 0;
    private int jumpDelay = 0;

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

            if (attackDelay > 0) attackDelay--;
            if (jumpDelay > 0) jumpDelay--;

            // --- TRIGGERBOT ---
            if (triggerEnabled) {
                PlayerEntity target = findTarget(client);
                if (target != null) {
                    targetEntity = target;
                    if (onlyWeapons) {
                        String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                        if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                    }

                    if (onlyCrits) {
                        handleCrits(client, target, client.player);
                    } else {
                        if (client.player.getAttackCooldownProgress(0.5f) >= 1.0f) {
                            client.interactionManager.attackEntity(client.player, target);
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                    }
                } else {
                    targetEntity = null;
                }
            }

            // --- KILLAURA (С СИЛЕНТ АИМОМ И КРИТАМИ) ---
            if (killAuraEnabled) {
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

                    // Бег за целью
                    if (distSq > attackRangeSq) {
                        client.options.forwardKey.setPressed(true);
                    } else {
                        client.options.forwardKey.setPressed(false);
                    }

                    if (kaOnlyWeapons) {
                        String itemName = client.player.getMainHandStack().getItem().getTranslationKey();
                        if (!itemName.contains("sword") && !itemName.contains("axe")) return;
                    }

                    if (distSq <= attackRangeSq) {
                        // Применяем логику критов и наводки
                        handleCrits(client, target, client.player);
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

    // --- ЛОГИКА КРИТОВ ОТ OPENCODE ---
    private void handleCrits(MinecraftClient client, LivingEntity target, ClientPlayerEntity player) {
        if (canCrit(player)) {
            if (attackDelay <= 0) {
                // 1. Отключаем спринт
                stopSprint(client, player);
                
                // 2. Делаем невидимую наводку
                float savedYaw = player.getYaw();
                float savedPitch = player.getPitch();
                aimServerSide(player, target);
                
                // 3. Бьем
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);
                
                // 4. Возвращаем камеру
                player.setYaw(savedYaw);
                player.setPitch(savedPitch);

                attackDelay = attackInterval(player);
            }
            return;
        }

        // Если бить нельзя (не в падении) - прыгаем
        if (jumpDelay <= 0 && player.isOnGround()
                && !player.isTouchingWater() && !player.isInLava()
                && attackDelay <= 6) {
            player.jump();
            jumpDelay = 10;
        }
    }

    private static void stopSprint(MinecraftClient client, ClientPlayerEntity player) {
        if (!player.isSprinting()) return;
        player.setSprinting(false);
        client.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
    }

    private static boolean canCrit(ClientPlayerEntity player) {
        return player.fallDistance > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.hasVehicle()
                && !player.hasStatusEffect(StatusEffects.BLINDNESS)
                && !player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                && !player.hasStatusEffect(StatusEffects.LEVITATION)
                && player.getAttackCooldownProgress(0.5F) >= 0.9F;
    }

    private static int attackInterval(ClientPlayerEntity player) {
        double speed = player.getAttributeValue(EntityAttributes.ATTACK_SPEED);
        if (speed < 0.1D) speed = 4.0D;
        return Math.max(1, (int) Math.ceil(20.0D / speed));
    }

    private void aimServerSide(ClientPlayerEntity player, LivingEntity target) {
        Vec3d aimPoint = target.getPos().add(0, target.getHeight() * 0.85, 0);
        Vec3d diff = aimPoint.subtract(player.getEyePos());
        double distXZ = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, distXZ));
        player.setYaw(yaw);
        player.setPitch(MathHelper.clamp(pitch, -90f, 90f));
    }

    private static PlayerEntity findTarget(MinecraftClient client) {
        HitResult hit = client.crosshairTarget;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return null;
        Entity entity = ((EntityHitResult) hit).getEntity();
        if (!(entity instanceof PlayerEntity target)) return null;
        if (target == client.player || !target.isAlive() || target.isSpectator()) return null;
        return target;
    }

    private boolean isValidTarget(ClientPlayerEntity player, Entity target, boolean hPlayers, boolean hMobs) {
        if (target == player) return false;
        if (target instanceof PlayerEntity) return hPlayers;
        if (target instanceof LivingEntity) return hMobs;
        return false;
    }
}
