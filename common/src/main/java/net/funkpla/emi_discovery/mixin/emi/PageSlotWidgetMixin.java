package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.emi.emi.api.recipe.EmiIngredientRecipe$PageSlotWidget")
public abstract class PageSlotWidgetMixin extends SlotWidgetMixin {

    @Unique
    private boolean emi_discovery$drawIcon = false;

    /**
     * Override the wrapper for SlotWidget.drawSlotHighlight() so we can skip it if the item isn't
     * known.
     */
    @Unique
    @Override
    protected void overrideDrawSlotHighlight(
            GuiGraphics draw, Bounds bounds, Operation<Void> original) {
        if (KnownItems.shouldBlackoutRecipes() || KnownItems.shouldIngredientDisplay(((SlotWidget) (Object) this).getStack())) {
            original.call(draw, bounds);
        }
    }

    /**
     * Finagle our way into isEmpty(), used to filter out empty recipes, so we can also remove recipes
     * with unknown ingredients.
     *
     * @param ingredient the ingredient to test
     * @param original   original operation (unused)
     * @return false if none of the items in the ingredient are known
     */
    @WrapOperation(
            remap = false,
            method = "render",
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/stack/EmiIngredient;isEmpty()Z"))
    private boolean filterPageSlots(EmiIngredient ingredient, Operation<Boolean> original) {
        emi_discovery$drawIcon = KnownItems.shouldBlackoutRecipes() || KnownItems.shouldIngredientDisplay(ingredient);
        return original.call(ingredient);
    }

    /**
     * Inject to draw the slot background and then cancel without drawing the icon if the ingredient
     * is not known
     */
    @Inject(
            method = "render",
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Ldev/emi/emi/api/widget/SlotWidget;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void drawBackgroundAnyway(
            GuiGraphics draw, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!emi_discovery$drawIcon && !KnownItems.shouldBlackoutRecipes()) {
            ((SlotWidget) (Object) this).drawBackground(draw, mouseX, mouseY, delta);
            ci.cancel();
        }
    }
}
