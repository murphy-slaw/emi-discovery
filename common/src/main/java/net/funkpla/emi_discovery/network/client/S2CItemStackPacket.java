package net.funkpla.emi_discovery.network.client;

import dev.emi.emi.screen.EmiScreenManager;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record S2CItemStackPacket(ItemStack stack) implements S2CModPacket {

    public S2CItemStackPacket(FriendlyByteBuf buf) {
        this(read(buf));
    }

    private static ItemStack read(FriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            return ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(buf.readResourceLocation()));
    }

    @Override
    public void handleClient() {
        KnownItems.addKnown(stack);
        //Fake a search update to update the index view.
        EmiScreenManager.search.update();
    }

    @Override
    public void write(FriendlyByteBuf to) {
        if (to instanceof RegistryFriendlyByteBuf registryBuf) {
            to.writeBoolean(true);
            ItemStack.STREAM_CODEC.encode(registryBuf, stack);
        } else {
            to.writeBoolean(false);
            to.writeResourceLocation(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }
}
