package net.funkpla.emi_discovery.mixin.remi;

import com.evandev.remi.feature.creativemodetab.CreativeModeTabManager;
import com.evandev.remi.feature.creativemodetab.gui.CreativeModeTabGui;
import com.evandev.remi.feature.creativemodetab.gui.itemtab.ItemTab;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.emi.emi.config.EmiConfig;
import net.funkpla.emi_discovery.KnownItems;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = CreativeModeTabManager.class, remap = false)
public abstract class CreativeModeTabManagerMixin {

    @Shadow
    private static CreativeModeTab currentTab;

    @Shadow
    private static CreativeModeTab indexCreativeModeTab;

    @Final
    @Shadow
    private static List<CreativeModeTab> creativeModeTabs;

    @Shadow
    private static List<ItemTab> updateTabs() {
        throw new AssertionError();
    }

    @Shadow
    public static void onTabSelected(ItemTab tab) {
        throw new AssertionError();
    }

    @ModifyReturnValue(method = "shouldHideTab", at = @At("RETURN"))
    private static boolean filterUndiscoveredTabs(boolean original, CreativeModeTab tab) {
        if (original) {
            return true;
        }
        if (!KnownItems.isModEnabled() || !KnownItems.shouldHideEmptyRemiTabs() || EmiConfig.editMode) {
            return false;
        }
        return !KnownItems.hasDiscoveredItems(tab);
    }

    @Inject(method = "reload", at = @At("TAIL"))
    private static void onReload(CallbackInfo ci) {
        if (!KnownItems.isModEnabled() || !KnownItems.shouldHideEmptyRemiTabs()) {
            return;
        }
        if (currentTab != null && !creativeModeTabs.contains(currentTab)) {
            currentTab = null;
            if (indexCreativeModeTab != null && creativeModeTabs.contains(indexCreativeModeTab)) {
                CreativeModeTabGui.selectTab(0, false);
                var bar = CreativeModeTabGui.currentTheme() == CreativeModeTabGui.TabTheme.HORIZONTAL
                        ? CreativeModeTabGui.topTabNavigationBar : CreativeModeTabGui.leftTabNavigationBar;
                if (!bar.visibleTabs.isEmpty()) {
                    onTabSelected(bar.visibleTabs.getFirst());
                }
            }
        }
        if (CreativeModeTabGui.tabCount > 0) {
            CreativeModeTabGui.onLayout();
            updateTabs();
        }
    }
}
