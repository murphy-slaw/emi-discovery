package net.funkpla.emi_discovery.mixin;

import net.funkpla.emi_discovery.ServerDiscoveryTracker;
import net.funkpla.emi_discovery.network.client.S2CItemStackPacket;
import net.funkpla.emi_discovery.platform.Services;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryChangeTrigger.class)
public class InventoryChangeTriggerMixin {

    @Inject(
            method = "trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "HEAD"))
    private void sendPacket(ServerPlayer pPlayer, Inventory pInventory, ItemStack pStack, CallbackInfo ci) {
        if (pStack != null && !pStack.isEmpty() && ServerDiscoveryTracker.shouldSendAndTrack(pPlayer, pStack.getItem())) {
            Services.PLATFORM.sendToClient(new S2CItemStackPacket(pStack), pPlayer);
        }
    }
}

