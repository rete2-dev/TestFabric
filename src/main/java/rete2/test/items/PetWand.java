package rete2.test.items;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import rete2.test.client.renders.PetWandRenderer;
import rete2.test.entities.PetEntity;
import rete2.test.init.TestModEntities;
import rete2.test.logic.PetInventoryStorage;
import rete2.test.logic.PetManager;
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
            List<PetEntity> existingPets = world.getEntitiesByClass(PetEntity.class, user.getBoundingBox().expand(100.0), pet -> pet.getOwnerUuid() != null && pet.getOwnerUuid().equals(user.getUuid()));
            if (!existingPets.isEmpty()) {
                for (PetEntity pet : existingPets) {
                    pet.discard();
                    PetManager.unregisterPet(user);
                }
                user.sendMessage(Text.of("Питомец деспавнился!"), false);
            } else {
                PetEntity pet = new PetEntity(TestModEntities.PET_ENTITY, world);
                pet.setPosition(user.getX(), user.getY(), user.getZ());
                pet.setOwner(user);
                pet.setTamed(true);
                if (world.spawnEntity(pet)) {
                    PetManager.registerPet(user, pet);
                    user.sendMessage(Text.of("Питомец призван!"), false);
                } else {
                    user.sendMessage(Text.of("Не удалось призвать питомца!"), false);
                }
            }
        }
        return TypedActionResult.success(itemStack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.arcania_test.pet_wand.tooltip1"));
        tooltip.add(Text.translatable("item.arcania_test.pet_wand.tooltip2"));
        tooltip.add(Text.translatable("item.arcania_test.pet_wand.tooltip3"));
        tooltip.add(Text.translatable("item.arcania_test.pet_wand.tooltip4"));
        tooltip.add(Text.translatable("item.arcania_test.pet_wand.tooltip5"));
        if (Screen.hasShiftDown()) {
            tooltip.add(Text.translatable("item.arcania_test.pet_wand.shift_tooltip1"));
            tooltip.add(Text.translatable("item.arcania_test.pet_wand.shift_tooltip2"));
        } else tooltip.add(Text.translatable("item.arcania_test.pet_wand.tooltip6"));
    }
}