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
import rete2.test.network.PetInventoryPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetManager {
    private static final Map<UUID, UUID> PLAYER_PET_MAP = new HashMap<>();

    public static void registerPet(PlayerEntity owner, PetEntity pet) {
        if (owner != null && pet != null) {
            PLAYER_PET_MAP.put(owner.getUuid(), pet.getUuid());
            ArcaniaTestMod.LOGGER.info("Registered pet UUID {} for player {} with UUID {}", pet.getUuid(), owner.getName().getString(), owner.getUuid());
        } else {
            ArcaniaTestMod.LOGGER.warn("Failed to register pet: owner or pet is null");
        }
    }

    public static void unregisterPet(PlayerEntity owner) {
        if (owner != null) {
            UUID petUuid = PLAYER_PET_MAP.remove(owner.getUuid());
            ArcaniaTestMod.LOGGER.info("Unregistered pet UUID {} for player {} with UUID {}", petUuid, owner.getName().getString(), owner.getUuid());
        } else {
            ArcaniaTestMod.LOGGER.warn("Failed to unregister pet: owner is null");
        }
    }

    public static PetEntity getPet(PlayerEntity owner, ServerWorld world) {
        if (owner == null || world == null) {
            ArcaniaTestMod.LOGGER.warn("Cannot get pet: owner or world is null");
            return null;
        }
        UUID petUuid = PLAYER_PET_MAP.get(owner.getUuid());
        if (petUuid != null) {
            Entity entity = world.getEntity(petUuid);
            if (entity instanceof PetEntity pet && pet.isAlive()) {
                ArcaniaTestMod.LOGGER.info("Found pet UUID {} for player {}", petUuid, owner.getUuid());
                return pet;
            }
        }
        ArcaniaTestMod.LOGGER.info("No pet found for player {} in world {}", owner.getUuid(), world.getRegistryKey().getValue());
        return null;
    }

    private static PetEntity findPetByOwner(MinecraftServer server, UUID ownerUuid) {
        if (server == null || ownerUuid == null) {
            ArcaniaTestMod.LOGGER.warn("Cannot find pet: server or ownerUuid is null");
            return null;
        }
        for (ServerWorld world : server.getWorlds()) {
            for (Entity entity : world.getEntitiesByType(TestModEntities.PET_ENTITY, pet -> pet.getOwnerUuid() != null && pet.getOwnerUuid().equals(ownerUuid))) {
                if (entity instanceof PetEntity pet && pet.isAlive()) {
                    ArcaniaTestMod.LOGGER.info("Found pet UUID {} for player {} in world {}", pet.getUuid(), ownerUuid, world.getRegistryKey().getValue());
                    return pet;
                }
            }
        }
        ArcaniaTestMod.LOGGER.info("No pet found for player {} across all worlds", ownerUuid);
        return null;
    }

    public static void init() {
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            ArcaniaTestMod.LOGGER.info("Player {} changed world from {} to {}", player.getUuid(), origin.getRegistryKey().getValue(), destination.getRegistryKey().getValue());
            PetEntity pet = getPet(player, origin);
            if (pet != null && pet.isAlive()) {
                ArcaniaTestMod.LOGGER.info("Teleporting pet {} for player {} to world {}", pet.getUuid(), player.getUuid(), destination.getRegistryKey().getValue());
                pet.saveInventorySync(origin); // Сохраняем перед телепортацией
                teleportPetToOwner(pet, player, destination);
                PetInventoryPacket.sendToClient(player, pet.getUuid(), pet.getInventory());
                ArcaniaTestMod.LOGGER.info("Sent inventory sync for pet {} to player {}", pet.getUuid(), player.getUuid());

            } else {
                ArcaniaTestMod.LOGGER.warn("No pet found for player {} in origin world {}", player.getUuid(), origin.getRegistryKey().getValue());
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            ArcaniaTestMod.LOGGER.info("Player {} respawned in world {}", newPlayer.getUuid(), newPlayer.getWorld().getRegistryKey().getValue());
            PetEntity pet = getPet(newPlayer, (ServerWorld) oldPlayer.getWorld());
            if (pet != null && pet.isAlive()) {
                ArcaniaTestMod.LOGGER.info("Teleporting pet {} for player {} after respawn", pet.getUuid(), newPlayer.getUuid());
                teleportPetToOwner(pet, newPlayer, (ServerWorld) newPlayer.getWorld());
                PetInventoryPacket.sendToClient(newPlayer, pet.getUuid(), pet.getInventory());
                ArcaniaTestMod.LOGGER.info("Sent inventory sync for pet {} to player {}", pet.getUuid(), newPlayer.getUuid());
            } else {
                ArcaniaTestMod.LOGGER.warn("No pet found for player {} after respawn", newPlayer.getUuid());
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ArcaniaTestMod.LOGGER.info("Player {} joined server", player.getUuid());
            PetEntity pet = findPetByOwner(server, player.getUuid());
            if (pet != null && pet.isAlive()) {
                registerPet(player, pet);
                pet.loadInventorySync(player.getServerWorld());
                PetInventoryPacket.sendToClient(player, pet.getUuid(), pet.getInventory());
                ArcaniaTestMod.LOGGER.info("Re-registered and loaded inventory for pet UUID {} for player {}", pet.getUuid(), player.getUuid());
            } else {
                ArcaniaTestMod.LOGGER.info("No pet found for player {} on login", player.getUuid());
            }
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ArcaniaTestMod.LOGGER.info("Player {} disconnected", player.getUuid());
            PetEntity pet = PetManager.getPet(player, player.getServerWorld());
            if (pet != null) {
                pet.saveInventorySync(player.getServerWorld());
                PetManager.unregisterPet(player);
                ArcaniaTestMod.LOGGER.info("Saved pet inventory and unregistered pet for player: {}", player.getUuid());
            } else {
                ArcaniaTestMod.LOGGER.warn("No pet found for player {} on disconnect", player.getUuid());
            }
        });

        ServerEntityEvents.ENTITY_UNLOAD.register((entity, world) -> {
            if (entity instanceof PetEntity pet && !world.isClient) {
                PlayerEntity owner = (PlayerEntity) pet.getOwner();
                if (owner != null) {
                    ArcaniaTestMod.LOGGER.info("Unloading pet {} for player {}, saving inventory", pet.getUuid(), owner.getUuid());
                    pet.saveInventorySync(world);
                    PetManager.unregisterPet(owner);
                } else {
                    ArcaniaTestMod.LOGGER.warn("No owner found for pet {} during unload", pet.getUuid());
                }
            }
        });
    }

    private static void teleportPetToOwner(PetEntity pet, PlayerEntity owner, ServerWorld targetWorld) {
        ArcaniaTestMod.LOGGER.info("Teleporting pet {} to player {} in world {}", pet.getUuid(), owner.getUuid(), targetWorld.getRegistryKey().getValue());
        if (pet.getWorld() != targetWorld) {
            NbtCompound nbt = new NbtCompound();
            pet.writeNbt(nbt);
            pet.saveInventorySync(pet.getWorld() instanceof ServerWorld ? (ServerWorld) pet.getWorld() : targetWorld);
            pet.remove(Entity.RemovalReason.CHANGED_DIMENSION);

            PetEntity newPet = new PetEntity((EntityType<? extends TameableEntity>) pet.getType(), targetWorld);
            newPet.readNbt(nbt);
            targetWorld.spawnEntity(newPet);
            newPet.loadInventorySync(targetWorld);
            pet.copyFrom(newPet);
            ArcaniaTestMod.LOGGER.info("Created new pet instance {} in world {}", newPet.getUuid(), targetWorld.getRegistryKey().getValue());
        }

        Vec3d ownerPos = owner.getPos();
        for (int i = 0; i < 10; ++i) {
            double x = ownerPos.x + (pet.getRandom().nextDouble() - 0.5D) * 4.0D;
            double y = ownerPos.y + (pet.getRandom().nextDouble() - 0.5D) * 2.0D;
            double z = ownerPos.z + (pet.getRandom().nextDouble() - 0.5D) * 4.0D;
            if (pet.teleport(x, y, z, false)) {
                pet.getNavigation().stop();
                ArcaniaTestMod.LOGGER.info("Pet {} teleported to {}", pet.getUuid(), new Vec3d(x, y, z));
                return;
            }
        }
        ArcaniaTestMod.LOGGER.warn("Failed to teleport pet {} to owner {}", pet.getUuid(), owner.getUuid());
    }
}