package net.funkpla.emi_discovery.mixin.remi;

import com.evandev.remi.feature.stackgroup.EmiGroupStack;
import com.evandev.remi.feature.stackgroup.GroupedEmiStack;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.config.EmiConfig;
import net.funkpla.emi_discovery.KnownItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EmiGroupStack.class)
public class EmiGroupStackMixin {

    /**
     * Filter undiscovered items from REMI's EmiGroupStack.
     */
    @ModifyReturnValue(method = "getItems", at = @At("RETURN"), remap = false)
    private List<GroupedEmiStack<EmiStack>> filterUndiscoveredGroupItems(List<GroupedEmiStack<EmiStack>> original) {
        if (!KnownItems.isModEnabled() || !KnownItems.shouldFilterIndex() || EmiConfig.editMode) {
            return original;
        }
        return original.stream()
                .filter(item -> KnownItems.shouldStackDisplay(item.realStack))
                .toList();
    }
}
