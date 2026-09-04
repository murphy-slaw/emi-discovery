package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import net.funkpla.emi_discovery.KnownItems;
import net.funkpla.emi_discovery.mixin.emi.accessor.TagEmiIngredientAccessor;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
@Mixin(TagEmiIngredient.class)
public class TagEmiIngredientMixin {

    @WrapOperation(
            remap = false,
            method = "render",
            at =
            @At(
                    value = "FIELD",
                    target = "Ldev/emi/emi/api/stack/TagEmiIngredient;stacks:Ljava/util/List;",
                    opcode = Opcodes.GETFIELD))
    private List<EmiStack> filterStacks(
            TagEmiIngredient tagEmiIngredient, Operation<List<EmiStack>> original) {
        if (!KnownItems.isModEnabled()) {
            return original.call(tagEmiIngredient);
        }
        List<EmiStack> allStacks = ((TagEmiIngredientAccessor) tagEmiIngredient).getStacks();
        if (allStacks == null || allStacks.isEmpty()) {
            return original.call(tagEmiIngredient);
        }
        List<EmiStack> known = allStacks.stream().filter(KnownItems::shouldStackDisplay).toList();
        return known.isEmpty() ? allStacks : known;
    }
}
