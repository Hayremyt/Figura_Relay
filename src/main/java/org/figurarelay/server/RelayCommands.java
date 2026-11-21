package org.figurarelay.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.figurarelay.net.HashPayload;
import org.figurarelay.net.RelayOffPayload;

import java.util.UUID;
import java.util.function.BiConsumer;

/** "/figura relay ..." commands. Registered by the platform modules. */
public class RelayCommands {

    @FunctionalInterface
    public interface PlayerPacketSender {
        void send(ServerPlayer player, CustomPacketPayload payload);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
                                 PlayerPacketSender sender) {
        dispatcher.register(Commands.literal("figura")
                .then(Commands.literal("relay")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("ban")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> banPlayer(ctx, sender))))
                        .then(Commands.literal("unban")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .executes(ctx -> unbanPlayer(ctx, sender))))
                        .then(Commands.literal("banlist")
                                .executes(RelayCommands::listBanned))
                        .then(Commands.literal("reload")
                                .executes(RelayCommands::reloadConfig))
        ));
    }

    private static int banPlayer(CommandContext<CommandSourceStack> ctx, PlayerPacketSender sender) {
        String name = StringArgumentType.getString(ctx, "player");
        CommandSourceStack source = ctx.getSource();

        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) {
            RelayPackets.banPlayer(online, source.getServer(),
                    p -> sender.send(p, new HashPayload(online.getUUID(), "")),
                    p -> sender.send(p, new RelayOffPayload()));
            source.sendSuccess(() -> Component.literal("Banned " + name + " from Figura relay"), false);
            return 1;
        }

        var profile = source.getServer().getProfileCache().get(name);
        if (profile.isPresent()) {
            RelayPackets.banPlayer(profile.get().getId());
            source.sendSuccess(() -> Component.literal("Banned " + name + " (offline) from Figura relay"), false);
            return 1;
        }

        source.sendFailure(Component.literal("Player not found: " + name));
        return 0;
    }

    private static int unbanPlayer(CommandContext<CommandSourceStack> ctx, PlayerPacketSender sender) {
        String name = StringArgumentType.getString(ctx, "player");
        CommandSourceStack source = ctx.getSource();

        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(name);
        if (online != null) {
            RelayPackets.unbanPlayer(online.getUUID());
            source.sendSuccess(() -> Component.literal("Unbanned " + name + " from Figura relay"), false);
            return 1;
        }

        var profile = source.getServer().getProfileCache().get(name);
        if (profile.isPresent()) {
            RelayPackets.unbanPlayer(profile.get().getId());
            source.sendSuccess(() -> Component.literal("Unbanned " + name + " (offline) from Figura relay"), false);
            return 1;
        }

        source.sendFailure(Component.literal("Player not found: " + name));
        return 0;
    }

    private static int reloadConfig(CommandContext<CommandSourceStack> ctx) {
        RelayConfig.load();
        BanStore.load();
        ctx.getSource().sendSuccess(() -> Component.literal("Figura relay config reloaded"), false);
        return 1;
    }

    private static int listBanned(CommandContext<CommandSourceStack> ctx) {
        var banned = BanStore.getBanned();
        if (banned.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("No banned players"), false);
            return 0;
        }
        StringBuilder sb = new StringBuilder("Banned from relay: ");
        for (UUID uuid : banned) {
            var prof = ctx.getSource().getServer().getProfileCache().get(uuid);
            String profName = prof.isPresent() ? prof.get().getName() : uuid.toString();
            sb.append(profName).append(", ");
        }
        String result = sb.substring(0, sb.length() - 2);
        ctx.getSource().sendSuccess(() -> Component.literal(result), false);
        return banned.size();
    }
}
