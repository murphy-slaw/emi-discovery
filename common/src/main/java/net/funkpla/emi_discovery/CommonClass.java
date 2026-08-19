package net.funkpla.emi_discovery;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.funkpla.emi_discovery.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;

public class CommonClass {
  private static ConfigHolder<EmiDiscoveryConfig> configHolder = null;

  public static void init() {
    Constants.LOG.info("EmiDiscovery is certainly not breaking things.");
    AutoConfig.register(EmiDiscoveryConfig.class, Toml4jConfigSerializer::new);
    PacketHandler.registerPackets();
    configHolder = AutoConfig.getConfigHolder(EmiDiscoveryConfig.class);
    configHolder.registerSaveListener((holder, config) -> {
      KnownItems.invalidateCache();
      return InteractionResult.PASS;
    });
  }

  public static ConfigHolder<EmiDiscoveryConfig> getConfigHolder() {
    return configHolder;
  }

    public static ResourceLocation locate(String path) {
    return new ResourceLocation(Constants.MOD_ID, path);
  }
}
