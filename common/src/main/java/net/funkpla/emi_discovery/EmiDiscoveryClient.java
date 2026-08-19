package net.funkpla.emi_discovery;

import net.funkpla.emi_discovery.advancement.AdvancementCache;
import net.funkpla.emi_discovery.advancement.AdvancementDiscoveryManager;

public class EmiDiscoveryClient {
  public static void joinWorld() {
    AdvancementDiscoveryManager.load();
    KnownItems.loadFromDisk();
  }

  public static void leaveWorld() {
    KnownItems.clear();
    AdvancementCache.clear();
    AdvancementDiscoveryManager.reset();
  }
}
