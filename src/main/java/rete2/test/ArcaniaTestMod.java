package rete2.test;

import net.fabricmc.api.ModInitializer;
import rete2.test.init.TestModEntities;
import rete2.test.init.TestModItems;
import rete2.test.init.TestModInventories;
import rete2.test.logic.PetManager;
import rete2.test.network.PetInventoryPacket;
import software.bernie.geckolib.GeckoLib;

public class ArcaniaTestMod implements ModInitializer {

    public static final String MOD_ID = "testmod_arcania";

    @Override
    public void onInitialize() {
        GeckoLib.initialize();
        PetManager.init();
        TestModEntities.registerModEntities();
        TestModItems.registerModItems();
        TestModInventories.registerModInventories();
        PetInventoryPacket.register();
    }
}
