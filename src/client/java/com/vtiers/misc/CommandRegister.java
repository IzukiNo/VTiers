package com.vtiers.misc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.vtiers.VTiersClient;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class CommandRegister {
    private static final SuggestionProvider<FabricClientCommandSource> PLAYERS = (commandContext, suggestionsBuilder) -> suggestPlayers(suggestionsBuilder);

private static CompletableFuture<Suggestions> suggestPlayers(SuggestionsBuilder suggestionsBuilder) {
    Minecraft minecraftClient = Minecraft.getInstance();
    if (minecraftClient.level == null || minecraftClient.getConnection() == null)
        return suggestionsBuilder.buildFuture();

    if (SharedSuggestionProvider.matchesSubStr(suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT), "-config"))
        suggestionsBuilder.suggest("-config", () -> "Open VTiers config screen");

    for (PlayerInfo playerListEntry : minecraftClient.getConnection().getOnlinePlayers())
        if (SharedSuggestionProvider.matchesSubStr(suggestionsBuilder.getRemaining().toLowerCase(Locale.ROOT), playerListEntry.getProfile().name().toLowerCase(Locale.ROOT)) && playerListEntry.getProfile().name().length() > 2)
            suggestionsBuilder.suggest(playerListEntry.getProfile().name(), () -> "Search tiers for " + playerListEntry.getProfile().name());

    return suggestionsBuilder.buildFuture();
}

    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((commandDispatcher, commandRegistryAccess) -> commandDispatcher.register(
                ClientCommands.literal("vtiers").executes(ignored -> {
                            VTiersClient.toggleMod(null);
                            return 1;
                        })
                        .then(ClientCommands.argument("Name", StringArgumentType.string()).suggests(PLAYERS).executes(context -> {
                                    VTiersClient.tiersCommand(StringArgumentType.getString(context, "Name"));
                                    return 1;
                                })
                        )
        ));
    }
}