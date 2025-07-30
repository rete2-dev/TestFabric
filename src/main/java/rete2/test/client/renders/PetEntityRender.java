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
}
