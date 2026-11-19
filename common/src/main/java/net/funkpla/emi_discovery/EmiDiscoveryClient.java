package net.funkpla.emi_discovery;

import net.funkpla.emi_discovery.advancement.AdvancementCache;
import net.funkpla.emi_discovery.advancement.AdvancementDiscoveryManager;
import net.funkpla.emi_discovery.advancement.AdvancementLocks;

public class EmiDiscoveryClient {
  public static void joinWorld() {
    AdvancementDiscoveryManager.load();
    AdvancementLocks.invalidate();
    KnownItems.loadFromDisk();
  }

  public static void leaveWorld() {
    KnownItems.clear();
    AdvancementCache.clear();
    AdvancementDiscoveryManager.reset();
    AdvancementLocks.invalidate();
  }
}
