package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.tooltip.TagTooltipComponent;
import net.funkpla.emi_discovery.KnownItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TagTooltipComponent.class)
public class TagTooltipComponentMixin {

  /**
   * Draw a translucent black square instead of an icon for unknown items in the tag recipe tooltip.
   *
   * @param drawContext the wrapped GuiGraphics instance
   * @param emiIngredient the emiIngredient to draw
   * @param x duh
   * @param y double duh
   * @param flags flags for the original call
   * @param original original operation, called if the item is known
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
      EmiIngredient emiIngredient,
      int x,
      int y,
      int flags,
      Operation<Void> original) {
    if (KnownItems.shouldIngredientDisplay(emiIngredient)) {
      original.call(drawContext, emiIngredient, x, y, flags);
    } else if (KnownItems.shouldBlackoutRecipes()) {
      RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
      original.call(drawContext, emiIngredient, x, y, flags);
      RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    } else {
      drawContext.fill(x, y, 16, 16, 0x0FFFFFFF);
    }
  }
}
