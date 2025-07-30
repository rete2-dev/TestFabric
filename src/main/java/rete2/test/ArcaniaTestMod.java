package rete2.test;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rete2.test.init.TestModEntities;
import rete2.test.init.TestModItems;
import rete2.test.init.TestModInventories;
import rete2.test.logic.PetInventoryStorage;
import rete2.test.logic.PetManager;
import rete2.test.network.PetInventoryPacket;
import software.bernie.geckolib.GeckoLib;

public class ArcaniaTestMod implements ModInitializer {

    public static final String MOD_ID = "testmod_arcania";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GeckoLib.initialize();
        PetInventoryStorage.init();
        PetManager.init();
        TestModEntities.registerModEntities();
        TestModItems.registerModItems();
        TestModInventories.registerModInventories();
        PetInventoryPacket.register();
    }
}
