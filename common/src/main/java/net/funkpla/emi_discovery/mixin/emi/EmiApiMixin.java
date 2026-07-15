package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import net.funkpla.emi_discovery.CommonClass;
import net.funkpla.emi_discovery.EmiDiscoveryConfig;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EmiApi.class)
public abstract class EmiApiMixin {

  /**
   * Cancel getting a stack if there are no recipes that can be crafted from known ingredients. This
   * will prevent the tab for the category from being drawn.
   */
  @Inject(
      remap = false,
      method = "displayRecipes(Ldev/emi/emi/api/stack/EmiIngredient;)V",
      at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"),
      cancellable = true)
  private static void stopEmptyRecipeTabs(EmiIngredient stack, CallbackInfo ci) {
    if (EmiApi.getRecipeManager().getRecipesByOutput(stack.getEmiStacks().get(0)).stream()
        .noneMatch(KnownItems::areAllKnown)) ci.cancel();
  }

  /** Filter out unknown recipes when building the recipe display. */
  @WrapOperation(
      remap = false,
      method = "setPages",
      at = @At(value = "INVOKE", target = "Ljava/util/Set;stream()Ljava/util/stream/Stream;"))
  private static Stream<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> filterRecipeMap(
      Set<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> instance,
      Operation<Stream<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>>> original) {
      if (CommonClass.isDisabled())
          return original.call(instance);
    return KnownItems.filterEntrySet(instance);
  }
}
