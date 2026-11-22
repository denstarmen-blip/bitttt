package com.example.carcass.client;

import com.example.carcass.CorpseBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.WeakHashMap;

/** Малює вбитого моба, що лежить на боці (як при смерті у ванілі). */
public class CorpseRenderer implements BlockEntityRenderer<CorpseBlockEntity> {
    private static class Holder {
        String id;
        Entity entity; // null, якщо створити не вдалось
    }

    private final EntityRenderDispatcher dispatcher;
    private final Map<CorpseBlockEntity, Holder> cache = new WeakHashMap<>();

    public CorpseRenderer(BlockEntityRendererProvider.Context ctx) {
        this.dispatcher = ctx.getEntityRenderer();
    }

    @Override
    public void render(CorpseBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        String id = be.getMobType();
        Level level = be.getLevel();
        if (id == null || id.isEmpty() || level == null) return;

        Holder holder = cache.get(be);
        if (holder == null || !id.equals(holder.id) || (holder.entity != null && holder.entity.level() != level)) {
            holder = new Holder();
            holder.id = id;
            holder.entity = createDummy(be, level);
            cache.put(be, holder);
        }
        if (holder.entity == null) return;

        try {
            poseStack.pushPose();
            poseStack.translate(0.5, 0.07, 0.5);
            dispatcher.render(holder.entity, 0.0, 0.0, 0.0, 0.0f, 1.0f, poseStack, buffer, packedLight);
            poseStack.popPose();
        } catch (Exception e) {
            // якщо рендер якогось моба падає, просто не малюємо його
            holder.entity = null;
        }
    }

    private Entity createDummy(CorpseBlockEntity be, Level level) {
        try {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(be.getMobType()));
            if (type == null) return null;
            Entity e = type.create(level);
            if (e == null) return null;

            if (!be.getMobTag().isEmpty()) {
                try {
                    e.load(be.getMobTag().copy());
                } catch (Exception ignored) {
                    // не вдалось застосувати дані: малюємо стандартного моба
                }
            }
            e.setCustomName(null);
            e.setCustomNameVisible(false);
            e.setInvisible(false);

            if (e instanceof LivingEntity le) {
                le.deathTime = 20;   // ванільна анімація смерті: моб лежить на боці, червоний відтінок
                le.hurtTime = 0;
                float yaw = be.getYaw();
                le.yBodyRot = yaw;
                le.yBodyRotO = yaw;
                le.yHeadRot = yaw;
                le.yHeadRotO = yaw;
                le.setXRot(0f);
                le.xRotO = 0f;
            }
            return e;
        } catch (Exception ex) {
            return null;
        }
    }
}
