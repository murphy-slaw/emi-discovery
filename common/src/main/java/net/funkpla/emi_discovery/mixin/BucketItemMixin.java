package net.funkpla.emi_discovery.mixin;

import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public class BucketItemMixin {

    @Inject(method = "use", at = @At("RETURN"))
    private void emi_discovery$onBucketUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide() && cir.getReturnValue() != null && cir.getReturnValue().getResult().consumesAction()) {
            ItemStack result = cir.getReturnValue().getObject();
            if (!result.isEmpty()) {
                KnownItems.addKnown(result);
            }
            BucketItem bucket = (BucketItem) (Object) this;
            try {
                Fluid fluid = ((BucketItemAccessor) bucket).emi_discovery$getContent();
                if (fluid != null && fluid != Fluids.EMPTY) {
                    KnownItems.addKnown(fluid);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
