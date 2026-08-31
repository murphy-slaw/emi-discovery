package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.screen.RecipeScreen;
import net.funkpla.emi_discovery.KnownItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(RecipeScreen.class)
public abstract class RecipeScreenMixin {

    @Unique
    private static List<EmiIngredient> emi_discovery$filterWorkstations(
            EmiRecipeManager recipeManager,
            EmiRecipeCategory emiRecipeCategory,
            Operation<List<EmiIngredient>> original) {
        if (!KnownItems.isModEnabled() || KnownItems.shouldBlackoutRecipes() || KnownItems.displayWithUnknownWorkstation()) {
            return original.call(recipeManager, emiRecipeCategory);
        }
        return KnownItems.workstationsFiltered(emiRecipeCategory);
    }

    /**
     * Intercept the call to getWorkstations() to filter out unknown workstations from the recipe
     * display.
     *
     * @param recipeManager     the recipe manager to get the workstations
     * @param emiRecipeCategory the category to filter
     * @param original          original operation (unused)
     * @return a filtered list of workstations
     */
    @WrapOperation(
            remap = false,
            method = "setPage",
            at =
            @At(
                    target =
                            "Ldev/emi/emi/api/recipe/EmiRecipeManager;getWorkstations(Ldev/emi/emi/api/recipe/EmiRecipeCategory;)Ljava/util/List;",
                    value = "INVOKE"))
    public List<EmiIngredient> filterUnknownWorkstations(
            EmiRecipeManager recipeManager,
            EmiRecipeCategory emiRecipeCategory,
            Operation<List<EmiIngredient>> original) {
        return emi_discovery$filterWorkstations(recipeManager, emiRecipeCategory, original);
    }

    @WrapOperation(
            method = "render",
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Ldev/emi/emi/api/recipe/EmiRecipeManager;getWorkstations(Ldev/emi/emi/api/recipe/EmiRecipeCategory;)Ljava/util/List;"))
    private List<EmiIngredient> filterWorkstations(
            EmiRecipeManager recipeManager,
            EmiRecipeCategory emiRecipeCategory,
            Operation<List<EmiIngredient>> original) {
        return emi_discovery$filterWorkstations(recipeManager, emiRecipeCategory, original);
    }
}
