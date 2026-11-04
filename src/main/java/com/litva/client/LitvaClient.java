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
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class LitvaClient implements ClientModInitializer {
    public static boolean triggerEnabled = true;
    public static boolean onlyCrits = true; 
    public static boolean killAuraEnabled = false;
    public static boolean kaHitPlayers = true;
    public static boolean kaHitMobs = true;
    public static boolean kaOnlyWeapons = true;
    public static float kaRange = 3.0f;

    public static boolean showHud = true;
    public static Entity targetEntity = null;

    public static int hudWatermarkX = 5, hudWatermarkY = 5;
    public static int hudInfoX = 5, hudInfoY = 300;
    public static int hudTargetX = 200, hudTargetY = 200;

    private boolean wasRightShiftPressed = false;
    private int kaDelay = 0;

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

            if (kaDelay > 0) kaDelay--;

            // --- TRIGGERBOT ---
            if (triggerEnabled && client.crosshairTarget instanceof EntityHitResult hitResult) {
                if (hitResult.getEntity() instanceof LivingEntity target && target != client.player && canAttack(client, target)) {
                    client.interactionManager.attackEntity(client.player, target);
                    client.player.swingHand(Hand.MAIN_HAND);
                }
            }

            // --- KILLAURA ---
            if (killAuraEnabled && kaDelay <= 0) {
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

                    if (canAttack(client, closestTarget)) {
                        client.interactionManager.attackEntity(client.player, closestTarget);
                        client.player.swingHand(Hand.MAIN_HAND);
                        kaDelay = 10; 
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

    private boolean canAttack(MinecraftClient mc, LivingEntity target) {
        if (target == null || target == mc.player || !target.isAttackable() || target.isDead()) return false;

        if (kaOnlyWeapons && !isSwordInHand(mc)) return false;

        if (mc.player.getAttackCooldownProgress(0.5F) < 0.93F) return false;

        boolean reasonForAttack = mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
                || mc.player.isClimbing()
                || (mc.player.isTouchingWater() && mc.player.isSubmergedIn(FluidTags.WATER))
                || mc.player.hasVehicle()
                || mc.player.getAbilities().flying;

        if (!reasonForAttack && onlyCrits) {
            return !mc.player.isOnGround() && mc.player.fallDistance > 0.0F;
        }

        return !reasonForAttack;
    }

    private boolean isSwordInHand(MinecraftClient mc) {
        ItemStack mainHandItem = mc.player.getMainHandStack();
        return mainHandItem.getItem() == Items.WOODEN_SWORD || mainHandItem.getItem() == Items.STONE_SWORD
                || mainHandItem.getItem() == Items.IRON_SWORD || mainHandItem.getItem() == Items.GOLDEN_SWORD
                || mainHandItem.getItem() == Items.DIAMOND_SWORD || mainHandItem.getItem() == Items.NETHERITE_SWORD;
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
