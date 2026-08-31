package net.funkpla.emi_discovery;

import com.evandev.remi.feature.stackgroup.EmiGroupStack;
import com.evandev.remi.feature.stackgroup.GroupedEmiStack;
import com.evandev.remi.integration.emi.StackManager;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.authlib.GameProfile;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiCraftingRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.registry.EmiRecipes;
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class KnownItems {
    private static final Set<Item> knownItems = new HashSet<>();
    private static final Set<Fluid> knownFluids = new HashSet<>();
    private static final Set<MobEffect> knownEffects = new HashSet<>();
    private static final File PRE_DISCOVERED =
            new File("config", "emi_discovery_pre_discovered.json");
    private static final Path DATA_PATH =
            Services.PLATFORM.getGameDir().resolve(Path.of("moddata", "emi_discovery"));

    private static final AtomicInteger UPDATE_COUNT = new AtomicInteger();
    private static final Gson gson = new Gson();
    private static final ExecutorService SAVE_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EmiDiscovery-SaveThread");
        t.setDaemon(true);
        return t;
    });
    private static final LoadingCache<EmiStack, Boolean> stackDisplayCache =
            CacheBuilder.newBuilder()
                    .maximumSize(100000)
                    .build(
                            new CacheLoader<>() {
                                @Override
                                public @NotNull Boolean load(@NotNull EmiStack stack) {
                                    return shouldStackDisplayUncached(stack);
                                }
                            });

    public static int getUpdateCount() {
        return UPDATE_COUNT.get();
    }

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
        try {
            if (StackManager.sourceStacks != null && !StackManager.sourceStacks.isEmpty()) {
                StackManager.buildStacks(StackManager.sourceStacks);
                StackManager.repopulateIndexPanelsIfDirty();
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

    private static <T> boolean addKnownElements(Set<T> targetSet, Collection<T> elements, java.util.function.Predicate<T> filter) {
        if (elements == null || elements.isEmpty()) return false;
        boolean anyAdded = false;
        for (T elem : elements) {
            if (elem != null && filter.test(elem) && targetSet.add(elem)) {
                anyAdded = true;
            }
        }
        if (anyAdded) {
            invalidateCache();
            saveToDisk();
        }
        return anyAdded;
    }

    public static boolean addKnownItems(Collection<Item> items) {
        return addKnownElements(knownItems, items, item -> true);
    }

    public static boolean addKnownFluids(Collection<Fluid> fluids) {
        return addKnownElements(knownFluids, fluids, fluid -> fluid != Fluids.EMPTY);
    }

    public static boolean addKnownEffects(Collection<MobEffect> effects) {
        return addKnownElements(knownEffects, effects, effect -> true);
    }

    private static <T> boolean removeKnownElements(Set<T> targetSet, Collection<T> elements) {
        if (elements == null || elements.isEmpty()) return false;
        boolean anyRemoved = false;
        for (T elem : elements) {
            if (elem != null && targetSet.remove(elem)) {
                anyRemoved = true;
            }
        }
        if (anyRemoved) {
            invalidateCache();
            saveToDisk();
        }
        return anyRemoved;
    }

    public static boolean removeKnownItems(Collection<Item> items) {
        return removeKnownElements(knownItems, items);
    }

    public static boolean removeKnownFluids(Collection<Fluid> fluids) {
        return removeKnownElements(knownFluids, fluids);
    }

    public static boolean removeKnownEffects(Collection<MobEffect> effects) {
        return removeKnownElements(knownEffects, effects);
    }

    public static boolean removeKnown(Item item) {
        if (item != null && knownItems.remove(item)) {
            invalidateCache();
            saveToDisk();
            return true;
        }
        return false;
    }

    public static boolean removeKnown(Fluid fluid) {
        if (fluid != null && knownFluids.remove(fluid)) {
            invalidateCache();
            saveToDisk();
            return true;
        }
        return false;
    }

    public static boolean removeKnown(MobEffect effect) {
        if (effect != null && knownEffects.remove(effect)) {
            invalidateCache();
            saveToDisk();
            return true;
        }
        return false;
    }

    public static void addEverything() {
        boolean changed = false;
        for (Item item : BuiltInRegistries.ITEM) {
            if (item != null && item != Items.AIR) {
                changed |= knownItems.add(item);
            }
        }
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid != null && fluid != Fluids.EMPTY) {
                changed |= knownFluids.add(fluid);
            }
        }
        for (MobEffect effect : BuiltInRegistries.MOB_EFFECT) {
            if (effect != null) {
                changed |= knownEffects.add(effect);
            }
        }
        if (changed) {
            invalidateCache();
            saveToDisk();
        }
    }

    public static void clearAndSave() {
        clear();
        saveToDisk();
    }

    public static boolean addByIds(Collection<ResourceLocation> ids) {
        if (ids == null || ids.isEmpty()) return false;
        boolean changed = false;
        for (ResourceLocation id : ids) {
            if (id == null) continue;
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                changed |= knownItems.add(BuiltInRegistries.ITEM.get(id));
            }
            if (BuiltInRegistries.FLUID.containsKey(id)) {
                Fluid fluid = BuiltInRegistries.FLUID.get(id);
                if (fluid != Fluids.EMPTY) {
                    changed |= knownFluids.add(fluid);
                }
            }
            if (BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                changed |= knownEffects.add(BuiltInRegistries.MOB_EFFECT.get(id));
            }
        }
        if (changed) {
            invalidateCache();
            saveToDisk();
        }
        return changed;
    }

    public static boolean removeByIds(Collection<ResourceLocation> ids) {
        if (ids == null || ids.isEmpty()) return false;
        boolean changed = false;
        for (ResourceLocation id : ids) {
            if (id == null) continue;
            if (BuiltInRegistries.ITEM.containsKey(id)) {
                changed |= knownItems.remove(BuiltInRegistries.ITEM.get(id));
            }
            if (BuiltInRegistries.FLUID.containsKey(id)) {
                changed |= knownFluids.remove(BuiltInRegistries.FLUID.get(id));
            }
            if (BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                changed |= knownEffects.remove(BuiltInRegistries.MOB_EFFECT.get(id));
            }
        }
        if (changed) {
            invalidateCache();
            saveToDisk();
        }
        return changed;
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

    /**
     * Does the fluid exist in the known set or has its bucket item been discovered?
     */
    public static boolean isKnown(Fluid fluid) {
        if (!isModEnabled() || !isFluidDiscoveryEnabled()) return true;
        if (fluid == null || fluid == Fluids.EMPTY) return true;
        if (knownFluids.contains(fluid)) return true;
        Item bucket = fluid.getBucket();
        return bucket != Items.AIR && knownItems.contains(bucket);
    }

    /**
     * Does the mob effect exist in the known set?
     */
    public static boolean isKnown(MobEffect effect) {
        if (!isModEnabled() || !isEffectDiscoveryEnabled()) return true;
        if (effect == null) return true;
        return knownEffects.contains(effect);
    }

    /**
     * Convenience method to unwrap EmiStacks for the above.
     */
    public static boolean isKnown(EmiStack stack) {
        if (stack == null || stack.isEmpty()) return true;
        if (!isModEnabled()) return true;
        if (stack instanceof EmiGroupStack groupStack) return isKnown(groupStack);
        if (stack instanceof GroupedEmiStack<?> groupedStack) return isKnown(groupedStack.realStack);

        Object key = stack.getKey();
        if (key instanceof Item item) {
            return knownItems.contains(item);
        }
        if (key instanceof ItemStack itemStack) {
            return isKnown(itemStack);
        }
        if (key instanceof Fluid fluid) {
            return isKnown(fluid);
        }
        if (key instanceof MobEffect effect) {
            return isKnown(effect);
        }
        if (key instanceof Holder<?> holder && holder.value() instanceof MobEffect effect) {
            return isKnown(effect);
        }

        ItemStack itemStack = stack.getItemStack();
        if (itemStack != null && !itemStack.isEmpty()) {
            return isKnown(itemStack);
        }

        ResourceLocation id = stack.getId();
        if (id != null) {
            if (isFluidDiscoveryEnabled() && BuiltInRegistries.FLUID.containsKey(id)) {
                return isKnown(BuiltInRegistries.FLUID.get(id));
            }
            if (isEffectDiscoveryEnabled() && BuiltInRegistries.MOB_EFFECT.containsKey(id)) {
                return isKnown(BuiltInRegistries.MOB_EFFECT.get(id));
            }
        }
        return true;
    }

    /**
     * For ingredients, if any stack matches, we call it a match. This gives the expected behavior for
     * both single-item stacks and tag and list stacks.
     */
    public static boolean isKnown(EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return true;
        List<EmiStack> stacks = ingredient.getEmiStacks();
        for (EmiStack stack : stacks) {
            if (isKnown(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method takes an EmiGroupStack and returns true if any of the items associated
     * with any of the GroupedEmiStacks are known.
     */
    public static boolean isKnown(EmiGroupStack groupStack) {
        if (groupStack == null) return false;
        var items = groupStack.getItems();
        if (items.isEmpty()) return false;
        for (GroupedEmiStack<EmiStack> item : items) {
            if (isKnown(item.realStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any of the EmiStacks in the EmiIngredient are known.
     */
    public static boolean areAnyKnown(EmiIngredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return true;
        List<EmiStack> stacks = ingredient.getEmiStacks();
        for (EmiStack stack : stacks) {
            if (isKnownOrCraftable(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the provided emiStack is known or craftable.
     */
    public static boolean isKnownOrCraftable(EmiStack emiStack) {
        return isCraftable(emiStack) || isKnown(emiStack);
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

    public static boolean exemptInventoryCrafting() {
        return getConfig().exemptInventoryCrafting;
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
        if (emiStack == null || emiStack.isEmpty()) return false;
        if (emiStack instanceof EmiGroupStack groupStack) return isCraftable(groupStack);
        if (emiStack instanceof GroupedEmiStack<?> groupedStack) return isCraftable(groupedStack.realStack);
        if (EmiRecipes.manager == null) return false;
        try {
            List<EmiRecipe> recipes = EmiRecipes.manager.getRecipesByOutput(emiStack);
            if (recipes == null || recipes.isEmpty()) return false;

            boolean reqWorkstation = requireWorkstationForCraftable();
            for (EmiRecipe recipe : recipes) {
                if (recipe == null) continue;

                if (reqWorkstation && !isWorkstationKnownForRecipe(recipe)) {
                    continue;
                }
                if (!catalystsKnown(recipe)) {
                    continue;
                }

                List<EmiIngredient> inputs = recipe.getInputs();
                if (inputs == null || inputs.isEmpty()) continue;

                boolean allInputsKnown = true;
                for (EmiIngredient input : inputs) {
                    if (!isKnown(input)) {
                        allInputsKnown = false;
                        break;
                    }
                }
                if (allInputsKnown) {
                    return true;
                }
            }
            return false;
        } catch (Throwable e) {
            Constants.LOG.error("Unexpected error checking isCraftable for {}", emiStack, e);
            return false;
        }
    }

    /**
     * For EmiGroupStacks, we call the stack craftable if any of the real stacks are craftable.
     */
    public static boolean isCraftable(EmiGroupStack groupStack) {
        if (groupStack == null) return false;
        var items = groupStack.getItems();
        if (items.isEmpty()) return false;
        for (GroupedEmiStack<EmiStack> item : items) {
            if (isCraftable(item.realStack)) {
                return true;
            }
        }
        return false;
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
        if (emiIngredient == null || emiIngredient.isEmpty()) return true;
        if (emiIngredient instanceof EmiStack stack) {
            return shouldStackDisplay(stack);
        }
        List<EmiStack> stacks = emiIngredient.getEmiStacks();
        if (stacks.isEmpty()) return false;
        for (EmiStack stack : stacks) {
            if (shouldStackDisplay(stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if catalysts are disabled or if at least one of the EmiRecipe's catalysts are known, or if there are no
     * catalysts.
     */
    private static boolean catalystsKnown(EmiRecipe recipe) {
        if (!requireCatalystsKnown()) return true;
        if (recipe == null) return true;
        List<EmiIngredient> catalysts = recipe.getCatalysts();
        if (catalysts == null || catalysts.isEmpty()) return true;
        for (EmiIngredient catalyst : catalysts) {
            if (isKnown(catalyst)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if all the inputs for the given EmiRecipe are known, workstation is known (if required
     * because displayWithUnknownWorkstation is false), and at least one catalyst is known (or catalysts are not required).
     */
    public static boolean areAllKnown(EmiRecipe recipe) {
        if (!isModEnabled()) return true;
        if (recipe == null) return true;
        if (!displayWithUnknownWorkstation() && !isWorkstationKnownForRecipe(recipe)) return false;
        if (!catalystsKnown(recipe)) return false;
        List<EmiIngredient> inputs = recipe.getInputs();
        if (inputs == null || inputs.isEmpty()) return true;
        for (EmiIngredient input : inputs) {
            if (!isKnown(input)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a list of EmiIngredients representing workstations associated with the given
     * EmiRecipeCategory that are known.
     */
    public static List<EmiIngredient> workstationsFiltered(EmiRecipeCategory category) {
        if (category == null || EmiApi.getRecipeManager() == null) return List.of();
        List<EmiIngredient> workstations = EmiApi.getRecipeManager().getWorkstations(category);
        if (workstations == null || workstations.isEmpty()) return List.of();
        List<EmiIngredient> filtered = new ArrayList<>(workstations.size());
        for (EmiIngredient ws : workstations) {
            if (isKnown(ws)) {
                filtered.add(ws);
            }
        }
        return filtered;
    }

    /**
     * Returns true if the EmiRecipe is a 2x2 crafting recipe (can be crafted in player inventory without workstation),
     * or if the recipe's category has at least one known workstation (or no associated workstations).
     */
    public static boolean isWorkstationKnownForRecipe(EmiRecipe recipe) {
        if (recipe == null) return true;
        if (exemptInventoryCrafting() && recipe instanceof EmiCraftingRecipe craftingRecipe && craftingRecipe.canFit(2, 2)) {
            return true;
        }
        return workstationsKnown(recipe.getCategory());
    }

    /**
     * Returns true if the EmiRecipeCategory has at least one known workstation, or has no associated
     * workstations.
     */
    public static boolean workstationsKnown(EmiRecipeCategory category) {
        if (category == null || EmiApi.getRecipeManager() == null) return true;
        List<EmiIngredient> workstations = EmiApi.getRecipeManager().getWorkstations(category);
        if (workstations == null || workstations.isEmpty()) return true;
        for (EmiIngredient workstation : workstations) {
            if (isKnown(workstation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This is used in EmiApiMixin to intercept the call that fetches the EntrySet stream of recipes
     * to display from the global Map, and returns a stream filtered to remove recipes with unknown
     * workstations or unknown ingredients.
     */
    public static Stream<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> filterEntrySet(
            Set<Map.Entry<EmiRecipeCategory, List<EmiRecipe>>> entrySet) {
        if (!isModEnabled()) {
            return entrySet.stream();
        }
        boolean blackout = shouldBlackoutRecipes();
        boolean allowUnknownWorkstation = displayWithUnknownWorkstation();

        Map<EmiRecipeCategory, List<EmiRecipe>> result = new HashMap<>();
        for (Map.Entry<EmiRecipeCategory, List<EmiRecipe>> entry : entrySet) {
            EmiRecipeCategory category = entry.getKey();
            List<EmiRecipe> recipes = entry.getValue();
            List<EmiRecipe> filtered = new ArrayList<>(recipes.size());
            for (EmiRecipe r : recipes) {
                if (!allowUnknownWorkstation && !isWorkstationKnownForRecipe(r)) {
                    continue;
                }
                if (blackout || areAllKnown(r)) {
                    filtered.add(r);
                }
            }
            if (!filtered.isEmpty()) {
                result.put(category, filtered);
            }
        }
        return result.entrySet().stream();
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
        invalidateCache();
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
        final JsonArray jsonSnapshot;
        synchronized (knownItems) {
            jsonSnapshot = discoveredToJson();
        }
        SAVE_EXECUTOR.execute(() -> {
            JsonWriter writer = null;
            try {
                writer = gson.newJsonWriter(new FileWriter(getKnownItemsFile()));
                writer.setIndent("    ");
                gson.toJson(jsonSnapshot, writer);
            } catch (Exception e) {
                Constants.LOG.error("Couldn't save discovered", e);
            } finally {
                IOUtils.closeQuietly(writer);
            }
        });
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
