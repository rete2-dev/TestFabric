package rete2.test.client.models;

import net.minecraft.util.Identifier;
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
        if (head != null) {
            head.setRotX(entity.getPitch() * 0.017453292F);
            head.setRotY(-entity.getHeadYaw() * 0.017453292F);
        }
    }

}
