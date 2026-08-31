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
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.io.FileReader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AdvancementDiscoveryManager {

    public static final Gson GSON = new GsonBuilder()
            .setLenient()
            .registerTypeAdapter(new TypeToken<Set<String>>() {
            }.getType(), new StringOrCollectionDeserializer<>(HashSet::new))
            .registerTypeAdapter(new TypeToken<List<String>>() {
            }.getType(), new StringOrCollectionDeserializer<>(ArrayList::new))
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

        boolean[] foundFiles = new boolean[]{false};
        List<Path> searchDirs = List.of(advConfigDir, rootConfigDir);
        for (Path dir : searchDirs) {
            if (!Files.exists(dir)) continue;
            try (Stream<Path> paths = (dir.equals(advConfigDir) ? Files.walk(dir) : Files.list(dir))) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .forEach(path -> {
                            foundFiles[0] = true;
                            parseFile(path, loadedRules);
                        });
            } catch (Exception e) {
                Constants.LOG.error("EMI Discovery: Failed to read advancement rules from {}", dir, e);
            }
        }

        if (!foundFiles[0]) {
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
        applyRules(ALL_RULES);
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

        if (!candidateRules.isEmpty()) {
            applyRules(candidateRules);
        }
    }

    /**
     * I'm sorry murphy, I don't really use javadocs
     **/
    private static void applyRules(Collection<AdvancementDiscoveryRule> rules) {
        Set<Item> newlyDiscoveredItems = new HashSet<>();
        Set<Fluid> newlyDiscoveredFluids = new HashSet<>();
        Set<MobEffect> newlyDiscoveredEffects = new HashSet<>();

        for (AdvancementDiscoveryRule rule : rules) {
            if (rule.isSatisfied()) {
                newlyDiscoveredItems.addAll(rule.getOrResolveItems());
                newlyDiscoveredFluids.addAll(rule.getOrResolveFluids());
                newlyDiscoveredEffects.addAll(rule.getOrResolveEffects());
            }
        }

        boolean changed = false;
        if (!newlyDiscoveredItems.isEmpty()) {
            changed |= KnownItems.addKnownItems(newlyDiscoveredItems);
        }
        if (!newlyDiscoveredFluids.isEmpty()) {
            changed |= KnownItems.addKnownFluids(newlyDiscoveredFluids);
        }
        if (!newlyDiscoveredEffects.isEmpty()) {
            changed |= KnownItems.addKnownEffects(newlyDiscoveredEffects);
        }

        if (changed && EmiScreenManager.search != null) {
            EmiScreenManager.search.update();
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
                    ],
                    "fluids": [
                      "minecraft:water"
                    ],
                    "effects": [
                      "minecraft:speed"
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

    private record StringOrCollectionDeserializer<T extends Collection<String>>(
            Supplier<T> factory) implements JsonDeserializer<T> {

        @Override
        public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            T collection = factory.get();
            if (json.isJsonArray()) {
                json.getAsJsonArray().forEach(e -> {
                    if (e.isJsonPrimitive()) collection.add(e.getAsString());
                });
            } else if (json.isJsonPrimitive()) {
                collection.add(json.getAsString());
            }
            return collection;
        }
    }
}
