package net.funkpla.emi_discovery.mixin;

import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LavaCauldronBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void emi_discovery$onUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue() != null && cir.getReturnValue().consumesAction()) {
            if (player == null || result == null) return;
            Level level = player.level();
            BlockPos pos = result.getBlockPos();
            FluidState fluidState = level.getFluidState(pos);
            if (!fluidState.isEmpty()) {
                Fluid fluid = fluidState.getType();
                if (fluid instanceof FlowingFluid flowing) fluid = flowing.getSource();
                if (fluid != Fluids.EMPTY) {
                    KnownItems.addKnown(fluid);
                }
            }
            BlockState blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof LayeredCauldronBlock) {
                KnownItems.addKnown(Fluids.WATER);
            } else if (blockState.getBlock() instanceof LavaCauldronBlock) {
                KnownItems.addKnown(Fluids.LAVA);
            }
        }
    }
}
