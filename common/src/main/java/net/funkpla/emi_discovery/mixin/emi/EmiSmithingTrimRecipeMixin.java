package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.special.EmiSmithingTrimRecipe;
import java.util.List;
import net.funkpla.emi_discovery.KnownItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EmiSmithingTrimRecipe.class)
public class EmiSmithingTrimRecipeMixin {
  @WrapOperation(
      remap = false,
      method = "getStack",
      at =
          @At(
              value = "INVOKE",
              target = "Ldev/emi/emi/api/stack/EmiIngredient;getEmiStacks()Ljava/util/List;"))
  private List<EmiStack> filterUnknownTrimMaterials(
      EmiIngredient ingredient, Operation<List<EmiStack>> original) {
    if (KnownItems.shouldBlackoutRecipes()) {
      return original.call(ingredient);
    }
    return ingredient.getEmiStacks().stream().filter(KnownItems::shouldStackDisplay).toList();
  }
}
