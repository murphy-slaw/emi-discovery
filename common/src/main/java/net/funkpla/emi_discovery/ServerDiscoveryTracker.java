package net.funkpla.emi_discovery;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public class ServerDiscoveryTracker {
    private static final WeakHashMap<ServerPlayer, Set<DiscoveryStacks.Key>> CACHE = new WeakHashMap<>();

    public static synchronized boolean shouldSendAndTrack(ServerPlayer player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return false;
        Set<DiscoveryStacks.Key> playerItems = CACHE.computeIfAbsent(player, p -> new HashSet<>());
        return playerItems.add(DiscoveryStacks.Key.of(stack));
    }

    public static synchronized void clearCache(ServerPlayer player) {
        if (player != null) {
            CACHE.remove(player);
        }
    }

    public static synchronized void removeFromCache(ServerPlayer player, Collection<Item> items) {
        if (player != null && items != null) {
            Set<DiscoveryStacks.Key> playerItems = CACHE.get(player);
            if (playerItems != null) {
                playerItems.removeIf(key -> items.contains(key.item()));
            }
        }
    }
}
