package net.funkpla.emi_discovery;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Helpers for turning an observed stack into the form that gets discovered.
 */
public final class DiscoveryStacks {
    /**
     * Components that change during normal use and never identify a different variant.
     */
    private static final Set<DataComponentType<?>> VOLATILE_COMPONENTS = Set.of(
            DataComponents.DAMAGE,
            DataComponents.REPAIR_COST,
            DataComponents.CUSTOM_NAME,
            DataComponents.LORE,
            DataComponents.CONTAINER,
            DataComponents.BUNDLE_CONTENTS,
            DataComponents.CHARGED_PROJECTILES,
            DataComponents.WRITABLE_BOOK_CONTENT,
            DataComponents.WRITTEN_BOOK_CONTENT
    );

    private DiscoveryStacks() {
    }

    /**
     * Returns a single-count copy of the stack with volatile components reset to the item's defaults.
     */
    public static ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        DataComponentPatch patch = stack.getComponentsPatch().forget(VOLATILE_COMPONENTS::contains);
        return new ItemStack(stack.getItemHolder(), 1, patch);
    }

    /**
     * Server-side identity of a discovery, used to avoid resending the same stack to a player.
     */
    public record Key(Item item, DataComponentPatch components) {
        public static Key of(ItemStack stack) {
            ItemStack normalized = normalize(stack);
            return new Key(normalized.getItem(), normalized.getComponentsPatch());
        }
    }
}
