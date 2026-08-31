package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.Minecraft;
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
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/stack/EmiIngredient;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void renderSlotStack(
            EmiIngredient instance, GuiGraphics draw, int x, int y, float delta, Operation<Void> original) {
        if (KnownItems.shouldBlackoutRecipes() && !KnownItems.shouldIngredientDisplay(instance)) {
            RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
            original.call(instance, draw, x, y, delta);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            if (KnownItems.shouldShowQuestionMarkOverlay()) {
                Minecraft client = Minecraft.getInstance();
                String q = "?";
                int textX = x + 16 - client.font.width(q);
                int textY = y + 16 - client.font.lineHeight + 1;
                draw.pose().pushPose();
                draw.pose().translate(0, 0, 200);
                draw.drawString(client.font, q, textX, textY, 0xFFE0E0E0, true);
                draw.pose().popPose();
            }
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
        }
    }
}
