package com.tiers.misc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.tiers.TiersClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.command.CommandSource;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class CommandRegister {
    private static final SuggestionProvider<FabricClientCommandSource> PLAYERS = (commandContext, suggestionsBuilder) -> suggestPlayers(suggestionsBuilder);

private static CompletableFuture<Suggestions> suggestPlayers(SuggestionsBuilder suggestionsBuilder) {
    MinecraftClient minecraftClient = MinecraftClient.getInstance();
    if (minecraftClient.world == null || minecraftClient.getNetworkHandler() == null)
        return suggestionsBuilder.buildFuture();

    if (CommandSource.shouldSuggest(suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT), "-config"))
        suggestionsBuilder.suggest("-config", () -> "Open Tiers config screen");

    for (PlayerListEntry playerListEntry : minecraftClient.getNetworkHandler().getPlayerList())
        if (CommandSource.shouldSuggest(suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT), playerListEntry.getProfile().name().toLowerCase(Locale.ROOT)) && playerListEntry.getProfile().name().length() > 2)
            suggestionsBuilder.suggest(playerListEntry.getProfile().name(), () -> "Search tiers for " + playerListEntry.getProfile().name());

    return suggestionsBuilder.buildFuture();
}

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess) -> commandDispatcher.register(
                ClientCommandManager.literal("vtiers").executes(ignored -> {
                            TiersClient.toggleMod(null);
                            return 1;
                        })
                        .then(ClientCommandManager.argument("Name", StringArgumentType.string()).suggests(PLAYERS).executes(context -> {
                                    TiersClient.tiersCommand(StringArgumentType.getString(context, "Name"));
                                    return 1;
                                })
                        )
        ));
    }
}