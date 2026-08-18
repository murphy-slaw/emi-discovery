package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.ListEmiIngredient;
import java.util.List;
import net.funkpla.emi_discovery.KnownItems;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ListEmiIngredient.class)
public class ListEmiIngredientMixin {
  @Unique
  private @NotNull List<? extends EmiIngredient> emi_discovery$filterEmiIngredients(
      ListEmiIngredient listEmiIngredient) {
    return listEmiIngredient.getIngredients().stream()
        .filter(KnownItems::shouldIngredientDisplay)
        .toList();
  }

  @WrapOperation(
      remap = false,
      method = "render",
      at =
          @At(
              value = "FIELD",
              target = "Ldev/emi/emi/api/stack/ListEmiIngredient;ingredients:Ljava/util/List;",
              opcode = Opcodes.GETFIELD))
  private List<? extends EmiIngredient> filterRenderedStacks(
      ListEmiIngredient listEmiIngredient, Operation<List<? extends EmiIngredient>> original) {
    return emi_discovery$filterEmiIngredients(listEmiIngredient);
  }

  @WrapOperation(
      remap = false,
      method = "getTooltip",
      at =
          @At(
              value = "FIELD",
              target = "Ldev/emi/emi/api/stack/ListEmiIngredient;ingredients:Ljava/util/List;",
              opcode = Opcodes.GETFIELD))
  private List<? extends EmiIngredient> filterTooltipStacks(
      ListEmiIngredient listEmiIngredient, Operation<List<? extends EmiIngredient>> original) {
    return emi_discovery$filterEmiIngredients(listEmiIngredient);
  }
}
