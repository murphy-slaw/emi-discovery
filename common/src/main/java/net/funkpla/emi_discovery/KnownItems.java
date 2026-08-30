package net.funkpla.emi_discovery;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import com.evandev.remi.feature.stackgroup.EmiGroupStack;
import com.evandev.remi.feature.stackgroup.GroupedEmiStack;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiRecipes;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.emi.emi.screen.EmiScreenManager;
import net.funkpla.emi_discovery.mixin.BucketItemAccessor;
import net.funkpla.emi_discovery.mixin.MinecraftServerStorageSourceAccessor;
import net.funkpla.emi_discovery.platform.Services;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

public class KnownItems {
  private static final Set<Item> knownItems = new HashSet<>();
  private static final Set<Fluid> knownFluids = new HashSet<>();
  private static final Set<MobEffect> knownEffects = new HashSet<>();
  private static final File PRE_DISCOVERED =
      new File("config", "emi_discovery_pre_discovered.json");
  private static final Path DATA_PATH =
      Services.PLATFORM.getGameDir().resolve(Path.of("moddata", "emi_discovery"));

  private static final AtomicInteger UPDATE_COUNT = new AtomicInteger();

  public static int getUpdateCount(){
      return UPDATE_COUNT.get();
  }

  private static final Gson gson = new Gson();

  /** Cache expensive visibility calculations, invalidate on add. */
  private static final LoadingCache<EmiStack, Boolean> stackDisplayCache =
      CacheBuilder.newBuilder()
          .maximumSize(5000)
          .build(
              new CacheLoader<>() {
                @Override
                public @NotNull Boolean load(@NotNull EmiStack stack) {
                  return shouldStackDisplayUncached(stack);
                }
              });

  public static void invalidateCache() {
    stackDisplayCache.invalidateAll();
    UPDATE_COUNT.getAndIncrement();
    try {
      EmiGroupStack.onStackFilterChanged();
    } catch (Throwable ignored) {
    }
    try {
      if (EmiScreenManager.search != null) {
        EmiScreenManager.search.update();
      }
    } catch (Throwable ignored) {
    }
  }

  public static void addKnown(ItemStack stack) {
    if (stack != null && !stack.isEmpty()) {
      Item item = stack.getItem();
      boolean itemAdded = knownItems.add(item);
      boolean fluidAdded = false;

      if (item instanceof BucketItem bucketItem) {
        try {
          Fluid fluid = ((BucketItemAccessor) bucketItem).emi_discovery$getContent();
          if (fluid != null && fluid != Fluids.EMPTY && knownFluids.add(fluid)) {
            fluidAdded = true;
          }
        } catch (Throwable ignored) {
        }
      }
      for (Fluid fluid : BuiltInRegistries.FLUID) {
        if (fluid != null && fluid != Fluids.EMPTY && fluid.getBucket() == item) {
          if (knownFluids.add(fluid)) {
            fluidAdded = true;
          }
        }
      }

      if (itemAdded || fluidAdded) {
        invalidateCache();
        saveToDisk();
      }
    }
  }

  public static void addKnown(Fluid fluid) {
    if (fluid != null && fluid != Fluids.EMPTY && knownFluids.add(fluid)) {
      invalidateCache();
      saveToDisk();
    }
  }

  public static void addKnown(MobEffect effect) {
    if (effect != null && knownEffects.add(effect)) {
      invalidateCache();
      saveToDisk();
    }
  }

  public static void addKnown(Holder<MobEffect> effectHolder) {
    if (effectHolder != null) {
        addKnown(effectHolder.value());
    }
  }

  public static boolean addKnownItems(Collection<Item> items) {
    boolean anyAdded = false;
    for (Item item : items) {
      if (item != null && knownItems.add(item)) {
        anyAdded = true;
      }
    }
    if (anyAdded) {
      invalidateCache();
      saveToDisk();
    }
    return anyAdded;
  }

  public static boolean addKnownFluids(Collection<Fluid> fluids) {
    boolean anyAdded = false;
    for (Fluid fluid : fluids) {
      if (fluid != null && fluid != Fluids.EMPTY && knownFluids.add(fluid)) {
        anyAdded = true;
      }
    }
    if (anyAdded) {
      invalidateCache();
      saveToDisk();
    }
    return anyAdded;
  }

  public static boolean addKnownEffects(Collection<MobEffect> effects) {
    boolean anyAdded = false;
    for (MobEffect effect : effects) {
      if (effect != null && knownEffects.add(effect)) {
        anyAdded = true;
      }
    }
    if (anyAdded) {
      invalidateCache();
      saveToDisk();
    }
    return anyAdded;
  }

  public static void clear() {
    knownItems.clear();
    knownFluids.clear();
    knownEffects.clear();
    invalidateCache();
  }

  /**
   * Does the item represented by the given stack exist in the known set? Also returns true for
   * empty stacks so empty slots in the recipe don't count.
   *
   * @param stack The stack to test.
   */
  public static boolean isKnown(ItemStack stack) {
    if (!isModEnabled()) return true;
    return stack == null || stack.isEmpty() || knownItems.contains(stack.getItem());
  }

  /** Does the fluid exist in the known set or has its bucket item been discovered? */
  public static boolean isKnown(Fluid fluid) {
    if (!isModEnabled() || !isFluidDiscoveryEnabled()) return true;
    if (fluid == null || fluid == Fluids.EMPTY) return true;
    if (knownFluids.contains(fluid)) return true;
    Item bucket = fluid.getBucket();
    return bucket != Items.AIR && knownItems.contains(bucket);
  }

  /** Does the mob effect exist in the known set? */
  public static boolean isKnown(MobEffect effect) {
    if (!isModEnabled() || !isEffectDiscoveryEnabled()) return true;
    if (effect == null) return true;
    return knownEffects.contains(effect);
  }

  /** Convenience method to unwrap EmiStacks for the above. */
  public static boolean isKnown(EmiStack stack) {
    if (stack == null) return true;
    if (stack instanceof EmiGroupStack groupStack) return isKnown(groupStack);
    if (stack instanceof GroupedEmiStack<?> groupedStack) return isKnown(groupedStack.realStack);
    if (stack.isEmpty()) return true;
    if (stack.getKey() instanceof Fluid fluid) return isKnown(fluid);
    if (stack.getKey() instanceof MobEffect effect) return isKnown(effect);
    if (stack.getKey() instanceof Holder<?> holder && holder.value() instanceof MobEffect effect) return isKnown(effect);
    if (stack.getId() != null) {
      if (BuiltInRegistries.FLUID.containsKey(stack.getId())) {
        return isKnown(BuiltInRegistries.FLUID.get(stack.getId()));
      }
      if (BuiltInRegistries.MOB_EFFECT.containsKey(stack.getId())) {
        return isKnown(BuiltInRegistries.MOB_EFFECT.get(stack.getId()));
      }
    }
    return isKnown(stack.getItemStack());
  }

  /**
   * For ingredients, if any stack matches, we call it a match. This gives the expected behavior for
   * both single-item stacks and tag and list stacks.
   */
  public static boolean isKnown(EmiIngredient ingredient) {
    return ingredient.getEmiStacks().stream().anyMatch(KnownItems::isKnown);
  }

  /**
   * This is the part where I get confused and angry about EMI++'s API naming. EmiGroupStacks
   * *contain* GroupedEmiStacks, which are wrapped EmiStacks. Each EmiGroupStack also has an
   * EmiStackGroup which represents metadata about the group. Yes. this is confusing.
   *
   * <p>This method takes an EmiGroupStack calls items() (which returns a list of GroupedEmiStacks),
   * and returns true if any of the items associated with any of the GroupedEmiStacks are known.
   */
  public static boolean isKnown(EmiGroupStack groupStack) {
    if (groupStack == null) return false;
    var items = groupStack.getItems();
    if (items.isEmpty()) return false;
    return items.stream().anyMatch(KnownItems::isKnown);
  }

  /** Returns true if any of the EmiStacks in the EmiIngredient are known. */
  public static boolean areAnyKnown(EmiIngredient ingredient) {
    return ingredient.getEmiStacks().stream().anyMatch(KnownItems::isKnownOrCraftable);
  }

  /**
   * This ugly beast returns true if the provided emiStack is known or craftable. The criteria
   * differ slightly for different subclasses of EmiStack. Bleah.
   */
  public static boolean isKnownOrCraftable(EmiStack emiStack) {
    if (emiStack instanceof EmiGroupStack groupStack)
      return (isCraftable(groupStack) || isKnown(groupStack));
    if (emiStack instanceof GroupedEmiStack<?> groupedStack)
      return (isKnownOrCraftable(groupedStack));
    return (isCraftable(emiStack) || isKnown(emiStack));
  }

  public static EmiDiscoveryConfig getConfig() {
    if (CommonClass.getConfigHolder() != null && CommonClass.getConfigHolder().get() != null) {
      return CommonClass.getConfigHolder().get();
    }
    return new EmiDiscoveryConfig();
  }

  public static boolean isModEnabled() {
    return getConfig().enabled;
  }

  public static boolean isFluidDiscoveryEnabled() {
    return isModEnabled() && getConfig().enableFluidDiscovery;
  }

  public static boolean isEffectDiscoveryEnabled() {
    return isModEnabled() && getConfig().enableEffectDiscovery;
  }

  public static boolean shouldFilterIndex() {
    return isModEnabled() && getConfig().filterIndex;
  }

  public static boolean shouldDisplayCraftableInIndex() {
    return getConfig().displayCraftableInIndex;
  }

  public static boolean requireWorkstationForCraftable() {
    return getConfig().requireWorkstationForCraftable;
  }

  public static boolean displayWithUnknownWorkstation() {
    return getConfig().displayWithUnknownWorkstation;
  }

  public static boolean requireCatalystsKnown() {
    return getConfig().requireCatalystsKnown;
  }

  public static boolean allowRecipeLookupForUndiscovered() {
    return !isModEnabled() || getConfig().allowRecipeLookupForUndiscovered;
  }

  public static boolean allowUsageLookupForUndiscovered() {
    return !isModEnabled() || getConfig().allowUsageLookupForUndiscovered;
  }

  public static boolean shouldBlackoutRecipes() {
    return isModEnabled() && getConfig().blackoutUnknownInRecipes;
  }

  public static boolean shouldObscureTooltips() {
    return isModEnabled() && getConfig().obscureTooltips;
  }

  public static boolean shouldShowQuestionMarkOverlay() {
    return isModEnabled() && getConfig().showQuestionMarkOverlay;
  }

  public static boolean isAdvancementDiscoveryEnabled() {
    return isModEnabled() && getConfig().enableAdvancementDiscovery;
  }

  /**
   * For EmiStacks, we call the stack craftable if any recipe for the stack has a known (or empty
   * catalyst), has at least one known workstation (if required), and can be made entirely with known items.
   */
  public static boolean isCraftable(EmiStack emiStack) {
    if (emiStack instanceof EmiGroupStack groupStack) return isCraftable(groupStack);
    try {
      boolean reqWorkstation = requireWorkstationForCraftable();
      return EmiRecipes.manager.getRecipesByOutput(emiStack).stream()
          .filter(
              recipe ->
                  (recipe.getCatalysts().isEmpty() || catalystsKnown(recipe))
                      && (!reqWorkstation
                          || EmiApi.getRecipeManager().getWorkstations(recipe.getCategory()).isEmpty()
                          || EmiApi.getRecipeManager().getWorkstations(recipe.getCategory()).stream()
                              .anyMatch(KnownItems::isKnown)))
          .anyMatch(r -> r.getInputs().stream().allMatch(KnownItems::isKnown));
    } catch (NullPointerException e) {
      Constants.LOG.error("Unexpected NPE in getInputs():", e);
      return false;
    }
  }

  /** For EmiGroupStacks, we call the stack craftable if any of the real stacks are craftable. */
  public static boolean isCraftable(EmiGroupStack groupStack) {
    if (groupStack == null) return false;
    var items = groupStack.getItems();
    if (items.isEmpty()) return false;
    return items.stream()
        .anyMatch(
            groupedStack -> isCraftable(groupedStack.realStack));
  }

  /** For GroupedEmiStacks, we unwrap the real stack and check it. */
  public static boolean isKnownOrCraftable(GroupedEmiStack<?> groupedStack) {
    return isCraftable(groupedStack.realStack) || isKnown(groupedStack.realStack);
  }

  /**
   * Switch between displaying only known stacks and known or craftable stacks based on the config
   */
  public static boolean shouldStackDisplay(EmiStack emiStack) {
    try {
      return stackDisplayCache.get(emiStack);
    } catch (ExecutionException | NullPointerException e) {
      Constants.LOG.error("Unexpected error checking stack for display", e);
      return false;
    }
  }

  public static boolean shouldStackDisplayUncached(EmiStack emiStack) {
    if (!shouldFilterIndex()) {
      return true;
    }
    if (emiStack instanceof EmiGroupStack groupStack) {
      if (groupStack.getItems().isEmpty()) {
        return false;
      }
    }
    return shouldDisplayCraftableInIndex()
        ? isKnownOrCraftable(emiStack)
        : isKnown(emiStack);
  }

  public static boolean shouldIngredientDisplay(EmiIngredient emiIngredient) {
    return shouldStackDisplay(emiIngredient.getEmiStacks().get(0));
  }

  /**
   * Returns true if catalysts are disabled or if at least one of the EmiRecipe's catalysts are known, or if there are no
   * catalysts.
   */
  private static boolean catalystsKnown(EmiRecipe recipe) {
    if (!requireCatalystsKnown()) return true;
    return (recipe.getCatalysts().isEmpty())
        || recipe.getCatalysts().stream().anyMatch(KnownItems::isKnown);
  }

  /**
   * Returns true if all the inputs for the given EmiRecipe are known, workstation is known (if required),
   * and at least one catalyst is known (or catalysts are not required).
   */
  public static boolean areAllKnown(EmiRecipe recipe) {
    if (!isModEnabled()) return true;
    return workstationsKnown(recipe.getCategory())
        && recipe.getInputs().stream().allMatch(KnownItems::isKnown)
        && catalystsKnown(recipe);
  }

  /**
   * Returns a list of EmiIngredients representing workstations associated with the given
   * EmiRecipeCategory that are known.
   */
  public static List<EmiIngredient> workstationsFiltered(EmiRecipeCategory category) {
    return EmiApi.getRecipeManager().getWorkstations(category).stream()
        .filter(KnownItems::isKnown)
        .toList();
  }

  /**
   * Returns true if the EmiRecipeCategory has at least one known workstation, or has no associated
   * workstations.
   */
  public static boolean workstationsKnown(EmiRecipeCategory category) {
    return displayWithUnknownWorkstation()
        || EmiApi.getRecipeManager().getWorkstations(category).isEmpty()
        || workstationsFiltered(category).stream().anyMatch(KnownItems::isKnown);
  }

  /**
   * This is used in EmiApiMixin to intercept the call that fetches the EntrySet stream of recipes
   * to display from the global Map, and returns a stream filtered to remove recipes with unknown
   * workstations or unknown ingredients.
   */
  public static Stream<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> filterEntrySet(
      Set<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> entrySet) {
    if (shouldBlackoutRecipes()) {
      return entrySet.stream();
    }
    return entrySet.stream()
        .filter(entry -> workstationsKnown(entry.getKey()))
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().stream().filter(KnownItems::areAllKnown).toList()))
        .entrySet()
        .stream();
  }

  public static void loadFromDisk() {
    clear();

    File worldDiscovered = getKnownItemsFile();

    if (!worldDiscovered.exists() && PRE_DISCOVERED.exists()) { // add pre discovered entries
      try {
        Files.copy(
            PRE_DISCOVERED.toPath(), worldDiscovered.toPath(), StandardCopyOption.REPLACE_EXISTING);

      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    if (worldDiscovered.exists()) { // load existing discoveries
      Reader reader = null;
      try {
        reader = new FileReader(worldDiscovered);
        JsonReader jsonReader = new JsonReader(reader);
        Constants.LOG.info("Loading existing discoveries");
        JsonArray json = gson.fromJson(jsonReader, JsonArray.class);

        if (json != null) {
          for (JsonElement element : json) {
            ResourceLocation loc = ResourceLocation.tryParse(element.getAsString());
            if (loc == null) continue;
            if (BuiltInRegistries.ITEM.containsKey(loc)) {
              knownItems.add(BuiltInRegistries.ITEM.get(loc));
            }
            if (BuiltInRegistries.FLUID.containsKey(loc)) {
              knownFluids.add(BuiltInRegistries.FLUID.get(loc));
            }
            if (BuiltInRegistries.MOB_EFFECT.containsKey(loc)) {
              knownEffects.add(BuiltInRegistries.MOB_EFFECT.get(loc));
            }
          }
        }

      } catch (Exception e) {
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }
  }

  static JsonArray discoveredToJson() {
    JsonArray array = new JsonArray();
    for (Item item : knownItems) {
      ResourceLocation loc = BuiltInRegistries.ITEM.getKey(item);
      array.add(loc.toString());
    }
    for (Fluid fluid : knownFluids) {
      ResourceLocation loc = BuiltInRegistries.FLUID.getKey(fluid);
      array.add(loc.toString());
    }
    for (MobEffect effect : knownEffects) {
      ResourceLocation loc = BuiltInRegistries.MOB_EFFECT.getKey(effect);
      if (loc != null) array.add(loc.toString());
    }
    return array;
  }

  public static Path getKnownItemsPath() {
    return DATA_PATH.resolve(getWorldName().replace('/', '_') + ".json");
  }

  public static File getKnownItemsFile() {
    if (!DATA_PATH.toFile().exists() && !DATA_PATH.toFile().mkdirs()) {
      throw new RuntimeException("Could not create data directory.");
    }
    return getKnownItemsPath().toFile();
  }

  public static void saveToDisk() {
    JsonWriter writer = null;
    try {
      writer = gson.newJsonWriter(new FileWriter(getKnownItemsFile()));
      writer.setIndent("    ");
      gson.toJson(discoveredToJson(), writer);
    } catch (Exception e) {
      Constants.LOG.error("Couldn't save discovered");
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  public static String getWorldName() {
    Minecraft client = Minecraft.getInstance();
    if (client.isLocalServer() && client.getSingleplayerServer() != null) {
      IntegratedServer server = client.getSingleplayerServer();
      GameProfile profile = server.getSingleplayerProfile();
      String levelId =
          ((MinecraftServerStorageSourceAccessor) server).getStorageSource().getLevelId();
      return profile != null ? profile.getName() + " - " + levelId : levelId;
    } else {
      ServerData serverdata = client.getCurrentServer();
      return serverdata != null ? serverdata.name : "unknown";
    }
  }
}
