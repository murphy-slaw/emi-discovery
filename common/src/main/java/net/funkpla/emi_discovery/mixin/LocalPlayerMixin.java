package net.funkpla.emi_discovery.mixin;

import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void emi_discovery$onTickCheckFluids(CallbackInfo ci) {
        if (!KnownItems.isFluidDiscoveryEnabled()) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        Level level = player.level();
        if (level == null) return;

        AABB aabb = player.getBoundingBox();
        int minX = Mth.floor(aabb.minX);
        int maxX = Mth.ceil(aabb.maxX);
        int minY = Mth.floor(aabb.minY);
        int maxY = Mth.ceil(aabb.maxY);
        int minZ = Mth.floor(aabb.minZ);
        int maxZ = Mth.ceil(aabb.maxZ);

        // I know this is a scary triple nested for loop, but this should only be like 12ish blocks
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x < maxX; x++) {
            for (int y = minY; y < maxY; y++) {
                for (int z = minZ; z < maxZ; z++) {
                    pos.set(x, y, z);
                    FluidState fluidState = level.getFluidState(pos);
                    if (!fluidState.isEmpty()) {
                        Fluid fluid = fluidState.getType();
                        if (fluid instanceof FlowingFluid flowing) {
                            fluid = flowing.getSource();
                        }
                        if (fluid != null && fluid != Fluids.EMPTY) {
                            KnownItems.addKnown(fluid);
                        }
                    }
                }
            }
        }

        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        FluidState eyeFluid = level.getFluidState(eyePos);
        if (!eyeFluid.isEmpty()) {
            Fluid fluid = eyeFluid.getType();
            if (fluid instanceof FlowingFluid flowing) {
                fluid = flowing.getSource();
            }
            if (fluid != null && fluid != Fluids.EMPTY) {
                KnownItems.addKnown(fluid);
            }
        }
    }
}
