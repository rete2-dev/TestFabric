package rete2.test;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import rete2.test.init.TestModEntities;
import rete2.test.client.renders.PetEntityRender;

public class ArcaniaTestModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(TestModEntities.PET_ENTITY, PetEntityRender::new);
    }
}
