package rete2.test.init;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroupEntries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import rete2.test.ArcaniaTestMod;
import rete2.test.items.PetWand;

public class TestModItems {

    public static final ItemGroup CREATIVE_GROUP = Registry.register(Registries.ITEM_GROUP,
            new Identifier(ArcaniaTestMod.MOD_ID, "arcania"),
            FabricItemGroup.builder().displayName(Text.translatable("itemgroup.testmod_arcania"))
                    .icon(() -> new ItemStack(TestModItems.PET_WAND)).entries((displayContext, entries) -> {
                        entries.add(TestModItems.PET_WAND);
                    }).build());

    public static final PetWand PET_WAND = (PetWand) registerItem(new PetWand(new FabricItemSettings()));

    public static void addItemsToIngredientItemCreativeGroup(FabricItemGroupEntries entries){
        entries.add(PET_WAND);
    }

    private static Item registerItem(Item item){
        return Registry.register(Registries.ITEM, new Identifier(ArcaniaTestMod.MOD_ID, "arcania_pet_wand"), item);
    }

    public static void registerModItems(){
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(TestModItems::addItemsToIngredientItemCreativeGroup);
    }

}
