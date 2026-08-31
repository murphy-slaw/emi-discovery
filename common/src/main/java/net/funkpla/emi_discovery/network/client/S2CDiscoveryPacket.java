package net.funkpla.emi_discovery.network.client;

import dev.emi.emi.screen.EmiScreenManager;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record S2CDiscoveryPacket(Action action, boolean everything,
                                 List<ResourceLocation> ids) implements S2CModPacket {

    public S2CDiscoveryPacket(FriendlyByteBuf buf) {
        this(
                buf.readEnum(Action.class),
                buf.readBoolean(),
                buf.readList(FriendlyByteBuf::readResourceLocation)
        );
    }

    @Override
    public void handleClient() {
        if (action == Action.GRANT) {
            if (everything) {
                KnownItems.addEverything();
            } else {
                KnownItems.addByIds(ids);
            }
        } else if (action == Action.REVOKE) {
            if (everything) {
                KnownItems.clearAndSave();
            } else {
                KnownItems.removeByIds(ids);
            }
        }
        try {
            if (EmiScreenManager.search != null) {
                EmiScreenManager.search.update();
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void write(FriendlyByteBuf to) {
        to.writeEnum(action);
        to.writeBoolean(everything);
        to.writeCollection(ids, FriendlyByteBuf::writeResourceLocation);
    }

    public enum Action {
        GRANT,
        REVOKE
    }
}
