package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiSidebars;
import net.funkpla.emi_discovery.KnownItems;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(EmiSidebars.class)
public class EmiSidebarsMixin {

    @Unique
    private static List<EmiStack> emi_discovery$cachedFilteredList;
    @Unique
    private static int emi_discovery$lastUpdateCount = -1;
    @Unique
    private static List<EmiStack> emi_discovery$lastFilteredStacks;

    /**
     * Slide in to getStacks to filter out unknown items from the index.
     *
     * @param operation original operation (unused)
     * @return a filtered list of stacks
     */
    @WrapOperation(
            remap = false,
            method = "getStacks",
            at =
            @At(
                    value = "FIELD",
                    opcode = Opcodes.GETSTATIC,
                    target = "Ldev/emi/emi/registry/EmiStackList;filteredStacks:Ljava/util/List;"))
    private static List<EmiStack> filterFiltered(Operation<List<EmiStack>> operation) {
        if (!KnownItems.isModEnabled() || !KnownItems.shouldFilterIndex()) {
            return operation.call();
        }
        List<EmiStack> src = EmiStackList.filteredStacks;
        int currentUpdate = KnownItems.getUpdateCount();
        if (emi_discovery$cachedFilteredList != null
                && currentUpdate == emi_discovery$lastUpdateCount
                && emi_discovery$lastFilteredStacks == src) {
            return emi_discovery$cachedFilteredList;
        }
        List<EmiStack> filtered = new ArrayList<>(src.size());
        for (EmiStack stack : src) {
            if (KnownItems.shouldStackDisplay(stack)) {
                filtered.add(stack);
            }
        }
        emi_discovery$lastUpdateCount = currentUpdate;
        emi_discovery$lastFilteredStacks = src;
        emi_discovery$cachedFilteredList = filtered;
        return filtered;
    }
}
