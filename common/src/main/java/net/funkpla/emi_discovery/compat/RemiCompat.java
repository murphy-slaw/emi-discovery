package net.funkpla.emi_discovery.compat;

import com.evandev.remi.feature.stackgroup.EmiGroupStack;
import com.evandev.remi.feature.stackgroup.GroupedEmiStack;
import com.evandev.remi.integration.emi.StackManager;
import dev.emi.emi.api.stack.EmiStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Only call into this class after checking that REMI is loaded, or it'll cause a crash
 */
public final class RemiCompat {
    /**
     * Rebuild REMI's group stacks and displayed-stack list after the known set changes
     */
    public static void onFilterChanged() {
        try {
            EmiGroupStack.onStackFilterChanged();
        } catch (Throwable ignored) {
        }
        try {
            if (StackManager.sourceStacks != null && !StackManager.sourceStacks.isEmpty()) {
                StackManager.buildStacks(StackManager.sourceStacks);
                StackManager.repopulateIndexPanelsIfDirty();
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * If the stack is an EmiGroupStack, returns the real stacks of its members. Otherwise returns null
     */
    public static List<EmiStack> getGroupContents(EmiStack stack) {
        if (!(stack instanceof EmiGroupStack groupStack)) return null;
        var items = groupStack.getItems();
        List<EmiStack> result = new ArrayList<>(items.size());
        for (GroupedEmiStack<EmiStack> item : items) {
            result.add(item.realStack);
        }
        return result;
    }

    /**
     * If the stack is a GroupedEmiStack, returns the real stack it wraps. Otherwise returns the stack unchanged
     */
    public static EmiStack unwrap(EmiStack stack) {
        if (stack instanceof GroupedEmiStack<?> groupedStack) return groupedStack.realStack;
        return stack;
    }
}
