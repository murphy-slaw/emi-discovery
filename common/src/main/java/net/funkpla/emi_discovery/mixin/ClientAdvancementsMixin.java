package net.funkpla.emi_discovery.mixin;

import net.funkpla.emi_discovery.advancement.AdvancementCache;
import net.funkpla.emi_discovery.advancement.AdvancementDiscoveryManager;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is also largely reused from Reliable Remover
 **/
@Mixin(ClientAdvancements.class)
public class ClientAdvancementsMixin {

    @Inject(method = "update", at = @At("TAIL"))
    private void emi_discovery$onAdvancementsUpdate(ClientboundUpdateAdvancementsPacket packet, CallbackInfo ci) {
        if (packet.shouldReset()) {
            AdvancementCache.clear();
            for (Map.Entry<ResourceLocation, AdvancementProgress> entry : packet.getProgress().entrySet()) {
                if (entry.getValue().isDone()) {
                    AdvancementCache.markCompleted(entry.getKey());
                }
            }
            AdvancementDiscoveryManager.onProgressChanged(true, Set.of());
            return;
        }

        Set<ResourceLocation> changed = new HashSet<>();

        for (ResourceLocation id : packet.getRemoved()) {
            if (AdvancementCache.isDone(id)) {
                AdvancementCache.markNotCompleted(id);
                changed.add(id);
            }
        }

        for (Map.Entry<ResourceLocation, AdvancementProgress> entry : packet.getProgress().entrySet()) {
            ResourceLocation id = entry.getKey();
            boolean wasDone = AdvancementCache.isDone(id);
            boolean isDone = entry.getValue().isDone();

            if (isDone && !wasDone) {
                AdvancementCache.markCompleted(id);
                changed.add(id);
            } else if (!isDone && wasDone) {
                AdvancementCache.markNotCompleted(id);
                changed.add(id);
            }
        }

        if (!changed.isEmpty()) {
            AdvancementDiscoveryManager.onProgressChanged(false, changed);
        }
    }
}
