package net.funkpla.emi_discovery;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

public class ServerDiscoveryTracker {
    private static final WeakHashMap<ServerPlayer, Set<Item>> CACHE = new WeakHashMap<>();

    public static synchronized boolean shouldSendAndTrack(ServerPlayer player, Item item) {
        if (player == null || item == null) return false;
        Set<Item> playerItems = CACHE.computeIfAbsent(player, p -> new HashSet<>());
        return playerItems.add(item);
    }

    public static synchronized void clearCache(ServerPlayer player) {
        if (player != null) {
            CACHE.remove(player);
        }
    }

    public static synchronized void removeFromCache(ServerPlayer player, Collection<Item> items) {
        if (player != null && items != null) {
            Set<Item> playerItems = CACHE.get(player);
            if (playerItems != null) {
                playerItems.removeAll(items);
            }
        }
    }
}
