package rete2.test.init;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import rete2.test.ArcaniaTestMod;
import rete2.test.entities.PetEntity;

public class TestModEntities {

    public static final EntityType<PetEntity> PET_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(ArcaniaTestMod.MOD_ID, "arcania_pet_entity"),
            EntityType.Builder.create(PetEntity::new, SpawnGroup.CREATURE).setDimensions(1.75f, 1.75f).build("arcania_pet_entity")
    );

    public static void registerModEntities() {
        FabricDefaultAttributeRegistry.register(PET_ENTITY, PetEntity.createMobAttributes());
    }
}
