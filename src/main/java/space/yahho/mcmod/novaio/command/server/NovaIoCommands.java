package space.yahho.mcmod.novaio.command.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;
import space.yahho.mcmod.novaio.command.arguments.NovaIoUuidArgument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static net.minecraft.commands.Commands.LEVEL_ADMINS;
import static space.yahho.mcmod.novaio.NovaIO.*;

public class NovaIoCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal(MODID)
                .requires((src) -> src.hasPermission(LEVEL_ADMINS))
                .then(Commands.literal("updateNum")
                        .then(Commands.argument("UUID", NovaIoUuidArgument.novaIoUuid())
                                .then(Commands.literal("fromSource")
                                        .then(Commands.argument("filename", StringArgumentType.string())
                                                .executes((ctx) -> {
                                                    return updateNumber(ctx, NovaIoUuidArgument.getUuid(ctx, "UUID"), StringArgumentType.getString(ctx,"filename"));
                                                })
                                        )
                                )
                        )
                )
        );
    }

    private static int updateNumber(CommandContext<CommandSourceStack> context, UUID uuid, String filename) {
        if (!NUMBER_MANAGER.getLoadedNumbers().contains(uuid)) {
            context.getSource().sendFailure(Component.translatable("commands.novaio.updatenum.error.noid", uuid));
            return -1;
        }
        Path p = context.getSource().getServer().getWorldPath(LevelResource.ROOT).normalize().toAbsolutePath().resolve(MODNAME).resolve(filename);
        try {
            if (!p.toFile().isFile() || Files.size(p) <= 0) {
                context.getSource().sendFailure(Component.translatable("commands.novaio.updatenum.error.nofile", filename));
                return -1;
            }
            if (NUMBER_MANAGER.updateStoredNumber(uuid, p)) {
                context.getSource().sendSuccess(()-> Component.translatable("commands.novaio.updatenum.success"),true);
                return 0;
            } else {
                context.getSource().sendFailure(Component.translatable("commands.novaio.updatenum.error"));
                return -3;
            }
        } catch (IOException e) {
            context.getSource().sendFailure(Component.translatable("commands.novaio.updatenum.error.iofail"));
            LOGGER.error(e.getMessage());
            return -2;
        }
    }
}
