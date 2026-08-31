package net.funkpla.emi_discovery;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.funkpla.emi_discovery.command.DiscoveryCommand;

public class EmiDiscoveryFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CommonClass.init();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DiscoveryCommand.register(dispatcher);
        });
    }
}
