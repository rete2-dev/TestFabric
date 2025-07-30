package rete2.test.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import rete2.test.init.TestModEntities;
import rete2.test.client.renders.PetEntityRender;
import rete2.test.init.TestModInventories;
import rete2.test.client.gui.PetInventoryScreen;

public class ArcaniaTestModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(TestModEntities.PET_ENTITY, PetEntityRender::new);
        HandledScreens.register(TestModInventories.PET_INVENTORY_SCREEN_HANDLER, PetInventoryScreen::new);
    }
}
