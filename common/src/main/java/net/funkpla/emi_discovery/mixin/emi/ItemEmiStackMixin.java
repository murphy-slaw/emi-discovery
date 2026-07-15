package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.emi.api.stack.ItemEmiStack;
import java.util.List;
import net.funkpla.emi_discovery.CommonClass;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ItemEmiStack.class)
public class ItemEmiStackMixin {
  /** Empty out the tooltip for unknown stacks. */
  @Inject(
      method = "getTooltip",
      at =
          @At(
              value = "INVOKE",
              target = "Ldev/emi/emi/api/stack/ItemEmiStack;isEmpty()Z",
              shift = At.Shift.AFTER),
      remap = false,
      cancellable = true)
  private void killTooltip(
      CallbackInfoReturnable<List<ClientTooltipComponent>> cir,
      @Local(name = "stack") ItemStack stack,
      @Local(name = "list") List<ClientTooltipComponent> list) {
    if (!CommonClass.isDisabled() && !KnownItems.shouldStackDisplay(ItemEmiStack.of(stack))) {
      cir.setReturnValue(list);
    }
  }
}
