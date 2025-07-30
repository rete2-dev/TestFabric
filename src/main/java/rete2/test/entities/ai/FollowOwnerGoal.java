package rete2.test.entities.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import rete2.test.entities.PetEntity;

import java.util.EnumSet;

public class FollowOwnerGoal extends Goal {
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
            double x = ownerPos.x + (this.pet.getRandom().nextDouble() - 0.5D) * 4.0D;
            double y = ownerPos.y + (this.pet.getRandom().nextDouble() - 0.5D) * 4.0D;
            double z = ownerPos.z + (this.pet.getRandom().nextDouble() - 0.5D) * 4.0D;
            if (this.pet.teleport(x, y, z, false)) return;
        }
    }
}
