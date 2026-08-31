package net.funkpla.emi_discovery.network;

import net.funkpla.emi_discovery.CommonClass;
import net.funkpla.emi_discovery.network.client.S2CDiscoveryPacket;
import net.funkpla.emi_discovery.network.client.S2CItemStackPacket;
import net.funkpla.emi_discovery.platform.Services;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public class PacketHandler {

    public static void registerPackets() {
        Services.PLATFORM.registerClientPacket(S2CItemStackPacket.class, S2CItemStackPacket::new);
        Services.PLATFORM.registerClientPacket(S2CDiscoveryPacket.class, S2CDiscoveryPacket::new);
    }

    public static ResourceLocation packet(Class<?> clazz) {
        return CommonClass.locate(clazz.getName().toLowerCase(Locale.ROOT));
    }

}
