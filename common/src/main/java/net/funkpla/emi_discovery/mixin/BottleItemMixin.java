package net.funkpla.emi_discovery.mixin;

import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BottleItem.class)
public class BottleItemMixin {

    @Inject(method = "use", at = @At("RETURN"))
    private void emi_discovery$onBottleUse(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide() && cir.getReturnValue() != null && cir.getReturnValue().getResult().consumesAction()) {
            KnownItems.addKnown(Fluids.WATER);
        }
    }
}
