package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.screen.tooltip.IngredientTooltipComponent;
import java.util.List;
import net.funkpla.emi_discovery.KnownItems;
import net.funkpla.emi_discovery.mixin.emi.accessor.IngredientTooltipComponentAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Debug(export = true)
@Mixin(IngredientTooltipComponent.class)
public class IngredientTooltipComponentMixin {
  @Unique
  private List<? extends EmiIngredient> emi_discovery$filterIngredients(IngredientTooltipComponent component) {
    return ((IngredientTooltipComponentAccessor) component)
        .getIngredients().stream().filter(KnownItems::shouldStackDisplay).toList();
  }

  @WrapOperation(
      remap = false,
      method = "getHeight",
      at =
          @At(
              value = "FIELD",
              target =
                  "Ldev/emi/emi/screen"
                      + "/tooltip/IngredientTooltipComponent;ingredients:Ljava/util/List;",
              opcode = Opcodes.GETFIELD))
  private List<? extends EmiIngredient> fixStackWidth(
      IngredientTooltipComponent component, Operation<List<? extends EmiIngredient>> original) {
    return emi_discovery$filterIngredients(component);
  }

  @WrapOperation(
      remap = false,
      method = "getStackWidth",
      at =
          @At(
              value = "FIELD",
              target =
                  "Ldev/emi/emi/screen"
                      + "/tooltip/IngredientTooltipComponent;ingredients:Ljava/util/List;",
              opcode = Opcodes.GETFIELD))
  private List<? extends EmiIngredient> fixHeight(
      IngredientTooltipComponent component, Operation<List<? extends EmiIngredient>> original) {
    return emi_discovery$filterIngredients(component);
  }

  @WrapOperation(
      remap = false,
      method = "drawTooltip",
      at =
          @At(
              value = "FIELD",
              target =
                  "Ldev/emi/emi/screen"
                      + "/tooltip/IngredientTooltipComponent;ingredients:Ljava/util/List;",
              opcode = Opcodes.GETFIELD))
  private List<? extends EmiIngredient> filterComponents(
      IngredientTooltipComponent instance, Operation<List<? extends EmiIngredient>> original) {
    return emi_discovery$filterIngredients(instance);
  }
}
