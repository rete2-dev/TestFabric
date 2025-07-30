package rete2.test.logic;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rete2.test.ArcaniaTestMod;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PetInventoryStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArcaniaTestMod.MOD_ID + "_storage");
    private static final String STORAGE_DIR = "arcania_pet_inventories";

    public static void init() {
        LOGGER.info("Инициализация PetInventoryStorage");
    }

    public static CompletableFuture<SimpleInventory> loadInventoryAsync(UUID playerUuid, ServerWorld world) {
        LOGGER.info("Попытка асинхронной загрузки инвентаря для игрока: {}", playerUuid);
        return CompletableFuture.supplyAsync(() -> loadInventorySync(playerUuid, world));
    }

    public static SimpleInventory loadInventorySync(UUID playerUuid, ServerWorld world) {
        try {
            File file = getInventoryFile(playerUuid, world);
            LOGGER.info("Проверка файла инвентаря: {}", file.getAbsolutePath());
            if (!file.exists()) {
                LOGGER.info("Файл инвентаря не найден для игрока: {}", playerUuid);
                return new SimpleInventory(108);
            }

            NbtCompound nbt = NbtIo.readCompressed(file);
            SimpleInventory inventory = new SimpleInventory(108);
            NbtList items = nbt.getList("Items", 10); // 10 - тип NbtCompound
            LOGGER.info("Загружено {} элементов из файла для игрока: {}", items.size(), playerUuid);
            for (int i = 0; i < items.size() && i < inventory.size(); i++) {
                NbtCompound itemNbt = items.getCompound(i);
                int slot = itemNbt.getInt("Slot");
                if (slot >= 0 && slot < inventory.size()) {
                    ItemStack stack = ItemStack.fromNbt(itemNbt);
                    inventory.setStack(slot, stack);
                    LOGGER.debug("Загружен предмет в слот {}: {}", slot, stack);
                }
            }
            LOGGER.info("Успешно загружен инвентарь для игрока: {}", playerUuid);
            return inventory;
        } catch (Exception e) {
            LOGGER.error("Не удалось загрузить инвентарь для игрока: {}", playerUuid, e);
            return new SimpleInventory(108);
        }
    }

    public static void saveInventoryAsync(UUID playerUuid, SimpleInventory inventory, ServerWorld world) {
        LOGGER.info("Запуск асинхронного сохранения инвентаря для игрока: {}", playerUuid);
        CompletableFuture.runAsync(() -> saveInventorySync(playerUuid, inventory, world));
    }

    public static void saveInventorySync(UUID playerUuid, SimpleInventory inventory, ServerWorld world) {
        try {
            File file = getInventoryFile(playerUuid, world);
            LOGGER.info("Сохранение инвентаря в файл: {}", file.getAbsolutePath());
            file.getParentFile().mkdirs();

            NbtCompound nbt = new NbtCompound();
            NbtList items = new NbtList();
            int itemCount = 0;
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    NbtCompound itemNbt = new NbtCompound();
                    itemNbt.putInt("Slot", i);
                    stack.writeNbt(itemNbt);
                    items.add(itemNbt);
                    itemCount++;
                    LOGGER.debug("Сохранён предмет в слот {}: {}", i, stack);
                }
            }
            nbt.put("Items", items);
            LOGGER.info("Подготовлено {} элементов для сохранения для игрока: {}", itemCount, playerUuid);

            NbtIo.writeCompressed(nbt, file);
            LOGGER.info("Успешно сохранен инвентарь для игрока: {}", playerUuid);
        } catch (Exception e) {
            LOGGER.error("Не удалось сохранить инвентарь для игрока: {}", playerUuid, e);
        }
    }

    private static File getInventoryFile(UUID playerUuid, ServerWorld world) {
        if (world == null) {
            LOGGER.error("ServerWorld is null для игрока: {}", playerUuid);
            throw new IllegalStateException("Не удалось получить доступ к миру для хранения инвентаря");
        }
        File storageDir = new File(world.getServer().getSavePath(WorldSavePath.ROOT).toFile(), STORAGE_DIR);
        File file = new File(storageDir, playerUuid.toString() + ".nbt");
        LOGGER.info("Получен путь к файлу инвентаря: {}", file.getAbsolutePath());
        return file;
    }
}