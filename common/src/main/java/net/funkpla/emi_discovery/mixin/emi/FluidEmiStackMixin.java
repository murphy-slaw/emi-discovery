package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FluidEmiStack.class)
public class FluidEmiStackMixin {
    /**
     * Fluid sprites reset the shader colour before drawing, so they ignore the blackout tint (draw a plain silhouette
     * instead while a blackout is being rendered).
     */
    @WrapOperation(
            remap = false,
            method = "render",
            at = @At(
                    value = "INVOKE",
                    remap = true,
                    target = "Ldev/emi/emi/platform/EmiAgnos;renderFluid(Ldev/emi/emi/api/stack/FluidEmiStack;Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"))
    private void blackoutFluid(
            FluidEmiStack stack, PoseStack matrices, int x, int y, float delta, Operation<Void> original,
            @Local(argsOnly = true) GuiGraphics draw) {
        if (KnownItems.isRenderingBlackout()) {
            draw.fill(x, y, x + 16, y + 16, 0xFF000000);
        } else {
            original.call(stack, matrices, x, y, delta);
        }
    }
}
