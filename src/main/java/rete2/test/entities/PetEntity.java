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
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import rete2.test.ArcaniaTestMod;
import rete2.test.containers.PetInventoryScreenHandler;
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
        if (!world.isClient && getOwnerUuid() != null) {
            loadInventoryAsync();
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
                    ArcaniaTestMod.LOGGER.info("Restored owner for pet: {} to player: {}", getUuid(), owner.getUuid());
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

    private void loadInventoryAsync() {
        if (getOwnerUuid() != null) {
            PetInventoryStorage.loadInventoryAsync(getOwnerUuid()).thenAccept(inventory -> {
                if (!this.isRemoved()) {
                    for (int i = 0; i < Math.min(this.inventory.size(), inventory.size()); i++) {
                        this.inventory.setStack(i, inventory.getStack(i));
                    }
                    ArcaniaTestMod.LOGGER.info("Loaded inventory for pet of player: {}", getOwnerUuid());
                } else {
                    ArcaniaTestMod.LOGGER.warn("Pet removed before inventory could be loaded for player: {}", getOwnerUuid());
                }
            }).exceptionally(throwable -> {
                ArcaniaTestMod.LOGGER.error("Failed to load inventory for pet of player: {}", getOwnerUuid(), throwable);
                return null;
            });
        }
    }

    private void saveInventorySync() {
        if (getOwnerUuid() != null) {
            PetInventoryStorage.saveInventorySync(getOwnerUuid(), this.inventory);
            ArcaniaTestMod.LOGGER.info("Synchronously saved inventory for pet of player: {}", getOwnerUuid());
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
        saveInventorySync();
        ArcaniaTestMod.LOGGER.info("Wrote NBT for pet: {}", getUuid());
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
                loadInventoryAsync();
                ArcaniaTestMod.LOGGER.info("Read NBT and restored pet: {} for player: {}", getUuid(), getOwnerUuid());
            } else {
                ArcaniaTestMod.LOGGER.info("Pet loaded but owner not found: {}", getUuid());
            }
        }
    }

    @Override
    public void setOwner(PlayerEntity player) {
        super.setOwner(player);
        if (player != null) {
            this.ownerName = player.getName().getString();
            PetManager.registerPet(player, this);
            loadInventoryAsync();
            ArcaniaTestMod.LOGGER.info("Set owner for pet: {} to player: {}", getUuid(), player.getUuid());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.getWorld().isClient && getOwner() != null && reason == RemovalReason.DISCARDED) {
            saveInventorySync();
            PetManager.unregisterPet((PlayerEntity) getOwner());
            ArcaniaTestMod.LOGGER.info("Removed pet: {} for player: {}", getUuid(), getOwnerUuid());
        }
        super.remove(reason);
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

    private static class FollowOwnerGoal extends Goal {
        private final PetEntity pet;
        private final double speed;
        private final float maxDistance;
        private final float desiredDistance;
        private PlayerEntity owner;
        private int updateCountdownTicks;

        public FollowOwnerGoal(PetEntity pet, double speed, float maxDistance, float desiredDistance) {
            this.pet = pet;
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.desiredDistance = desiredDistance;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            this.owner = (PlayerEntity) this.pet.getOwner();
            if (this.owner == null || this.pet.isSitting()) {
                return false;
            }
            double distance = this.pet.squaredDistanceTo(this.owner);
            return distance > this.desiredDistance * this.desiredDistance;
        }

        @Override
        public boolean shouldContinue() {
            if (this.owner == null || this.pet.isSitting()) {
                return false;
            }
            double distance = this.pet.squaredDistanceTo(this.owner);
            return distance > this.desiredDistance * this.desiredDistance;
        }

        @Override
        public void start() {
            this.updateCountdownTicks = 0;
        }

        @Override
        public void stop() {
            this.owner = null;
            this.pet.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (this.owner == null) return;

            this.pet.getLookControl().lookAt(this.owner, 30.0F, 30.0F);
            if (--this.updateCountdownTicks <= 0) {
                this.updateCountdownTicks = 10;
                double distance = this.pet.squaredDistanceTo(this.owner);
                if (distance > this.maxDistance * this.maxDistance) {
                    this.teleportToOwner();
                } else if (distance > this.desiredDistance * this.desiredDistance) {
                    this.pet.getNavigation().startMovingTo(this.owner, this.speed);
                } else {
                    this.pet.getNavigation().stop();
                }
            }
        }

        private void teleportToOwner() {
            if (this.owner == null || !(this.pet.getWorld() instanceof ServerWorld)) return;

            Vec3d ownerPos = this.owner.getPos();
            for (int i = 0; i < 10; ++i) {
                double x = ownerPos.x + (this.pet.random.nextDouble() - 0.5D) * 4.0D;
                double y = ownerPos.y + (this.pet.random.nextDouble() - 0.5D) * 4.0D;
                double z = ownerPos.z + (this.pet.random.nextDouble() - 0.5D) * 4.0D;
                if (this.pet.teleport(x, y, z, false)) return;
            }
        }
    }
}