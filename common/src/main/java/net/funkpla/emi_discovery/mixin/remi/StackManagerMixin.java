package net.funkpla.emi_discovery.mixin.remi;

import com.evandev.remi.feature.stackgroup.EmiGroupStack;
import com.evandev.remi.integration.emi.StackManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.EmiConfig;
import net.funkpla.emi_discovery.KnownItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@IfModLoaded("remi")
@Mixin(value = StackManager.class, remap = false)
public class StackManagerMixin {
    /**
     * Filter unknown items out of REMI's displayed-stack list.
     * <p>
     * Was originally pretty clean... no longer the case, unfortunately.
     */
    @ModifyReturnValue(method = "buildDisplayedStacks", at = @At("RETURN"))
    private static List<EmiStack> filterDisplayedStacks(List<EmiStack> original) {
        if (!KnownItems.isModEnabled() || !KnownItems.shouldFilterIndex() || EmiConfig.editMode) {
            return original;
        }
        List<EmiStack> result = new ArrayList<>(original.size());
        for (EmiStack stack : original) {
            if (stack instanceof EmiGroupStack gs) {
                var items = gs.getItems();
                if (items.size() > 1) {
                    result.add(gs);
                } else if (items.size() == 1) {
                    result.add(items.get(0).realStack);
                }
            } else if (KnownItems.shouldStackDisplay(stack)) {
                result.add(stack);
            }
        }
        return result;
    }
}
