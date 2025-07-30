package rete2.test.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import rete2.test.ArcaniaTestMod;
import rete2.test.containers.PetInventoryScreenHandler;

import java.util.Objects;
import java.util.UUID;

public class PetInventoryPacket {
    public static final Identifier PET_INVENTORY_PACKET_ID = new Identifier(ArcaniaTestMod.MOD_ID, "pet_inventory_packet");

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PET_INVENTORY_PACKET_ID, (client, handler, buf, responseSender) -> {
            UUID petUuid = buf.readUuid();
            NbtList inventoryList = Objects.requireNonNull(buf.readNbt()).getList("Inventory", 10);
            SimpleInventory inventory = new SimpleInventory(108);
            for (int i = 0; i < inventoryList.size(); i++) {
                NbtCompound itemNbt = inventoryList.getCompound(i);
                int slot = itemNbt.getByte("Slot") & 255;
                if (slot < inventory.size()) {
                    inventory.setStack(slot, ItemStack.fromNbt(itemNbt));
                }
            }
            client.execute(() -> {
                if (client.player != null && client.player.currentScreenHandler instanceof PetInventoryScreenHandler screenHandler) {
                    screenHandler.updatePetInventory(petUuid, inventory);
                }
                System.out.println("Received PetInventoryPacket for UUID: " + petUuid + ", inventory size: " + inventory.size());
            });
        });
    }

    public static void sendToClient(ServerPlayerEntity player, UUID petUuid, SimpleInventory inventory) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(petUuid);
        NbtCompound nbt = new NbtCompound();
        NbtList inventoryList = new NbtList();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound itemNbt = new NbtCompound();
                itemNbt.putByte("Slot", (byte) i);
                stack.writeNbt(itemNbt);
                inventoryList.add(itemNbt);
            }
        }
        nbt.put("Inventory", inventoryList);
        buf.writeNbt(nbt);
        ServerPlayNetworking.send(player, PET_INVENTORY_PACKET_ID, buf);
        System.out.println("Sent PetInventoryPacket for UUID: " + petUuid);
    }
}