package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.ItemEmiStack;
import dev.emi.emi.runtime.EmiDrawContext;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ItemEmiStack.class)
public class ItemEmiStackMixin {

    /**
     * Suppress original numeric amount rendering when blackout is enabled and stack is undiscovered
     */
    @WrapOperation(
            remap = false,
            method = "render",
            at =
            @At(
                    value = "INVOKE",
                    target = "Ldev/emi/emi/EmiRenderHelper;renderAmount(Ldev/emi/emi/runtime/EmiDrawContext;IILnet/minecraft/network/chat/Component;)V"))
    private void suppressRenderAmount(
            EmiDrawContext context, int x, int y, Component amount, Operation<Void> original) {
        if (KnownItems.shouldBlackoutRecipes()
                && !KnownItems.shouldStackDisplay((ItemEmiStack) (Object) this)) {
            return;
        }
        original.call(context, x, y, amount);
    }
}
