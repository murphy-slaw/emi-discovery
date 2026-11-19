package net.funkpla.emi_discovery.compat;

import com.evandev.remi.feature.creativemodetab.CreativeModeTabManager;
import com.evandev.remi.feature.stackgroup.EmiGroupStack;
import com.evandev.remi.feature.stackgroup.GroupedEmiStack;
import com.evandev.remi.integration.emi.StackManager;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.EmiScreenManager;
import net.funkpla.emi_discovery.Constants;

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
            StackManager.invalidateStacks();
            List<EmiStack> source = StackManager.sourceStacks;
            if (source != null && !source.isEmpty()) {
                StackManager.updateSourceStacks(source);
            }
            StackManager.repopulateIndexPanelsIfDirty();
        } catch (Throwable e) {
            Constants.LOG.error("EMI Discovery: Failed to refresh Reliable EMI's index", e);
        }
    }

    /**
     * Rebuild REMI's creative tab list after tab visibility changes
     */
    public static void reloadTabs() {
        try {
            CreativeModeTabManager.reload();
            EmiScreenManager.recalculate();
        } catch (Throwable e) {
            Constants.LOG.error("EMI Discovery: Failed to reload Reliable EMI's tabs", e);
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
