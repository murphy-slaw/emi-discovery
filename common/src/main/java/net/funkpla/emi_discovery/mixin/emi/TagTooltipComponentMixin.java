package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.tooltip.EmiTooltipComponent;
import dev.emi.emi.screen.tooltip.TagTooltipComponent;
import net.funkpla.emi_discovery.KnownItems;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(TagTooltipComponent.class)
public class TagTooltipComponentMixin {

    @Final
    @Shadow(remap = false)
    private List<EmiStack> stacks;

    @ModifyVariable(method = "<init>", at = @At("HEAD"), argsOnly = true, remap = false)
    private static List<EmiStack> filterStacks(List<EmiStack> stacks) {
        if (stacks == null) return null;
        if (!KnownItems.isModEnabled() || KnownItems.shouldBlackoutRecipes()) {
            return stacks.stream().filter(s -> !s.isEmpty()).toList();
        }
        return stacks.stream().filter(s -> !s.isEmpty() && KnownItems.shouldStackDisplay(s)).toList();
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true, remap = false)
    private void fixEmptyHeight(CallbackInfoReturnable<Integer> cir) {
        if (this.stacks == null || this.stacks.isEmpty()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getStackWidth", at = @At("HEAD"), cancellable = true, remap = false)
    private void fixEmptyStackWidth(CallbackInfoReturnable<Integer> cir) {
        if (this.stacks == null || this.stacks.isEmpty()) {
            cir.setReturnValue(1);
        }
    }

    @Inject(method = "drawTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void fixEmptyDraw(EmiDrawContext context, EmiTooltipComponent.TooltipRenderData render, CallbackInfo ci) {
        if (this.stacks == null || this.stacks.isEmpty()) {
            ci.cancel();
        }
    }

    /**
     * Draw a translucent black square instead of an icon for unknown items in the tag recipe tooltip.
     *
     * @param drawContext the wrapped GuiGraphics instance
     * @param stack       the emiIngredient to draw
     * @param x           duh
     * @param y           double duh
     * @param flags       flags for the original call
     * @param original    original operation, called if the item is known
     */
    @WrapOperation(
            remap = false,
            method =
                    "drawTooltip(Ldev/emi/emi/runtime/EmiDrawContext;Ldev/emi/emi/screen/tooltip/EmiTooltipComponent$TooltipRenderData;)V",
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Ldev/emi/emi/runtime/EmiDrawContext;drawStack"
                                    + "(Ldev/emi/emi/api/stack/EmiIngredient;III)V"))
    private void pruneTagTooltip(
            EmiDrawContext drawContext,
            EmiIngredient stack,
            int x,
            int y,
            int flags,
            Operation<Void> original) {
        if (KnownItems.shouldIngredientDisplay(stack)) {
            original.call(drawContext, stack, x, y, flags);
        } else if (KnownItems.shouldBlackoutRecipes()) {
            drawContext.raw().flush();
            RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
            original.call(drawContext, stack, x, y, flags);
            drawContext.raw().flush();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            drawContext.fill(x, y, 16, 16, 0x0FFFFFFF);
        }
    }
}
