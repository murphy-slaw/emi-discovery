package net.funkpla.emi_discovery.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.funkpla.emi_discovery.ServerDiscoveryTracker;
import net.funkpla.emi_discovery.network.client.S2CDiscoveryPacket;
import net.funkpla.emi_discovery.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DiscoveryCommand {

    private static final DynamicCommandExceptionType ERROR_NOT_FOUND = new DynamicCommandExceptionType(
            id -> Component.translatableWithFallback("commands.discovery.notFound", "Unknown item, fluid, or effect: " + id, id)
    );

    private static final DynamicCommandExceptionType ERROR_TAG_NOT_FOUND = new DynamicCommandExceptionType(
            tag -> Component.translatableWithFallback("commands.discovery.tagNotFound", "Tag not found or empty: #" + tag, tag)
    );

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_DISCOVERABLES = (context, builder) -> {
        SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet().stream(), builder);
        SharedSuggestionProvider.suggestResource(BuiltInRegistries.FLUID.keySet().stream().filter(f -> !f.getPath().equals("empty")), builder);
        SharedSuggestionProvider.suggestResource(BuiltInRegistries.MOB_EFFECT.keySet().stream(), builder);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TAGS = (context, builder) -> {
        SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.getTagNames().map(TagKey::location), builder);
        SharedSuggestionProvider.suggestResource(BuiltInRegistries.FLUID.getTagNames().map(TagKey::location), builder);
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> discovery = buildRoot("discovery");
        LiteralArgumentBuilder<CommandSourceStack> emidiscovery = buildRoot("emidiscovery");

        dispatcher.register(discovery);
        dispatcher.register(emidiscovery);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildRoot(String name) {
        return Commands.literal(name)
                .requires(source -> source.hasPermission(2))
                .then(buildActionSubtree(Action.GRANT))
                .then(buildActionSubtree(Action.REVOKE));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildActionSubtree(Action action) {
        return Commands.literal(action.getName())
                .then(
                        Commands.argument("targets", EntityArgument.players())
                                .then(
                                        Commands.literal("only")
                                                .then(
                                                        Commands.argument("id", ResourceLocationArgument.id())
                                                                .suggests(SUGGEST_DISCOVERABLES)
                                                                .executes(context -> performSingle(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayers(context, "targets"),
                                                                        action,
                                                                        ResourceLocationArgument.getId(context, "id")
                                                                ))
                                                )
                                )
                                .then(
                                        Commands.literal("from")
                                                .then(
                                                        Commands.argument("id", ResourceLocationArgument.id())
                                                                .suggests(SUGGEST_DISCOVERABLES)
                                                                .executes(context -> performSingle(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayers(context, "targets"),
                                                                        action,
                                                                        ResourceLocationArgument.getId(context, "id")
                                                                ))
                                                )
                                )
                                .then(
                                        Commands.literal("until")
                                                .then(
                                                        Commands.argument("id", ResourceLocationArgument.id())
                                                                .suggests(SUGGEST_DISCOVERABLES)
                                                                .executes(context -> performSingle(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayers(context, "targets"),
                                                                        action,
                                                                        ResourceLocationArgument.getId(context, "id")
                                                                ))
                                                )
                                )
                                .then(
                                        Commands.literal("through")
                                                .then(
                                                        Commands.argument("id", ResourceLocationArgument.id())
                                                                .suggests(SUGGEST_DISCOVERABLES)
                                                                .executes(context -> performSingle(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayers(context, "targets"),
                                                                        action,
                                                                        ResourceLocationArgument.getId(context, "id")
                                                                ))
                                                )
                                )
                                .then(
                                        Commands.literal("tag")
                                                .then(
                                                        Commands.argument("tag", ResourceLocationArgument.id())
                                                                .suggests(SUGGEST_TAGS)
                                                                .executes(context -> performTag(
                                                                        context.getSource(),
                                                                        EntityArgument.getPlayers(context, "targets"),
                                                                        action,
                                                                        ResourceLocationArgument.getId(context, "tag")
                                                                ))
                                                )
                                )
                                .then(
                                        Commands.literal("everything")
                                                .executes(context -> performEverything(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        action
                                                ))
                                )
                );
    }

    private static int performSingle(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            Action action,
            ResourceLocation id
    ) throws CommandSyntaxException {
        boolean isItem = BuiltInRegistries.ITEM.containsKey(id);
        boolean isFluid = BuiltInRegistries.FLUID.containsKey(id);
        boolean isEffect = BuiltInRegistries.MOB_EFFECT.containsKey(id);

        if (!isItem && !isFluid && !isEffect) {
            throw ERROR_NOT_FOUND.create(id);
        }

        for (ServerPlayer player : targets) {
            if (action == Action.REVOKE && isItem) {
                Item item = BuiltInRegistries.ITEM.get(id);
                ServerDiscoveryTracker.removeFromCache(player, List.of(item));
            }
            Services.PLATFORM.sendToClient(
                    new S2CDiscoveryPacket(action.toPacketAction(), false, List.of(id)),
                    player
            );
        }

        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            source.sendSuccess(
                    () -> Component.literal((action == Action.GRANT ? "Granted " : "Revoked ") + id + (action == Action.GRANT ? " to " : " from ") + target.getScoreboardName()),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal((action == Action.GRANT ? "Granted " : "Revoked ") + id + (action == Action.GRANT ? " to " : " from ") + targets.size() + " players"),
                    true
            );
        }

        return targets.size();
    }

    private static int performTag(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            Action action,
            ResourceLocation tagId
    ) throws CommandSyntaxException {
        List<ResourceLocation> resolvedIds = new ArrayList<>();
        List<Item> resolvedItems = new ArrayList<>();

        Optional<Iterable<Holder<Item>>> itemTag = BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, tagId)).map(t -> t);
        if (itemTag.isPresent()) {
            for (Holder<Item> holder : itemTag.get()) {
                resolvedIds.add(BuiltInRegistries.ITEM.getKey(holder.value()));
                resolvedItems.add(holder.value());
            }
        }

        Optional<Iterable<Holder<Fluid>>> fluidTag = BuiltInRegistries.FLUID.getTag(TagKey.create(Registries.FLUID, tagId)).map(t -> t);
        if (fluidTag.isPresent()) {
            for (Holder<Fluid> holder : fluidTag.get()) {
                resolvedIds.add(BuiltInRegistries.FLUID.getKey(holder.value()));
            }
        }

        if (resolvedIds.isEmpty()) {
            throw ERROR_TAG_NOT_FOUND.create(tagId);
        }

        for (ServerPlayer player : targets) {
            if (action == Action.REVOKE && !resolvedItems.isEmpty()) {
                ServerDiscoveryTracker.removeFromCache(player, resolvedItems);
            }
            Services.PLATFORM.sendToClient(
                    new S2CDiscoveryPacket(action.toPacketAction(), false, resolvedIds),
                    player
            );
        }

        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            source.sendSuccess(
                    () -> Component.literal((action == Action.GRANT ? "Granted " : "Revoked ") + resolvedIds.size() + " discoveries for #" + tagId + (action == Action.GRANT ? " to " : " from ") + target.getScoreboardName()),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal((action == Action.GRANT ? "Granted " : "Revoked ") + resolvedIds.size() + " discoveries for #" + tagId + (action == Action.GRANT ? " to " : " from ") + targets.size() + " players"),
                    true
            );
        }

        return resolvedIds.size() * targets.size();
    }

    private static int performEverything(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            Action action
    ) {
        for (ServerPlayer player : targets) {
            if (action == Action.REVOKE) {
                ServerDiscoveryTracker.clearCache(player);
            }
            Services.PLATFORM.sendToClient(
                    new S2CDiscoveryPacket(action.toPacketAction(), true, List.of()),
                    player
            );
        }

        if (targets.size() == 1) {
            ServerPlayer target = targets.iterator().next();
            source.sendSuccess(
                    () -> Component.literal((action == Action.GRANT ? "Granted all discoveries to " : "Revoked all discoveries from ") + target.getScoreboardName()),
                    true
            );
        } else {
            source.sendSuccess(
                    () -> Component.literal((action == Action.GRANT ? "Granted all discoveries to " : "Revoked all discoveries from ") + targets.size() + " players"),
                    true
            );
        }

        return targets.size();
    }

    public enum Action {
        GRANT("grant"),
        REVOKE("revoke");

        private final String name;

        Action(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public S2CDiscoveryPacket.Action toPacketAction() {
            return this == GRANT ? S2CDiscoveryPacket.Action.GRANT : S2CDiscoveryPacket.Action.REVOKE;
        }
    }
}
