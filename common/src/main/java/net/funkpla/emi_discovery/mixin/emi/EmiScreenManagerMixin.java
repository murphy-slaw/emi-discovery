package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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
        if (KnownItems.allowUsageLookupForUndiscovered() || (KnownItems.shouldBlackoutIndex() && KnownItems.isKnown(fav)) || KnownItems.shouldIngredientDisplay(fav)) {
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
        if (KnownItems.allowUsageLookupForUndiscovered() || (KnownItems.shouldBlackoutIndex() && KnownItems.isKnown(fav)) || KnownItems.shouldIngredientDisplay(fav)) {
            original.call(fav);
        }
    }

    @WrapOperation(remap = false, method = "renderCurrentTooltip", at = @At(value = "INVOKE", target =
            "Ldev/emi/emi" + "/api/stack/EmiIngredient;getTooltip()Ljava/util/List;"))
    private static List<ClientTooltipComponent> obscureTooltip(EmiIngredient instance,
                                                               Operation<List<ClientTooltipComponent>> original) {
        if (KnownItems.shouldBlackoutIndex() && !KnownItems.shouldIngredientDisplay(instance) && KnownItems.shouldObscureTooltips()) {
            List<ClientTooltipComponent> list = new ArrayList<>();
            list.add(ClientTooltipComponent.create(Component.translatable("tooltip.emi_discovery.obscured").getVisualOrderText()));
            return list;
        } else if (!KnownItems.shouldBlackoutIndex() && !KnownItems.shouldIngredientDisplay(instance)) {
            return List.of();
        }
        return original.call(instance);
    }

}
