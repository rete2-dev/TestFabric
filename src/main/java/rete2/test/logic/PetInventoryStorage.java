package rete2.test.logic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import rete2.test.ArcaniaTestMod;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PetInventoryStorage {
    private static final Path CONFIG_PATH = Paths.get("config", ArcaniaTestMod.MOD_ID, "pet_inventories.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<UUID, SimpleInventory> inventoryCache = new HashMap<>();

    public static void init() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (!Files.exists(CONFIG_PATH)) {
                Files.createFile(CONFIG_PATH);
                Files.writeString(CONFIG_PATH, "{}");
            }
        } catch (IOException e) {
            ArcaniaTestMod.LOGGER.error("Failed to initialize pet inventory storage", e);
        }
    }

    public static CompletableFuture<SimpleInventory> loadInventoryAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (inventoryCache) {
                if (inventoryCache.containsKey(playerUuid)) {
                    ArcaniaTestMod.LOGGER.info("Loaded inventory from cache for player: {}", playerUuid);
                    return inventoryCache.get(playerUuid);
                }
                try {
                    String json = Files.readString(CONFIG_PATH);
                    JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
                    SimpleInventory inventory = new SimpleInventory(108);
                    if (jsonObject.has(playerUuid.toString())) {
                        JsonArray items = jsonObject.getAsJsonArray(playerUuid.toString());
                        for (int i = 0; i < Math.min(items.size(), 108); i++) {
                            NbtCompound nbt = JsonToNbt(items.get(i).getAsJsonObject());
                            inventory.setStack(i, ItemStack.fromNbt(nbt));
                        }
                        ArcaniaTestMod.LOGGER.info("Loaded inventory from file for player: {}, items: {}", playerUuid, items.size());
                    } else {
                        ArcaniaTestMod.LOGGER.info("No inventory found in file for player: {}", playerUuid);
                    }
                    inventoryCache.put(playerUuid, inventory);
                    return inventory;
                } catch (IOException e) {
                    ArcaniaTestMod.LOGGER.error("Failed to load inventory for player: {}", playerUuid, e);
                    return new SimpleInventory(108);
                }
            }
        });
    }

    public static void saveInventoryAsync(UUID playerUuid, SimpleInventory inventory) {
        CompletableFuture.runAsync(() -> {
            synchronized (inventoryCache) {
                saveInventoryInternal(playerUuid, inventory);
            }
        });
    }

    public static void saveInventorySync(UUID playerUuid, SimpleInventory inventory) {
        synchronized (inventoryCache) {
            saveInventoryInternal(playerUuid, inventory);
        }
    }

    private static void saveInventoryInternal(UUID playerUuid, SimpleInventory inventory) {
        inventoryCache.put(playerUuid, inventory);
        try {
            String json = Files.readString(CONFIG_PATH);
            JsonObject jsonObject = GSON.fromJson(json, JsonObject.class);
            if (jsonObject == null) {
                jsonObject = new JsonObject();
            }
            JsonArray items = new JsonArray();
            int itemCount = 0;
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                if (!stack.isEmpty()) {
                    JsonObject itemJson = NbtToJson(stack.writeNbt(new NbtCompound()));
                    items.add(itemJson);
                    itemCount++;
                }
            }
            jsonObject.add(playerUuid.toString(), items);
            Files.writeString(CONFIG_PATH, GSON.toJson(jsonObject));
            ArcaniaTestMod.LOGGER.info("Saved inventory for player: {}, items: {}", playerUuid, itemCount);
        } catch (IOException e) {
            ArcaniaTestMod.LOGGER.error("Failed to save inventory for player: {}", playerUuid, e);
        }
    }

    private static JsonObject NbtToJson(NbtCompound nbt) {
        String nbtString = nbt.toString();
        return GSON.fromJson(nbtString, JsonObject.class);
    }

    private static NbtCompound JsonToNbt(JsonObject json) {
        try {
            String jsonString = GSON.toJson(json);
            return StringNbtReader.parse(jsonString);
        } catch (Exception e) {
            ArcaniaTestMod.LOGGER.error("Failed to convert JSON to NBT: {}", json, e);
            return new NbtCompound();
        }
    }
}