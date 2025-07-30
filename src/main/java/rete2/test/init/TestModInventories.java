package rete2.test.init;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import rete2.test.ArcaniaTestMod;
import rete2.test.containers.PetInventoryScreenHandler;

public class TestModInventories {


    public static final ScreenHandlerType<PetInventoryScreenHandler> PET_INVENTORY_SCREEN_HANDLER =
            new ScreenHandlerType<>((syncId, playerInventory) -> new PetInventoryScreenHandler(syncId, playerInventory, null, null), FeatureSet.empty());

    public static void registerModInventories() {
        Registry.register(Registries.SCREEN_HANDLER, new Identifier(ArcaniaTestMod.MOD_ID, "pet_inventory"), PET_INVENTORY_SCREEN_HANDLER);
        System.out.println("Registered PET_INVENTORY_SCREEN_HANDLER");
    }
}