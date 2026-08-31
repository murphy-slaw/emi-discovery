package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.recipe.EmiIngredientRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.recipe.EmiTagRecipe;
import net.funkpla.emi_discovery.KnownItems;
import net.funkpla.emi_discovery.mixin.emi.accessor.EmiTagRecipeAccessor;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(EmiIngredientRecipe.class)
public class EmiIngredientRecipeMixin {
    /**
     * Filter unknown items from the stacks returned by getStacks() if the recipe is a TagRecipe. If
     * the resulting list is empty, return a list with one empty ingredient to prevent an exception.
     *
     * @param ingredientRecipe the recipe to filter
     * @param original         original operation, called for non-TagRecipes
     * @return ingredient list with unknown items removed, or a list of one empty ingredient
     */
    @SuppressWarnings("UnstableApiUsage")
    @WrapOperation(
            remap = false,
            method = "getInputs",
            at =
            @At(
                    target = "Ldev/emi/emi/api/recipe/EmiIngredientRecipe;getStacks()Ljava/util/List;",
                    value = "INVOKE"))
    private List<EmiIngredient> filterInputs(
            EmiIngredientRecipe ingredientRecipe, Operation<List<EmiIngredient>> original) {
        if (ingredientRecipe instanceof EmiTagRecipe tagRecipe) {
            if (KnownItems.shouldBlackoutRecipes()) {
                return original.call(ingredientRecipe);
            }

            List<EmiIngredient> emiIngredients = new ArrayList<>();
            emiIngredients.add(new ListEmiIngredient(((EmiTagRecipeAccessor) tagRecipe).getStacks(), 1L));
            List<EmiIngredient> filtered = emiIngredients.stream().filter(KnownItems::shouldIngredientDisplay).toList();

            return filtered.isEmpty() ? List.of(EmiIngredient.of(Ingredient.EMPTY)) : filtered;
        }
        return original.call(ingredientRecipe);
    }
}
