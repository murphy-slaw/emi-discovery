package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.screen.tooltip.IngredientTooltipComponent;
import net.funkpla.emi_discovery.KnownItems;
import net.funkpla.emi_discovery.mixin.emi.accessor.IngredientTooltipComponentAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(IngredientTooltipComponent.class)
public class IngredientTooltipComponentMixin {

    @WrapOperation(
            remap = false,
            method = {"getHeight", "getStackWidth", "drawTooltip"},
            at =
            @At(
                    value = "FIELD",
                    target =
                            "Ldev/emi/emi/screen/tooltip/IngredientTooltipComponent;ingredients:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD))
    private List<? extends EmiIngredient> filterIngredients(
            IngredientTooltipComponent component, Operation<List<? extends EmiIngredient>> original) {
        return ((IngredientTooltipComponentAccessor) component)
                .getIngredients().stream().filter(KnownItems::shouldStackDisplay).toList();
    }
}
