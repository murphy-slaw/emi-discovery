package net.funkpla.emi_discovery.mixin;

import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

    @Inject(method = "handleUpdateMobEffect", at = @At("TAIL"))
    private void onUpdateMobEffect(ClientboundUpdateMobEffectPacket packet, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null && packet.getEntityId() == client.player.getId()) {
            KnownItems.addKnown(packet.getEffect());

        }
    }
}
