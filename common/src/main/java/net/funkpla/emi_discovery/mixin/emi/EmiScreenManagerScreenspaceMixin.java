package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.screen.StackBatcher;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "dev.emi.emi.screen.EmiScreenManager$ScreenSpace")
public class EmiScreenManagerScreenspaceMixin {

    @WrapOperation(remap = false, method = "render", at = @At(value = "INVOKE", target = "Ldev/emi/emi/screen" +
            "/StackBatcher;render(Ldev/emi/emi/api/stack/EmiIngredient;Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private static void blackoutInIndex(StackBatcher instance, EmiIngredient stack, GuiGraphics draw, int x, int y,
                                        float delta, Operation<Void> original) {
        if (KnownItems.shouldBlackoutIndex() && !KnownItems.isKnown(stack)) {
            KnownItems.wrapBlackout(draw, x, y, () -> original.call(instance, stack, draw, x, y, delta));
        } else {
            original.call(instance, stack, draw, x, y, delta);
        }
    }
}
