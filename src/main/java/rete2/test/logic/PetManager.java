package rete2.test.logic;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import rete2.test.ArcaniaTestMod;
import rete2.test.entities.PetEntity;
import rete2.test.init.TestModEntities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetManager {
    private static final Map<UUID, UUID> PLAYER_PET_MAP = new HashMap<>();

    public static void registerPet(PlayerEntity owner, PetEntity pet) {
        if (owner != null && pet != null) {
            PLAYER_PET_MAP.put(owner.getUuid(), pet.getUuid());
            System.out.println("Registered pet UUID " + pet.getUuid() + " for player " + owner.getName().getString() + " with UUID " + owner.getUuid());
        }
    }

    public static void unregisterPet(PlayerEntity owner) {
        if (owner != null) {
            PLAYER_PET_MAP.remove(owner.getUuid());
            System.out.println("Unregistered pet for player " + owner.getName().getString() + " with UUID " + owner.getUuid());
        }
    }

    public static PetEntity getPet(PlayerEntity owner, ServerWorld world) {
        if (owner == null || world == null) return null;
        UUID petUuid = PLAYER_PET_MAP.get(owner.getUuid());
        if (petUuid != null) {
            Entity entity = world.getEntity(petUuid);
            if (entity instanceof PetEntity pet && pet.isAlive()) {
                return pet;
            }
        }
        return null;
    }

    // Поиск питомца по всем мирам сервера
    private static PetEntity findPetByOwner(MinecraftServer server, UUID ownerUuid) {
        if (server == null || ownerUuid == null) return null;
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.getEntitiesByType(TestModEntities.PET_ENTITY, pet -> pet.getOwnerUuid() != null && pet.getOwnerUuid().equals(ownerUuid))) {
                if (entity instanceof PetEntity pet && pet.isAlive()) {
                    return pet;
                }
            }
        }
        return null;
    }

    public static void init() {
        // Триггер: Событие смены измерения игрока
        // Срабатывает, когда игрок переходит в новое измерение (например, Оверворлд → Незер)
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            PetEntity pet = getPet(player, origin);
            if (pet != null && pet.isAlive()) {
                teleportPetToOwner(pet, player, destination);
                System.out.println("PetEntity teleported to owner after dimension change to " + destination.getRegistryKey().getValue());
            }
        });

        // Триггер: Событие возрождения игрока после смерти
        // Срабатывает после смерти игрока, когда он возрождается в точке спавна
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PetEntity pet = getPet(newPlayer, (ServerWorld) oldPlayer.getWorld());
            if (pet != null && pet.isAlive()) {
                teleportPetToOwner(pet, newPlayer, (ServerWorld) newPlayer.getWorld());
                System.out.println("PetEntity teleported to owner after respawn at " + newPlayer.getPos());
            }
        });

        // Триггер: Событие входа игрока в игру
        // Срабатывает, когда игрок подключается к серверу
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            PetEntity pet = findPetByOwner(server, player.getUuid());
            if (pet != null && pet.isAlive()) {
                registerPet(player, pet);
                System.out.println("Re-registered pet UUID " + pet.getUuid() + " for player " + player.getName().getString() + " on login");
            }
        });

        // Триггер: Событие выхода игрока из игры
        // Срабатывает, когда игрок отключается от сервера
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            PetEntity pet = PetManager.getPet(player, player.getServerWorld());
            if (pet != null) {
                PetInventoryStorage.saveInventorySync(player.getUuid(), pet.getInventory());
                PetManager.unregisterPet(player);
                ArcaniaTestMod.LOGGER.info("Saved pet inventory on player disconnect: {}", player.getUuid());
            }
        });

        // Обработка перехода в другое измерение
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof PetEntity pet && !world.isClient) {
                PlayerEntity owner = (PlayerEntity) pet.getOwner();
                if (owner != null) {
                    PetInventoryStorage.saveInventorySync(owner.getUuid(), pet.getInventory());
                    PetManager.unregisterPet(owner);
                    ArcaniaTestMod.LOGGER.info("Saved pet inventory on dimension change or unload: {}", owner.getUuid());
                }
            }
        });

    }

    private static void teleportPetToOwner(PetEntity pet, PlayerEntity owner, ServerWorld targetWorld) {
        if (pet.getWorld() != targetWorld) {
            // Сохраняем данные перед удалением
            NbtCompound nbt = new NbtCompound();
            pet.writeNbt(nbt);
            pet.remove(Entity.RemovalReason.CHANGED_DIMENSION);

            // Создаём новую сущность в целевом мире
            PetEntity newPet = new PetEntity((EntityType<? extends TameableEntity>) pet.getType(), targetWorld);
            newPet.readNbt(nbt);
            targetWorld.spawnEntity(newPet);
            pet.copyFrom(newPet); // Обновляем текущую сущность
        }

        // Телепортация к владельцу
        Vec3d ownerPos = owner.getPos();
        for (int i = 0; i < 10; ++i) {
            double x = ownerPos.x + (pet.getRandom().nextDouble() - 0.5D) * 4.0D;
            double y = ownerPos.y + (pet.getRandom().nextDouble() - 0.5D) * 2.0D;
            double z = ownerPos.z + (pet.getRandom().nextDouble() - 0.5D) * 4.0D;
            if (pet.teleport(x, y, z, false)) {
                pet.getNavigation().stop();
                return;
            }
        }
    }
}
