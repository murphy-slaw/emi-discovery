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
public class EmiScreenSpaceMixin {
    /**
     * Black out undiscovered stacks in the sidebars. These are drawn directly instead of through the batcher: batched
     * stacks are baked once and drawn later in {@code StackBatcher.draw()}, so the shader colour wouldn't apply.
     */
    @WrapOperation(
            remap = false,
            method = "render",
            at = @At(
                    value = "INVOKE",
                    remap = true,
                    target = "Ldev/emi/emi/screen/StackBatcher;render(Ldev/emi/emi/api/stack/EmiIngredient;Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void blackoutInSidebar(
            StackBatcher batcher, EmiIngredient stack, GuiGraphics draw, int x, int y, float delta,
            Operation<Void> original) {
        if (KnownItems.shouldBlackoutIndex() && !KnownItems.shouldIngredientDisplay(stack)) {
            KnownItems.renderBlackedOut(draw, x, y,
                    () -> stack.render(draw, x, y, delta, ~EmiIngredient.RENDER_AMOUNT));
        } else {
            original.call(batcher, stack, draw, x, y, delta);
        }
    }
}
