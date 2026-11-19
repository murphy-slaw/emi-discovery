package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.screen.EmiScreenManager;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(EmiScreenManager.class)
public class EmiScreenManagerMixin {
    /**
     * Stop usage lookups for unknown ingredients.
     *
     * @param fav      the ingredient being requested
     * @param original original operation, called if the ingredient is known
     */
    @WrapOperation(
            remap = false,
            method =
                    "stackInteraction(Ldev/emi/emi/api/stack/EmiStackInteraction;Ljava/util/function/Function;)Z",
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Ldev/emi/emi/api/EmiApi;displayUses(Ldev/emi/emi/api/stack/EmiIngredient;)V"))
    private static void stopUseLookup(EmiIngredient fav, Operation<Void> original) {
        if (KnownItems.shouldIngredientDisplayInRecipes(fav) || KnownItems.allowUsageLookupForUndiscovered()) {
            original.call(fav);
        }
    }

    /**
     * Stop recipe lookups for unknown ingredients.
     *
     * @param fav      the ingredient being requested
     * @param original original operation, called if the ingredient is known
     */
    @WrapOperation(
            remap = false,
            method =
                    "stackInteraction(Ldev/emi/emi/api/stack/EmiStackInteraction;Ljava/util/function/Function;)Z",
            at =
            @At(
                    value = "INVOKE",
                    target =
                            "Ldev/emi/emi/api/EmiApi;displayRecipes(Ldev/emi/emi/api/stack/EmiIngredient;)V"))
    private static void stopRecipeLookup(EmiIngredient fav, Operation<Void> original) {
        if (KnownItems.shouldIngredientDisplayInRecipes(fav) || KnownItems.allowRecipeLookupForUndiscovered()) {
            original.call(fav);
        }
    }

    /**
     * Obscure the hover tooltip of blacked-out stacks in the sidebars.
     */
    @WrapOperation(
            remap = false,
            method = "renderCurrentTooltip",
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/api/stack/EmiIngredient;getTooltip()Ljava/util/List;"))
    private static List<ClientTooltipComponent> obscureSidebarTooltip(
            EmiIngredient hovered, Operation<List<ClientTooltipComponent>> original) {
        if (KnownItems.shouldBlackoutIndex()
                && KnownItems.shouldObscureTooltips()
                && !KnownItems.shouldIngredientDisplay(hovered)) {
            List<ClientTooltipComponent> list = new ArrayList<>();
            list.add(ClientTooltipComponent.create(
                    Component.translatable("tooltip.emi_discovery.obscured").getVisualOrderText()));
            return list;
        }
        return original.call(hovered);
    }

    /**
     * Don't preview the recipe of an undiscovered stack when the "show craft" key is held over it, unless recipe
     * lookups for undiscovered stacks are allowed.
     */
    @WrapOperation(
            remap = false,
            method = "renderCurrentTooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/emi/emi/EmiUtil;getPreferredRecipe(Ldev/emi/emi/api/stack/EmiIngredient;Ldev/emi/emi/api/recipe/EmiPlayerInventory;Z)Ldev/emi/emi/api/recipe/EmiRecipe;"))
    private static EmiRecipe hideCraftPreview(
            EmiIngredient hovered, EmiPlayerInventory inventory, boolean requireCraftable,
            Operation<EmiRecipe> original) {
        if (!KnownItems.shouldIngredientDisplayInRecipes(hovered) && !KnownItems.allowRecipeLookupForUndiscovered()) {
            return null;
        }
        return original.call(hovered, inventory, requireCraftable);
    }
}
