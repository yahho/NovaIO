package space.yahho.mcmod.novaio.command.arguments;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.UuidArgument;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static space.yahho.mcmod.novaio.NovaIO.NUMBER_MANAGER;

public class NovaIoUuidArgument extends UuidArgument {
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        Set<UUID> set = NUMBER_MANAGER.getLoadedNumbers();
        if (context.getSource() instanceof SharedSuggestionProvider) {
            return SharedSuggestionProvider.suggest(set.stream().map(UUID::toString), builder);
        }
        return super.listSuggestions(context, builder);
    }

    public static NovaIoUuidArgument novaIoUuid() {
        return new NovaIoUuidArgument();
    }
}
