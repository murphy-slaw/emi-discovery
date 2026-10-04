package net.funkpla.emi_discovery.mixin.emi;

import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.client.Minecraft;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "dev.emi.emi.runtime.EmiReloadManager$ReloadWorker", remap = false)
public class EmiReloadManagerMixin {

    @Inject(
            method = "run",
            at = @At(value = "FIELD", target = "Ldev/emi/emi/registry/EmiComparisonDefaults;comparisons:Ljava/util/Map;", opcode = Opcodes.PUTSTATIC)
    )
    private void beforeComparisonsReset(CallbackInfo ci) {
        KnownItems.onComparisonsInvalidated();
    }

    @Inject(
            method = "run",
            at = @At(value = "INVOKE", target = "Ldev/emi/emi/registry/EmiRecipes;bake()V", shift = At.Shift.AFTER)
    )
    private void afterRecipesBake(CallbackInfo ci) {
        KnownItems.invalidateCache();
        Minecraft.getInstance().execute(KnownItems::onComparisonsReady);
    }
}
