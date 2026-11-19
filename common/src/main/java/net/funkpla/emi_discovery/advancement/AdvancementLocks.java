package net.funkpla.emi_discovery.advancement;

import com.mojang.datafixers.util.Pair;
import net.funkpla.emi_discovery.Constants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Items in an {@code emi_discovery:requires_advancement/<namespace>/<path>} item tag stay hidden from the index and
 * recipe screens until the player has earned the advancement {@code <namespace>:<path>}, unless already discovered.
 */
public final class AdvancementLocks {
    public static final String TAG_PREFIX = "requires_advancement/";

    private static volatile Map<Item, Set<ResourceLocation>> locks;
    private static volatile Set<ResourceLocation> lockAdvancements = Set.of();

    public static void invalidate() {
        locks = null;
    }

    private static Map<Item, Set<ResourceLocation>> locks() {
        Map<Item, Set<ResourceLocation>> current = locks;
        if (current != null) return current;
        Map<Item, Set<ResourceLocation>> built = new HashMap<>();
        Set<ResourceLocation> advancements = new HashSet<>();
        try {
            BuiltInRegistries.ITEM.getTags().forEach((Pair<TagKey<Item>, HolderSet.Named<Item>> pair) -> {
                ResourceLocation advancement = advancementFor(pair.getFirst().location());
                if (advancement == null) return;
                advancements.add(advancement);
                for (Holder<Item> holder : pair.getSecond()) {
                    built.computeIfAbsent(holder.value(), item -> new HashSet<>()).add(advancement);
                }
            });
        } catch (Exception e) {
            Constants.LOG.error("EMI Discovery: Failed to read advancement lock tags", e);
        }
        lockAdvancements = Collections.unmodifiableSet(advancements);
        locks = built;
        return built;
    }

    private static ResourceLocation advancementFor(ResourceLocation tagId) {
        if (!tagId.getNamespace().equals(Constants.MOD_ID) || !tagId.getPath().startsWith(TAG_PREFIX)) return null;
        String rest = tagId.getPath().substring(TAG_PREFIX.length());
        int slash = rest.indexOf('/');
        if (slash <= 0 || slash == rest.length() - 1) return null;
        return ResourceLocation.tryBuild(rest.substring(0, slash), rest.substring(slash + 1));
    }

    public static boolean hasLocks() {
        return !locks().isEmpty();
    }

    public static boolean isLockAdvancement(ResourceLocation id) {
        locks();
        return lockAdvancements.contains(id);
    }

    public static boolean isLocked(Item item) {
        Set<ResourceLocation> required = locks().get(item);
        if (required == null) return false;
        for (ResourceLocation id : required) {
            if (!AdvancementCache.isDone(id)) return true;
        }
        return false;
    }
}
