package rete2.test.containers;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import rete2.test.ArcaniaTestMod;
import rete2.test.entities.PetEntity;
import rete2.test.init.TestModInventories;
import rete2.test.logic.PetInventoryStorage;
import rete2.test.network.PetInventoryPacket;

import java.util.Map;
import java.util.UUID;

public class PetInventoryScreenHandler extends ScreenHandler {
    private final PetEntity pet;
    private final Inventory petInventory;
    private final UUID petUuid;

    public PetInventoryScreenHandler(int syncId, PlayerInventory playerInventory, PetEntity pet, UUID petUuid) {
        super(TestModInventories.PET_INVENTORY_SCREEN_HANDLER, syncId);
        this.pet = pet;
        this.petUuid = petUuid;
        this.petInventory = pet != null ? pet.getInventory() : new SimpleInventory(108);

        if (pet != null) {
            checkAccess(playerInventory.player, pet);
        }

        int xSize = 238;
        int ySize = 256;

        int rowLength = 12;
        for (int chestRow = 0; chestRow < 108 / rowLength; chestRow++) {
            for (int chestCol = 0; chestCol < rowLength; chestCol++) {
                int slotIndex = chestCol + chestRow * rowLength;
                addSlot(new Slot(petInventory, slotIndex, 12 + chestCol * 18, 8 + chestRow * 18) {
                    @Override
                    public void setStack(ItemStack stack) {
                        ItemStack oldStack = this.getStack();
                        super.setStack(stack);
                        if (!playerInventory.player.getWorld().isClient && (!ItemStack.areEqual(oldStack, stack))) {
                            PetInventoryPacket.sendSlotUpdateToClient((ServerPlayerEntity) playerInventory.player, petUuid, slotIndex, stack);
                            if (playerInventory.player.getWorld() instanceof ServerWorld serverWorld) {
                                PetInventoryStorage.saveInventoryAsync(playerInventory.player.getUuid(), (SimpleInventory) petInventory, serverWorld);
                            }
                        }
                    }
                });
            }
        }

        int leftCol = (xSize - 162) / 2 + 1;

        for (int playerInvRow = 0; playerInvRow < 3; playerInvRow++) {
            for (int playerInvCol = 0; playerInvCol < 9; playerInvCol++) {
                addSlot(new Slot(playerInventory, playerInvCol + playerInvRow * 9 + 9, leftCol + playerInvCol * 18, ySize - (4 - playerInvRow) * 18 - 10));
            }
        }

        for (int hotbarSlot = 0; hotbarSlot < 9; hotbarSlot++) {
            addSlot(new Slot(playerInventory, hotbarSlot, leftCol + hotbarSlot * 18, ySize - 24));
        }
        ArcaniaTestMod.LOGGER.info("PetInventoryScreenHandler created for pet: {}, UUID: {}", pet != null ? pet.getUuidAsString() : "null", petUuid);
    }

    public void updatePetInventory(UUID petUuid, Map<Integer, ItemStack> slotUpdates) {
        if (this.petUuid != null && this.petUuid.equals(petUuid)) {
            for (Map.Entry<Integer, ItemStack> entry : slotUpdates.entrySet()) {
                int slot = entry.getKey();
                if (slot >= 0 && slot < petInventory.size()) {
                    petInventory.setStack(slot, entry.getValue());
                    this.slots.get(slot).setStack(entry.getValue());
                }
            }
            ArcaniaTestMod.LOGGER.info("Updated PetInventoryScreenHandler inventory for UUID: {}, slots: {}", petUuid, slotUpdates.keySet());
        } else {
            ArcaniaTestMod.LOGGER.warn("Skipped update: UUID mismatch. Expected {}, got {}", petUuid, this.petUuid);
        }
    }

    private void checkAccess(PlayerEntity player, PetEntity pet) {
        UUID ownerUuid = pet.getOwnerUuid();
        if (ownerUuid == null || !player.getUuid().equals(ownerUuid)) {
            throw new IllegalStateException("Only the owner can access the pet's inventory");
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return pet == null || (pet.isAlive() && player.getUuid().equals(pet.getOwnerUuid()));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            stack = originalStack.copy();
            if (slotIndex < petInventory.size()) {
                if (!this.insertItem(originalStack, petInventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.insertItem(originalStack, 0, petInventory.size(), false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
            if (!player.getWorld().isClient && player.getWorld() instanceof ServerWorld serverWorld) {
                PetInventoryPacket.sendSlotUpdateToClient((ServerPlayerEntity) player, petUuid, slotIndex, originalStack);
                PetInventoryStorage.saveInventoryAsync(player.getUuid(), (SimpleInventory) petInventory, serverWorld);
            }
        }
        return stack;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (!player.getWorld().isClient && pet != null && player.getWorld() instanceof ServerWorld serverWorld) {
            PetInventoryStorage.saveInventoryAsync(player.getUuid(), (SimpleInventory) petInventory, serverWorld);
        }
    }

    public static class Factory implements NamedScreenHandlerFactory {
        private final PetEntity pet;

        public Factory(PetEntity pet) {
            this.pet = pet;
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new PetInventoryScreenHandler(syncId, playerInventory, pet, pet != null ? pet.getUuid() : new UUID(0, 0));
        }

        @Override
        public Text getDisplayName() {
            return Text.translatable("arcania_test.pet_inventory");
        }
    }
}