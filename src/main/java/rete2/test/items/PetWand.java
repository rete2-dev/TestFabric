package rete2.test.items;

import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import rete2.test.client.renders.PetWandRenderer;
import rete2.test.entities.PetEntity;
import rete2.test.init.TestModEntities;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.constant.DefaultAnimations;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PetWand extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public PetWand(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private PetWandRenderer renderer;

            public BuiltinModelItemRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new PetWandRenderer();
                }

                return this.renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return this.renderProvider;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, 20, (state) -> {
            state.getController().setAnimation(DefaultAnimations.IDLE);
            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


    @Override
    public boolean canBeNested() {
        return false;
    }


    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        if (!world.isClient) {
            // Проверяем, есть ли у игрока уже питомец
            List<PetEntity> existingPets = world.getEntitiesByClass(PetEntity.class, user.getBoundingBox().expand(100.0), pet -> pet.getOwnerUuid() != null && pet.getOwnerUuid().equals(user.getUuid()));
            if (!existingPets.isEmpty()) {
                // Деспавним существующего питомца
                for (PetEntity pet : existingPets) {
                    pet.discard();
                    System.out.println("Despawned PetEntity for player " + user.getName().getString());
                }
                user.sendMessage(Text.of("Питомец деспавнился!"), false);
            } else {
                // Призываем нового питомца
                PetEntity pet = new PetEntity(TestModEntities.PET_ENTITY, world);
                pet.setPosition(user.getX(), user.getY(), user.getZ());
                pet.setOwner(user);
                pet.setTamed(true);
                if (world.spawnEntity(pet)) {
                    user.sendMessage(Text.of("Питомец призван!"), false);
                    System.out.println("Spawned PetEntity for player " + user.getName().getString() + " at position " + pet.getPos());
                } else {
                    user.sendMessage(Text.of("Не удалось призвать питомца!"), false);
                    System.out.println("Failed to spawn PetEntity for player " + user.getName().getString());
                }
            }
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }
}
