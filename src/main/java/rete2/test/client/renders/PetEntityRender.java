package rete2.test.client.renders;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import rete2.test.client.models.PetEntityModel;
import rete2.test.entities.PetEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class PetEntityRender extends GeoEntityRenderer<PetEntity> {
    public PetEntityRender(EntityRendererFactory.Context renderManager) {
        super(renderManager, new PetEntityModel());
    }

    @Override
    protected void applyRotations(PetEntity entity, MatrixStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.applyRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);

        // Получаем ближайшего игрока на клиенте
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && entity.getOwnerUuid() != null && entity.getOwnerUuid().equals(player.getUuid())) {
            double distanceToPlayer = entity.squaredDistanceTo(player);
            if (distanceToPlayer <= 64.0D) { // 8 блоков (8^2 = 64)
                // Получаем кость головы из модели (предполагается, что кость называется "head")
                GeoBone headBone = this.getGeoModel().getBone("h_head").orElse(null);
                if (headBone != null) {
                    // Вычисляем углы поворота к игроку
                    double deltaX = player.getX() - entity.getX();
                    double deltaY = (player.getEyeY() - player.getHeight() * 0.5D) - (entity.getY() + entity.getHeight() * 0.5D);
                    double deltaZ = player.getZ() - entity.getZ();

                    // Угол поворота по горизонтали (yaw)
                    float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
                    // Угол поворота по вертикали (pitch)
                    float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)));

                    // Ограничиваем углы поворота для естественного вида
                    float maxYaw = 30.0F;
                    float maxPitch = 30.0F;
                    yaw = MathHelper.clamp(yaw - entity.getBodyYaw(), -maxYaw, maxYaw);
                    pitch = MathHelper.clamp(pitch, -maxPitch, maxPitch);

                    // Применяем поворот к кости головы
                    headBone.setRotY((float) Math.toRadians(-yaw));
                    headBone.setRotX((float) Math.toRadians(pitch));

                }
            }
        }
    }

}
