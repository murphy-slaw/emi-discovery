package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.screen.EmiScreenManager;
import net.funkpla.emi_discovery.KnownItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EmiScreenManager.class)
public class EmiScreenManagerMixin {
  /**
   * Stop usage lookups for unknown ingredients.
   *
   * @param fav the ingredient being requested
   * @param original original operation, called if the ingredient is known
   */
  @WrapOperation(
      remap = false,
      method =
          "stackInteraction(Ldev/emi/emi/api/stack/EmiStackInteraction;Ljava/util/function/Function;)Z",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Ldev/emi/emi/api/EmiApi;displayUses(Ldev/emi/emi/api/stack/EmiIngredient;)V"))
  private static void stopUseLookup(EmiIngredient fav, Operation<Void> original) {
    if (KnownItems.isKnown(fav) || KnownItems.allowUsageLookupForUndiscovered()) {
      original.call(fav);
    }
  }

  /**
   * Stop recipe lookups for unknown ingredients.
   *
   * @param fav the ingredient being requested
   * @param original original operation, called if the ingredient is known
   */
  @WrapOperation(
      remap = false,
      method =
          "stackInteraction(Ldev/emi/emi/api/stack/EmiStackInteraction;Ljava/util/function/Function;)Z",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Ldev/emi/emi/api/EmiApi;displayRecipes(Ldev/emi/emi/api/stack/EmiIngredient;)V"))
  private static void stopRecipeLookup(EmiIngredient fav, Operation<Void> original) {
    if (KnownItems.areAnyKnown(fav) || KnownItems.allowRecipeLookupForUndiscovered()) {
      original.call(fav);
    }
  }
}
