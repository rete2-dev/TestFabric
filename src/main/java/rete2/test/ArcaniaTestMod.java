package rete2.test;

import net.fabricmc.api.ModInitializer;
import rete2.test.init.TestModEntities;
import rete2.test.init.TestModItems;
import software.bernie.geckolib.GeckoLib;

public class ArcaniaTestMod implements ModInitializer {

    public static final String MOD_ID = "testmod_arcania";

    @Override
    public void onInitialize() {
        GeckoLib.initialize();
        TestModEntities.registerModEntities();
        TestModItems.registerModItems();
    }
}
