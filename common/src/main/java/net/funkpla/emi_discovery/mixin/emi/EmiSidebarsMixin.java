package net.funkpla.emi_discovery.mixin.emi;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiStackList;
import dev.emi.emi.runtime.EmiSidebars;
import java.util.List;

import net.funkpla.emi_discovery.CommonClass;
import net.funkpla.emi_discovery.KnownItems;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EmiSidebars.class)
public class EmiSidebarsMixin {
  /**
   * Slide in to getStacks to filter out unknown items from the index.
   *
   * @param operation original operation (unused)
   * @return a filtered list of stacks
   */
  @WrapOperation(
      remap = false,
      method = "getStacks",
      at =
          @At(
              value = "FIELD",
              opcode = Opcodes.GETSTATIC,
              target = "Ldev/emi/emi/registry/EmiStackList;filteredStacks:Ljava/util/List;"))
  private static List<EmiStack> filterFiltered(Operation<List<EmiStack>> operation) {
    if (CommonClass.isDisabled()) return operation.call();
    return EmiStackList.filteredStacks.stream().filter(KnownItems::shouldStackDisplay).toList();
  }
}
