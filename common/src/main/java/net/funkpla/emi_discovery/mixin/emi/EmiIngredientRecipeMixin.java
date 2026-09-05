package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.recipe.EmiIngredientRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.recipe.EmiTagRecipe;
import net.funkpla.emi_discovery.KnownItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(EmiIngredientRecipe.class)
public class EmiIngredientRecipeMixin {
    /**
     * Filter unknown items from the stacks returned by getStacks() if the recipe is a TagRecipe. If
     * the resulting list is empty, return a list with one empty stack to prevent an exception.
     *
     * @param ingredientRecipe the recipe to filter
     * @param original         original operation, called for non-TagRecipes
     * @return stack list with unknown items removed, or a list of one empty stack
     */
    @WrapOperation(
            remap = false,
            method = "getInputs",
            at =
            @At(
                    target = "Ldev/emi/emi/api/recipe/EmiIngredientRecipe;getStacks()Ljava/util/List;",
                    value = "INVOKE"))
    private List<EmiStack> filterInputs(
            EmiIngredientRecipe ingredientRecipe, Operation<List<EmiStack>> original) {
        List<EmiStack> stacks = original.call(ingredientRecipe);
        if (stacks == null || stacks.isEmpty()) {
            return List.of(EmiStack.EMPTY);
        }
        if (ingredientRecipe instanceof EmiTagRecipe) {
            if (KnownItems.shouldBlackoutRecipes()) {
                return stacks;
            }

            List<EmiStack> filtered = stacks.stream().filter(KnownItems::shouldStackDisplay).toList();
            return filtered.isEmpty() ? List.of(EmiStack.EMPTY) : filtered;
        }
        return stacks;
    }

    /**
     * Filter unknown items from the display stacks used for sizing and building slot widgets in tag pages.
     */
    @WrapOperation(
            remap = false,
            method = {"getDisplayHeight", "addWidgets"},
            at =
            @At(
                    target = "Ldev/emi/emi/api/recipe/EmiIngredientRecipe;getStacks()Ljava/util/List;",
                    value = "INVOKE"))
    private List<EmiStack> filterDisplayStacks(
            EmiIngredientRecipe ingredientRecipe, Operation<List<EmiStack>> original) {
        List<EmiStack> stacks = original.call(ingredientRecipe);
        if (stacks == null || stacks.isEmpty()) {
            return List.of();
        }
        if (KnownItems.shouldBlackoutRecipes()) {
            return stacks;
        }
        return stacks.stream().filter(KnownItems::shouldStackDisplay).toList();
    }
}
