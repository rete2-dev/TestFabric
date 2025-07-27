package rete2.test.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.AmphibiousSwimNavigation;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

public class PetEntity extends TameableEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PetEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world);
        setInvulnerable(true); // Делаем питомца неуязвимым
        this.goalSelector.add(1, new FollowOwnerGoal(this, 0.28D, 12.0F, 2.0F));
    }
    @Override
    protected EntityNavigation createNavigation(World world) {
        // Используем AmphibiousNavigation для перемещения по суше и воде
        return new AmphibiousSwimNavigation(this, world);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public @Nullable PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null; // Питомец не размножается
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (!this.getWorld().isClient && this.getOwner() != null) {
            PlayerEntity owner = (PlayerEntity) this.getOwner();
            double distanceToOwner = this.squaredDistanceTo(owner);
            if (distanceToOwner <= 64.0D) { // 8 блоков (8^2 = 64)
                this.lookAtEntity(owner, 30.0F, 30.0F); // Поворачиваем голову к игроку
            }
        }
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (getOwnerUuid() != null) {
            nbt.putUuid("Owner", getOwnerUuid());
        }
        System.out.println("Saved PetEntity NBT for owner UUID: " + getOwnerUuid());
        return nbt;
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.containsUuid("Owner")) {
            setOwnerUuid(nbt.getUuid("Owner"));
        }
        setInvulnerable(true); // Гарантируем неуязвимость после загрузки
        System.out.println("Loaded PetEntity NBT with owner UUID: " + getOwnerUuid());
    }

    @Override
    public boolean isInvulnerable() {
        return true; // Питомец неуязвим для любого урона
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, state ->
                state.isMoving() ? state.setAndContinue(DefaultAnimations.WALK) : state.setAndContinue(DefaultAnimations.IDLE)));
    }

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
        private final float desiredDistance; // Целевое расстояние (2 блока)
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
            return distance > this.desiredDistance * this.desiredDistance; // Начать движение, если дальше 2 блоков
        }

        @Override
        public boolean shouldContinue() {
            if (this.owner == null || this.pet.isSitting()) {
                return false;
            }
            double distance = this.pet.squaredDistanceTo(this.owner);
            return distance > this.desiredDistance * this.desiredDistance; // Продолжать, если дальше 2 блоков
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
                    // Телепортация, если питомец слишком далеко
                    this.teleportToOwner();
                } else if (distance > this.desiredDistance * this.desiredDistance) {
                    // Движение к владельцу, если дальше 2 блоков
                    this.pet.getNavigation().startMovingTo(this.owner, this.speed);
                    System.out.println("PetEntity moving to owner at distance " + Math.sqrt(distance));
                } else {
                    // Остановить движение, если ближе 2 блоков
                    this.pet.getNavigation().stop();
                    System.out.println("PetEntity stopped at distance " + Math.sqrt(distance));
                }
            }
        }

        private void teleportToOwner() {
            if (this.owner == null || !(this.pet.getWorld() instanceof ServerWorld)) return;

            ServerWorld serverWorld = (ServerWorld) this.pet.getWorld();
            Vec3d ownerPos = this.owner.getPos();
            for (int i = 0; i < 10; ++i) {
                double x = ownerPos.x + (this.pet.random.nextDouble() - 0.5D) * 4.0D;
                double y = ownerPos.y + (this.pet.random.nextDouble() - 0.5D) * 4.0D;
                double z = ownerPos.z + (this.pet.random.nextDouble() - 0.5D) * 4.0D;
                if (this.pet.teleport(x, y, z, false)) {
                    System.out.println("PetEntity teleported to owner " + this.owner.getName().getString() + " at position " + new Vec3d(x, y, z));
                    return;
                }
            }
        }
    }
}
