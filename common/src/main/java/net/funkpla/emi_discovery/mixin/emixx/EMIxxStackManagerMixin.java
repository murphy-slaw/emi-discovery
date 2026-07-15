package net.funkpla.emi_discovery.mixin.emixx;

import concerrox.emixx.content.StackManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.List;
import net.funkpla.emi_discovery.CommonClass;
import net.funkpla.emi_discovery.KnownItems;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StackManager.class)
public class EMIxxStackManagerMixin {

  @Unique @Nullable private List<EmiStack> filteredStackCache = null;
  @Unique private int updateCount = 0;

  /**
   * Get the internal list of displayed stacks from the StackManager and filter out stacks with no
   * known items.
   *
   * @return the filtered list
   */
  @Unique
  private synchronized List<EmiStack> getFilteredStacks() {
    if (filteredStackCache == null || updateCount != KnownItems.getUpdateCount()) {
      filteredStackCache =
          ((EMIxxStackManagerAccessor) this)
              .getInternalDisplayedStacks().stream()
                  .filter(KnownItems::shouldStackDisplay)
                  .toList();
      updateCount = KnownItems.getUpdateCount();
    }
    return this.filteredStackCache;
  }

  /**
   * Replace the return value of StackManager.displayedStacks with a list filtered for known items.
   *
   * @param returnable to set the return value
   */
  @Inject(
      remap = false,
      method = "getDisplayedStacks$emixx_common",
      at = @At("HEAD"),
      cancellable = true)
  private void filterStacks(CallbackInfoReturnable<List<EmiStack>> returnable) {
    if (!CommonClass.isDisabled()) returnable.setReturnValue(getFilteredStacks());
  }

  /** Invalidate the cache when a stack is toggled. */
  @Inject(remap = false, method = "onStackInteractionDeprecated", at = @At("HEAD"))
  private void clearFilteredCacheOnStack(EmiIngredient ingredient, CallbackInfo ci) {
    filteredStackCache = null;
  }

  /** Invalidate the cache when displayed stack list is rebuilt */
  @Inject(remap = false, method = "buildDisplayedStacks", at = @At("HEAD"))
  private void clearFilteredCacheDisplayed(CallbackInfo ci) {
    filteredStackCache = null;
  }
}
