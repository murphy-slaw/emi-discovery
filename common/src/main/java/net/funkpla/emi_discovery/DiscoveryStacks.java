package net.funkpla.emi_discovery;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Helpers for turning an observed stack into the form that gets discovered.
 */
public final class DiscoveryStacks {
    /**
     * Top-level tags that change during normal use and never identify a different variant.
     */
    private static final Set<String> VOLATILE_TAGS = Set.of(
            "Damage",
            "RepairCost",
            "Items",
            "ChargedProjectiles",
            "Charged",
            "pages",
            "filtered_pages",
            "title",
            "filtered_title",
            "author",
            "generation",
            "resolved"
    );

    /**
     * Tags inside "display" that are cosmetic (color is kept, since it identifies dyed variants).
     */
    private static final Set<String> VOLATILE_DISPLAY_TAGS = Set.of("Name", "Lore");

    /**
     * Tags inside "BlockEntityTag" that hold contents rather than identity (e.g. shulker box items).
     */
    private static final Set<String> VOLATILE_BLOCK_ENTITY_TAGS = Set.of("Items", "LootTable", "LootTableSeed");

    private DiscoveryStacks() {
    }

    /**
     * Returns a single-count copy of the stack with volatile tags removed.
     */
    public static ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack normalized = new ItemStack(stack.getItem());
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag = tag.copy();
            VOLATILE_TAGS.forEach(tag::remove);
            stripNested(tag, "display", VOLATILE_DISPLAY_TAGS);
            stripNested(tag, "BlockEntityTag", VOLATILE_BLOCK_ENTITY_TAGS);
            if (!tag.isEmpty()) normalized.setTag(tag);
        }
        return normalized;
    }

    private static void stripNested(CompoundTag tag, String key, Set<String> volatileKeys) {
        if (!tag.contains(key, CompoundTag.TAG_COMPOUND)) return;
        CompoundTag nested = tag.getCompound(key);
        volatileKeys.forEach(nested::remove);
        if (nested.isEmpty()) tag.remove(key);
    }

    /**
     * Server-side identity of a discovery, used to avoid resending the same stack to a player.
     */
    public record Key(Item item, CompoundTag tag) {
        public static Key of(ItemStack stack) {
            ItemStack normalized = normalize(stack);
            return new Key(normalized.getItem(), normalized.getTag());
        }
    }
}
