package net.funkpla.emi_discovery.advancement;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AdvancementCache {
    private static final Set<ResourceLocation> COMPLETED = ConcurrentHashMap.newKeySet();

    public static void markCompleted(ResourceLocation id) {
        COMPLETED.add(id);
    }

    public static void markNotCompleted(ResourceLocation id) {
        COMPLETED.remove(id);
    }

    public static void clear() {
        COMPLETED.clear();
    }

    public static boolean isDone(ResourceLocation id) {
        return COMPLETED.contains(id);
    }
}
