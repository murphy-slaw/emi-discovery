package net.funkpla.emi_discovery.advancement;

import com.google.gson.annotations.SerializedName;
import net.funkpla.emi_discovery.Constants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * All of this is ripped almost entirely from Reliable Remover... hey, if it works it works
 **/
public class AdvancementDiscoveryRule {

    @SerializedName(value = "advancements", alternate = {"advancement"})
    public Set<String> advancements = new HashSet<>();

    @SerializedName(value = "items", alternate = {"item"})
    public Set<String> items = new HashSet<>();

    @SerializedName(value = "fluids", alternate = {"fluid"})
    public Set<String> fluids = new HashSet<>();

    @SerializedName(value = "effects", alternate = {"effect", "mob_effects", "mob_effect", "status_effects", "status_effect"})
    public Set<String> effects = new HashSet<>();

    @SerializedName(value = "tags", alternate = {"tag"})
    public Set<String> tags = new HashSet<>();

    @SerializedName(value = "mod", alternate = {"mods"})
    public Set<String> mod = new HashSet<>();

    public String pattern;

    @SerializedName(value = "patterns", alternate = {"regex"})
    public List<String> patterns = new ArrayList<>();

    private transient volatile Set<Item> resolvedItemsCache = null;
    private transient volatile Set<Fluid> resolvedFluidsCache = null;
    private transient volatile Set<MobEffect> resolvedEffectsCache = null;
    private transient volatile List<Pattern> compiledPatterns = null;

    public boolean isSatisfied() {
        if (advancements == null || advancements.isEmpty()) {
            return false;
        }
        for (String advId : advancements) {
            ResourceLocation loc = ResourceLocation.tryParse(advId);
            if (loc == null || !AdvancementCache.isDone(loc)) {
                return false;
            }
        }
        return true;
    }

    public Set<Item> getOrResolveItems() {
        if (resolvedItemsCache != null) return resolvedItemsCache;
        synchronized (this) {
            if (resolvedItemsCache != null) return resolvedItemsCache;
            resolvedItemsCache = Collections.unmodifiableSet(
                resolveRegistryEntries(BuiltInRegistries.ITEM, Registries.ITEM, items, tags)
            );
            return resolvedItemsCache;
        }
    }

    public Set<Fluid> getOrResolveFluids() {
        if (resolvedFluidsCache != null) return resolvedFluidsCache;
        synchronized (this) {
            if (resolvedFluidsCache != null) return resolvedFluidsCache;
            resolvedFluidsCache = Collections.unmodifiableSet(
                resolveRegistryEntries(BuiltInRegistries.FLUID, Registries.FLUID, fluids, items, tags)
            );
            return resolvedFluidsCache;
        }
    }

    public Set<MobEffect> getOrResolveEffects() {
        if (resolvedEffectsCache != null) return resolvedEffectsCache;
        synchronized (this) {
            if (resolvedEffectsCache != null) return resolvedEffectsCache;
            resolvedEffectsCache = Collections.unmodifiableSet(
                resolveRegistryEntries(BuiltInRegistries.MOB_EFFECT, Registries.MOB_EFFECT, effects, items, tags)
            );
            return resolvedEffectsCache;
        }
    }

    @SafeVarargs
    private <T> Set<T> resolveRegistryEntries(Registry<T> registry, ResourceKey<? extends Registry<T>> registryKey, Set<String>... candidateSets) {
        Set<T> resolved = new HashSet<>();

        if (candidateSets != null) {
            for (Set<String> set : candidateSets) {
                if (set == null || set.isEmpty()) continue;
                for (String entry : set) {
                    if (entry == null || entry.isEmpty()) continue;
                    if (entry.startsWith("#")) {
                        resolveTag(entry.substring(1), registry, registryKey, resolved);
                    } else {
                        ResourceLocation loc = ResourceLocation.tryParse(entry);
                        if (loc != null && registry.containsKey(loc)) {
                            resolved.add(registry.get(loc));
                        }
                    }
                }
            }
        }

        // Mod namespace filters
        if (mod != null && !mod.isEmpty()) {
            for (Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
                if (mod.contains(entry.getKey().location().getNamespace())) {
                    resolved.add(entry.getValue());
                }
            }
        }

        // Regex patterns
        List<Pattern> patternList = getCompiledPatterns();
        if (!patternList.isEmpty()) {
            for (Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
                String idStr = entry.getKey().location().toString();
                for (Pattern p : patternList) {
                    if (p.matcher(idStr).matches()) {
                        resolved.add(entry.getValue());
                        break;
                    }
                }
            }
        }

        return resolved;
    }

    private <T> void resolveTag(String tagString, Registry<T> registry, ResourceKey<? extends Registry<T>> registryKey, Set<T> output) {
        ResourceLocation tagLoc = ResourceLocation.tryParse(tagString);
        if (tagLoc == null) return;
        TagKey<T> tagKey = TagKey.create(registryKey, tagLoc);
        registry.getTag(tagKey).ifPresent(holders -> {
            holders.forEach(holder -> output.add(holder.value()));
        });
    }

    public void invalidateCache() {
        this.resolvedItemsCache = null;
        this.resolvedFluidsCache = null;
        this.resolvedEffectsCache = null;
    }

    private List<Pattern> getCompiledPatterns() {
        if (compiledPatterns != null) return compiledPatterns;
        List<Pattern> list = new ArrayList<>();
        if (pattern != null && !pattern.isEmpty()) {
            Pattern p = compile(pattern);
            if (p != null) list.add(p);
        }
        if (patterns != null) {
            for (String s : patterns) {
                Pattern p = compile(s);
                if (p != null) list.add(p);
            }
        }
        compiledPatterns = list;
        return compiledPatterns;
    }

    private Pattern compile(String regex) {
        String p = regex.startsWith("/") && regex.endsWith("/")
                ? regex.substring(1, regex.length() - 1)
                : regex;
        try {
            return Pattern.compile(p);
        } catch (PatternSyntaxException e) {
            Constants.LOG.error("EMI Discovery: Invalid regex pattern in advancement rule: '{}'", regex, e);
            return null;
        }
    }
}
