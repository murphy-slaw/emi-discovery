package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.ListEmiIngredient;
import net.funkpla.emi_discovery.KnownItems;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(ListEmiIngredient.class)
public class ListEmiIngredientMixin {

    @WrapOperation(
            remap = false,
            method = "render",
            at =
            @At(
                    value = "FIELD",
                    target = "Ldev/emi/emi/api/stack/ListEmiIngredient;ingredients:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD))
    private List<? extends EmiIngredient> filterIngredients(
            ListEmiIngredient listEmiIngredient, Operation<List<? extends EmiIngredient>> original) {
        List<? extends EmiIngredient> all = original.call(listEmiIngredient);
        if (all == null || all.isEmpty()) {
            return all;
        }
        List<? extends EmiIngredient> known = all.stream()
                .filter(KnownItems::shouldIngredientDisplay)
                .toList();
        return known.isEmpty() ? all : known;
    }
}
