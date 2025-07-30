package rete2.test.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EntityView;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rete2.test.ArcaniaTestMod;
import rete2.test.containers.PetInventoryScreenHandler;
import rete2.test.entities.ai.FollowOwnerGoal;
import rete2.test.logic.PetInventoryStorage;
import rete2.test.logic.PetManager;
import rete2.test.network.PetInventoryPacket;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;
import java.util.List;

public class PetEntity extends TameableEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int ownerCheckTicks = 0;
    private static final int OWNER_CHECK_INTERVAL = 20;
    private String ownerName;
    private final SimpleInventory inventory = new SimpleInventory(108);
    private int interactCooldown;

    public PetEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        setInvulnerable(true);
        initializeGoals();
        if (!world.isClient && getOwnerUuid() != null && world instanceof ServerWorld serverWorld) {
            ArcaniaTestMod.LOGGER.info("Создание PetEntity с UUID: {} для игрока: {}", getUuid(), getOwnerUuid());
            loadInventorySync(serverWorld);
        }
    }

    public SimpleInventory getInventory() {
        return inventory;
    }

    private void initializeGoals() {
        this.goalSelector.add(1, new FollowOwnerGoal(this, 0.28D, 12.0F, 2.0F));
    }

    @Override
    protected EntityNavigation createNavigation(World world) {
        return new AmphibiousSwimNavigation(this, world);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (interactCooldown > 0) return ActionResult.PASS;
        if (!this.getWorld().isClient && player.getUuid().equals(getOwnerUuid())) {
            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.openHandledScreen(new PetInventoryScreenHandler.Factory(this));
                PetInventoryPacket.sendToClient(serverPlayer, this.getUuid(), this.getInventory());
                interactCooldown = 10;
                ArcaniaTestMod.LOGGER.info("Открыт инвентарь питомца {} для игрока: {}", getUuid(), player.getUuid());
                return ActionResult.SUCCESS;
            }
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (interactCooldown > 0) {
            interactCooldown--;
        }
        if (!this.getWorld().isClient && getOwner() == null && (getOwnerUuid() != null || ownerName != null)) {
            if (ownerCheckTicks++ >= OWNER_CHECK_INTERVAL) {
                ownerCheckTicks = 0;
                PlayerEntity owner = findOwner();
                if (owner != null) {
                    setOwner(owner);
                    setTamed(true);
                    initializeGoals();
                    ArcaniaTestMod.LOGGER.info("Восстановлен владелец для питомца: {} для игрока: {}", getUuid(), owner.getUuid());
                }
            }
        }
    }

    private PlayerEntity findOwner() {
        if (getWorld() instanceof ServerWorld serverWorld) {
            if (getOwnerUuid() != null) {
                PlayerEntity owner = serverWorld.getPlayerByUuid(getOwnerUuid());
                if (owner != null) {
                    return owner;
                }
            }
            if (ownerName != null) {
                List<ServerPlayerEntity> players = serverWorld.getPlayers();
                for (PlayerEntity player : players) {
                    if (player.getName().getString().equals(ownerName)) {
                        setOwnerUuid(player.getUuid());
                        return player;
                    }
                }
            }
        }
        return null;
    }

    public void loadInventorySync(ServerWorld world) {
        if (getOwnerUuid() != null) {
            ArcaniaTestMod.LOGGER.info("Синхронная загрузка инвентаря для питомца: {}, игрока: {}", getUuid(), getOwnerUuid());
            SimpleInventory loadedInventory = PetInventoryStorage.loadInventorySync(getOwnerUuid(), world);
            for (int i = 0; i < Math.min(this.inventory.size(), loadedInventory.size()); i++) {
                ItemStack stack = loadedInventory.getStack(i);
                this.inventory.setStack(i, stack);
                if (!stack.isEmpty()) {
                    ArcaniaTestMod.LOGGER.debug("Установлен предмет в слот {}: {}", i, stack);
                }
            }
            ArcaniaTestMod.LOGGER.info("Успешно загружен инвентарь для питомца: {}, игрока: {}", getUuid(), getOwnerUuid());
        } else {
            ArcaniaTestMod.LOGGER.warn("Не удалось загрузить инвентарь для питомца: {}, ownerUuid is null", getUuid());
        }
    }

    public void saveInventorySync(ServerWorld world) {
        if (getOwnerUuid() != null) {
            ArcaniaTestMod.LOGGER.info("Синхронное сохранение инвентаря для питомца: {}, игрока: {}", getUuid(), getOwnerUuid());
            PetInventoryStorage.saveInventorySync(getOwnerUuid(), this.inventory, world);
        } else {
            ArcaniaTestMod.LOGGER.warn("Не удалось сохранить инвентарь для питомца: {}, ownerUuid is null", getUuid());
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (getOwnerUuid() != null) {
            nbt.putUuid("Owner", getOwnerUuid());
        }
        nbt.putBoolean("Tamed", isTamed());
        if (getOwner() != null) {
            nbt.putString("OwnerName", getOwner().getName().getString());
        }
        if (getWorld() instanceof ServerWorld serverWorld) {
            saveInventorySync(serverWorld);
        } else {
            ArcaniaTestMod.LOGGER.warn("ServerWorld not available when writing NBT for pet: {}", getUuid());
        }
        ArcaniaTestMod.LOGGER.info("Записан NBT для питомца: {}", getUuid());
        return nbt;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.containsUuid("Owner")) {
            setOwnerUuid(nbt.getUuid("Owner"));
        }
        setTamed(nbt.getBoolean("Tamed"));
        if (nbt.contains("OwnerName")) {
            ownerName = nbt.getString("OwnerName");
        }
        setInvulnerable(true);
        initializeGoals();
        ownerCheckTicks = 0;
        if (getOwnerUuid() != null && getWorld() instanceof ServerWorld serverWorld) {
            PlayerEntity owner = serverWorld.getPlayerByUuid(getOwnerUuid());
            if (owner != null) {
                PetManager.registerPet(owner, this);
                loadInventorySync(serverWorld);
                ArcaniaTestMod.LOGGER.info("Прочитан NBT и восстановлен питомец: {} для игрока: {}", getUuid(), getOwnerUuid());
            } else {
                ArcaniaTestMod.LOGGER.info("Питомец загружен, но владелец не найден: {}", getUuid());
            }
        } else {
            ArcaniaTestMod.LOGGER.warn("Не удалось загрузить инвентарь при чтении NBT для питомца: {}, ownerUuid or ServerWorld is null", getUuid());
        }
    }

    @Override
    public void setOwner(PlayerEntity player) {
        super.setOwner(player);
        if (player != null) {
            this.ownerName = player.getName().getString();
            PetManager.registerPet(player, this);
            if (getWorld() instanceof ServerWorld serverWorld) {
                loadInventorySync(serverWorld);
            } else {
                ArcaniaTestMod.LOGGER.warn("ServerWorld not available when setting owner for pet: {}", getUuid());
            }
            ArcaniaTestMod.LOGGER.info("Установлен владелец для питомца: {} для игрока: {}", getUuid(), player.getUuid());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.getWorld().isClient && getOwner() != null && getWorld() instanceof ServerWorld serverWorld) {
            ArcaniaTestMod.LOGGER.info("Удаление питомца: {}, сохранение инвентаря для игрока: {}", getUuid(), getOwnerUuid());
            saveInventorySync(serverWorld);
            PetManager.unregisterPet((PlayerEntity) getOwner());
        }
        super.remove(reason);
        ArcaniaTestMod.LOGGER.info("Питомец удален: {} с причиной: {}", getUuid(), reason);
    }

    @Override
    public @Nullable TeleportTarget getTeleportTarget(ServerWorld destination) {
        ArcaniaTestMod.LOGGER.info("Pet {} attempted to teleport to world {}, but portal usage is disabled", getUuid(), destination.getRegistryKey().getValue());
        return null; // Запрещаем телепортацию через порталы
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, state ->
                state.isMoving() ? state.setAndContinue(DefaultAnimations.WALK) : state.setAndContinue(DefaultAnimations.IDLE)));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource damageSource) {
        return true;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public EntityView method_48926() {
        return getWorld();
    }

}