package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(SlotWidget.class)
public class SlotWidgetMixin {
    /**
     * Wrap the drawSlotHighlight method so that we can override it in subclasses.
     */
    @WrapMethod(method = "drawSlotHighlight", remap = false)
    protected void overrideDrawSlotHighlight(
            GuiGraphics draw, Bounds bounds, Operation<Void> original) {
        original.call(draw, bounds);
    }

    @WrapOperation(
            remap = false,
            method = "drawStack",
            at = @At(value = "INVOKE", remap = true, target = "Ldev/emi/emi/api/stack/EmiIngredient;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void renderSlotStack(
            EmiIngredient instance, GuiGraphics draw, int x, int y, float delta, Operation<Void> original) {
        if (KnownItems.shouldBlackoutRecipes() && !KnownItems.shouldIngredientDisplay(instance)) {
            KnownItems.wrapBlackout(draw, x, y, () -> original.call(instance, draw, x, y, delta));
        } else {
            original.call(instance, draw, x, y, delta);
        }
    }

    @Inject(method = "getTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void obscureSlotTooltip(
            int mouseX, int mouseY, CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        SlotWidget widget = (SlotWidget) (Object) this;
        if (widget.getStack().isEmpty()) return;
        if (KnownItems.shouldBlackoutRecipes()
                && !KnownItems.shouldIngredientDisplay(widget.getStack())
                && KnownItems.shouldObscureTooltips()) {
            List<ClientTooltipComponent> list = new ArrayList<>();
            list.add(ClientTooltipComponent.create(Component.translatable("tooltip.emi_discovery.obscured").getVisualOrderText()));
            cir.setReturnValue(list);
        } else if (!KnownItems.shouldBlackoutRecipes() && !KnownItems.shouldIngredientDisplay(widget.getStack())) {
            cir.setReturnValue(List.of());
        }
    }
}
