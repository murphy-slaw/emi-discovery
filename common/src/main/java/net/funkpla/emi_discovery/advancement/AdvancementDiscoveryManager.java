package net.funkpla.emi_discovery.advancement;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import dev.emi.emi.screen.EmiScreenManager;
import net.funkpla.emi_discovery.Constants;
import net.funkpla.emi_discovery.KnownItems;
import net.funkpla.emi_discovery.platform.Services;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class AdvancementDiscoveryManager {

    public static final Gson GSON = new GsonBuilder()
            .setLenient()
            .registerTypeAdapter(new TypeToken<Set<String>>() {
            }.getType(), new StringOrSetDeserializer())
            .registerTypeAdapter(new TypeToken<List<String>>() {
            }.getType(), new StringOrListDeserializer())
            .create();

    private static volatile List<AdvancementDiscoveryRule> ALL_RULES = new ArrayList<>();
    private static volatile Map<ResourceLocation, List<AdvancementDiscoveryRule>> RULES_BY_ADVANCEMENT = new HashMap<>();
    private static volatile Set<ResourceLocation> TRACKED_ADVANCEMENTS = ConcurrentHashMap.newKeySet();
    private static volatile boolean HAS_RULES = false;

    public static boolean hasAdvancementRules() {
        return HAS_RULES;
    }

    public static boolean isAdvancementTracked(ResourceLocation id) {
        return TRACKED_ADVANCEMENTS.contains(id);
    }

    public static synchronized void load() {
        List<AdvancementDiscoveryRule> loadedRules = new ArrayList<>();
        Path rootConfigDir = Services.PLATFORM.getGameDir().resolve("config").resolve("emi_discovery");
        Path advConfigDir = rootConfigDir.resolve("advancements");

        try {
            if (!Files.exists(advConfigDir)) {
                Files.createDirectories(advConfigDir);
            }
        } catch (Exception ignored) {
        }

        boolean foundFiles = false;

        // check config/emi_discovery/advancements/
        if (Files.exists(advConfigDir)) {
            try (Stream<Path> paths = Files.walk(advConfigDir)) {
                List<Path> files = paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();
                if (!files.isEmpty()) {
                    foundFiles = true;
                    files.forEach(path -> parseFile(path, loadedRules));
                }
            } catch (Exception e) {
                Constants.LOG.error("EMI Discovery: Failed to read advancement rules from {}", advConfigDir, e);
            }
        }

        // also check config/emi_discovery/*.json (except world data)
        if (Files.exists(rootConfigDir)) {
            try (Stream<Path> paths = Files.list(rootConfigDir)) {
                List<Path> files = paths.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();
                if (!files.isEmpty()) {
                    foundFiles = true;
                    files.forEach(path -> parseFile(path, loadedRules));
                }
            } catch (Exception e) {
                Constants.LOG.error("EMI Discovery: Failed to read rules from {}", rootConfigDir, e);
            }
        }

        if (!foundFiles) {
            generateDefaultConfig(advConfigDir);
        }

        Map<ResourceLocation, List<AdvancementDiscoveryRule>> byAdv = new HashMap<>();
        Set<ResourceLocation> tracked = ConcurrentHashMap.newKeySet();

        for (AdvancementDiscoveryRule rule : loadedRules) {
            if (rule.advancements != null) {
                for (String advStr : rule.advancements) {
                    ResourceLocation loc = ResourceLocation.tryParse(advStr);
                    if (loc != null) {
                        byAdv.computeIfAbsent(loc, k -> new ArrayList<>()).add(rule);
                        tracked.add(loc);
                    }
                }
            }
        }

        ALL_RULES = loadedRules;
        RULES_BY_ADVANCEMENT = byAdv;
        TRACKED_ADVANCEMENTS = tracked;
        HAS_RULES = !loadedRules.isEmpty();

        Constants.LOG.info("EMI Discovery: Loaded {} advancement discovery rules (tracking {} advancements).",
                loadedRules.size(), tracked.size());
    }

    public static void evaluateAll() {
        if (!HAS_RULES || !KnownItems.isAdvancementDiscoveryEnabled()) return;
        Set<Item> newlyDiscovered = new HashSet<>();

        for (AdvancementDiscoveryRule rule : ALL_RULES) {
            if (rule.isSatisfied()) {
                newlyDiscovered.addAll(rule.getOrResolveItems());
            }
        }

        if (!newlyDiscovered.isEmpty()) {
            boolean changed = KnownItems.addKnownItems(newlyDiscovered);
            if (changed && EmiScreenManager.search != null) {
                EmiScreenManager.search.update();
            }
        }
    }

    public static void onAdvancementsUpdated(Set<ResourceLocation> changedAdvancements) {
        if (!HAS_RULES || !KnownItems.isAdvancementDiscoveryEnabled() || changedAdvancements == null || changedAdvancements.isEmpty())
            return;

        Set<AdvancementDiscoveryRule> candidateRules = new HashSet<>();
        for (ResourceLocation advId : changedAdvancements) {
            List<AdvancementDiscoveryRule> rules = RULES_BY_ADVANCEMENT.get(advId);
            if (rules != null) {
                candidateRules.addAll(rules);
            }
        }

        if (candidateRules.isEmpty()) return;

        Set<Item> newlyDiscovered = new HashSet<>();
        for (AdvancementDiscoveryRule rule : candidateRules) {
            if (rule.isSatisfied()) {
                newlyDiscovered.addAll(rule.getOrResolveItems());
            }
        }

        if (!newlyDiscovered.isEmpty()) {
            boolean changed = KnownItems.addKnownItems(newlyDiscovered);
            if (changed && EmiScreenManager.search != null) {
                EmiScreenManager.search.update();
            }
        }
    }

    public static void reset() {
        for (AdvancementDiscoveryRule rule : ALL_RULES) {
            rule.invalidateCache();
        }
    }

    private static void parseFile(Path path, List<AdvancementDiscoveryRule> output) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            JsonReader reader = new JsonReader(fileReader);
            reader.setLenient(true);

            while (reader.peek() != JsonToken.END_DOCUMENT) {
                JsonElement json = JsonParser.parseReader(reader);
                if (json.isJsonArray()) {
                    for (JsonElement elem : json.getAsJsonArray()) {
                        if (elem.isJsonObject()) {
                            AdvancementDiscoveryRule rule = GSON.fromJson(elem, AdvancementDiscoveryRule.class);
                            if (rule != null) output.add(rule);
                        }
                    }
                } else if (json.isJsonObject()) {
                    AdvancementDiscoveryRule rule = GSON.fromJson(json, AdvancementDiscoveryRule.class);
                    if (rule != null) output.add(rule);
                }
            }
        } catch (Exception e) {
            Constants.LOG.error("EMI Discovery: Error parsing advancement rule file: {}", path, e);
        }
    }

    private static void generateDefaultConfig(Path dir) {
        String defaultJson = """
                [
                  {
                    "advancements": [
                      "minecraft:story/mine_stone"
                    ],
                    "items": [
                      "minecraft:furnace",
                      "minecraft:stone_pickaxe"
                    ],
                    "tags": [
                      "#minecraft:stone_tool_materials"
                    ]
                  }
                ]
                """;
        try {
            Files.writeString(dir.resolve("example.json.disabled"), defaultJson);
            Constants.LOG.info("EMI Discovery: Created default example config at config/emi_discovery/advancements/example.json.disabled");
        } catch (Exception e) {
            Constants.LOG.error("EMI Discovery: Failed to create example config file", e);
        }
    }

    private static class StringOrSetDeserializer implements JsonDeserializer<Set<String>> {
        @Override
        public Set<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            Set<String> set = new HashSet<>();
            if (json.isJsonArray()) {
                json.getAsJsonArray().forEach(e -> {
                    if (e.isJsonPrimitive()) set.add(e.getAsString());
                });
            } else if (json.isJsonPrimitive()) {
                set.add(json.getAsString());
            }
            return set;
        }
    }

    private static class StringOrListDeserializer implements JsonDeserializer<List<String>> {
        @Override
        public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            List<String> list = new ArrayList<>();
            if (json.isJsonArray()) {
                json.getAsJsonArray().forEach(e -> {
                    if (e.isJsonPrimitive()) list.add(e.getAsString());
                });
            } else if (json.isJsonPrimitive()) {
                list.add(json.getAsString());
            }
            return list;
        }
    }
}
