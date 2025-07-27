package rete2.test.client.renders;

import net.minecraft.util.Identifier;
import rete2.test.ArcaniaTestMod;
import rete2.test.items.PetWand;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class PetWandRenderer extends GeoItemRenderer<PetWand> {
    public PetWandRenderer() {
        super(new DefaultedItemGeoModel<>(new Identifier(ArcaniaTestMod.MOD_ID, "arcania_pet_wand")));
    }
}
