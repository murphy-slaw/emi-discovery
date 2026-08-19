package net.funkpla.emi_discovery.advancement;

import com.google.gson.annotations.SerializedName;
import net.funkpla.emi_discovery.Constants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

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

    @SerializedName(value = "tags", alternate = {"tag"})
    public Set<String> tags = new HashSet<>();

    @SerializedName(value = "mod", alternate = {"mods"})
    public Set<String> mod = new HashSet<>();

    public String pattern;

    @SerializedName(value = "patterns", alternate = {"regex"})
    public List<String> patterns = new ArrayList<>();

    private transient volatile Set<Item> resolvedItemsCache = null;
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
        if (resolvedItemsCache != null) {
            return resolvedItemsCache;
        }
        synchronized (this) {
            if (resolvedItemsCache != null) {
                return resolvedItemsCache;
            }
            Set<Item> resolved = new HashSet<>();

            // items or tags prefixed by #
            if (items != null) {
                for (String entry : items) {
                    if (entry == null || entry.isEmpty()) continue;
                    if (entry.startsWith("#")) {
                        resolveTag(entry.substring(1), resolved);
                    } else {
                        ResourceLocation loc = ResourceLocation.tryParse(entry);
                        if (loc != null && BuiltInRegistries.ITEM.containsKey(loc)) {
                            resolved.add(BuiltInRegistries.ITEM.get(loc));
                        }
                    }
                }
            }

            // tags
            if (tags != null) {
                for (String tagStr : tags) {
                    if (tagStr == null || tagStr.isEmpty()) continue;
                    String clean = tagStr.startsWith("#") ? tagStr.substring(1) : tagStr;
                    resolveTag(clean, resolved);
                }
            }

            // mod filters
            if (mod != null && !mod.isEmpty()) {
                for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
                    if (mod.contains(entry.getKey().location().getNamespace())) {
                        resolved.add(entry.getValue());
                    }
                }
            }

            // regex patterns
            List<Pattern> patternList = getCompiledPatterns();
            if (!patternList.isEmpty()) {
                for (Map.Entry<ResourceKey<Item>, Item> entry : BuiltInRegistries.ITEM.entrySet()) {
                    String idStr = entry.getKey().location().toString();
                    for (Pattern p : patternList) {
                        if (p.matcher(idStr).matches()) {
                            resolved.add(entry.getValue());
                            break;
                        }
                    }
                }
            }

            resolvedItemsCache = Collections.unmodifiableSet(resolved);
            return resolvedItemsCache;
        }
    }

    private void resolveTag(String tagString, Set<Item> output) {
        ResourceLocation tagLoc = ResourceLocation.tryParse(tagString);
        if (tagLoc == null) return;
        TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagLoc);
        BuiltInRegistries.ITEM.getTag(tagKey).ifPresent(holders -> {
            holders.forEach(holder -> output.add(holder.value()));
        });
    }

    public void invalidateCache() {
        this.resolvedItemsCache = null;
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
