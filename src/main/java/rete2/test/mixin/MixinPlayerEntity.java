package rete2.test.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rete2.test.init.TestModItems;
import rete2.test.items.PetWand;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity {

    @Inject(at = @At("HEAD"), method = "dropInventory")
    private void onDropInventory(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof PetWand) {
                player.getInventory().setStack(i, ItemStack.EMPTY);
                break;
            }
        }

    }

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onInit(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        player.getInventory().setStack(8, new ItemStack(TestModItems.PET_WAND, 1));
    }

    @Inject(at = @At("HEAD"), method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", cancellable = true)
    private void onItemDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (!stack.isEmpty()) {
            ItemStack originalStack = stack.copy();
            if (originalStack.getItem() instanceof PetWand) {
                player.sendMessage(Text.of("Вы не можете выбросить PetWand!"), false);
                ItemStack currentSlot8 = player.getInventory().getStack(8);
                if (currentSlot8.isEmpty()) {
                    player.getInventory().setStack(8, originalStack);
                } else player.getInventory().insertStack(originalStack);
                ci.setReturnValue(null);
                ci.cancel();
            }
        }
    }
}
