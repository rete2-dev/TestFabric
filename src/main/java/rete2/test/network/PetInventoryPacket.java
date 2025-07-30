package rete2.test.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import rete2.test.ArcaniaTestMod;
import rete2.test.containers.PetInventoryScreenHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PetInventoryPacket {
    public static final Identifier PET_INVENTORY_PACKET_ID = new Identifier(ArcaniaTestMod.MOD_ID, "pet_inventory");

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PET_INVENTORY_PACKET_ID, (client, handler, buf, responseSender) -> {
            UUID petUuid = buf.readUuid();
            boolean fullSync = buf.readBoolean();
            Map<Integer, ItemStack> slotUpdates = new HashMap<>();

            if (fullSync) {
                for (int i = 0; i < 108; i++) {
                    ItemStack stack = buf.readItemStack();
                    slotUpdates.put(i, stack);
                }
            } else {
                int updateCount = buf.readVarInt();
                for (int i = 0; i < updateCount; i++) {
                    int slot = buf.readVarInt();
                    ItemStack stack = buf.readItemStack();
                    slotUpdates.put(slot, stack);
                }
            }

            client.execute(() -> {
                assert client.player != null;
                ScreenHandler screenHandler = client.player.currentScreenHandler;
                if (screenHandler instanceof PetInventoryScreenHandler petScreenHandler) {
                    petScreenHandler.updatePetInventory(petUuid, slotUpdates);
                    ArcaniaTestMod.LOGGER.info("Client received inventory update for pet: {}, fullSync: {}", petUuid, fullSync);
                } else {
                    ArcaniaTestMod.LOGGER.warn("Client received inventory packet but no PetInventoryScreenHandler open for pet: {}", petUuid);
                }
            });
        });
    }

    public static void sendToClient(ServerPlayerEntity player, UUID petUuid, SimpleInventory inventory) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(petUuid);
        buf.writeBoolean(true); // Full sync
        for (int i = 0; i < inventory.size(); i++) {
            buf.writeItemStack(inventory.getStack(i));
        }
        ServerPlayNetworking.send(player, PET_INVENTORY_PACKET_ID, buf);
        ArcaniaTestMod.LOGGER.info("Sent full inventory sync to player: {} for pet: {}", player.getName().getString(), petUuid);
    }

    public static void sendSlotUpdateToClient(ServerPlayerEntity player, UUID petUuid, int slot, ItemStack stack) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(petUuid);
        buf.writeBoolean(false); // Partial sync
        buf.writeVarInt(1); // Number of updates
        buf.writeVarInt(slot);
        buf.writeItemStack(stack);
        ServerPlayNetworking.send(player, PET_INVENTORY_PACKET_ID, buf);
        ArcaniaTestMod.LOGGER.info("Sent slot update to player: {} for pet: {}, slot: {}", player.getName().getString(), petUuid, slot);
    }
}