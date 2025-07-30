package rete2.test.client.models;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import rete2.test.ArcaniaTestMod;
import rete2.test.entities.PetEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class PetEntityModel extends DefaultedEntityGeoModel<PetEntity> {
    public PetEntityModel() {
        super(new Identifier(ArcaniaTestMod.MOD_ID, "arcania_pet_entity"), true);
    }

    @Override
    public void setCustomAnimations(PetEntity entity, long instanceId, AnimationState<PetEntity> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);
        CoreGeoBone head = getAnimationProcessor().getBone("h_head");
        if (head != null && entity.getOwner() != null) {
            PlayerEntity owner = (PlayerEntity) entity.getOwner();
            if (entity.squaredDistanceTo(owner) <= 64.0D) {
                double deltaX = owner.getX() - entity.getX();
                double deltaY = (owner.getEyeY() - owner.getHeight() * 0.5D) - (entity.getY() + entity.getHeight() * 0.5D);
                double deltaZ = owner.getZ() - entity.getZ();

                float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
                float pitch = (float) -Math.toDegrees(Math.atan2(deltaY, Math.sqrt(deltaX * deltaX + deltaZ * deltaZ)));

                float maxYaw = 30.0F;
                float maxPitch = 30.0F;
                yaw = MathHelper.clamp(yaw - entity.getBodyYaw(), -maxYaw, maxYaw);
                pitch = MathHelper.clamp(pitch, -maxPitch, maxPitch);

                head.setRotY((float) Math.toRadians(-yaw));
                head.setRotX((float) Math.toRadians(pitch));
            } else {
                head.setRotX(0.0F);
                head.setRotY(0.0F);
            }
        }
    }

}
