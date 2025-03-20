package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.model.UserPreference;
import com.github.games647.changeskin.core.model.skin.SkinModel;
import com.github.games647.changeskin.core.shared.SkinFormatter;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.google.inject.Inject;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;

public class InfoCommand implements ChangeSkinCommand {

    @Inject
    private ChangeSkinSponge plugin;

    @Inject
    private SkinFormatter formatter;

    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) src).getUniqueId();
        Task.builder().async()
                .execute(() -> {
                    UserPreference preferences = plugin.getCore().getStorage().getPreferences(uniqueId);
                    Task.builder().execute(() -> sendSkinDetails(uniqueId, preferences)).submit(plugin);
                })
                .submit(plugin);

        return CommandResult.success();
    }
    
    public CommandResult process(CommandContext context) throws CommandException {
        Optional<CommandSource> optionalSource = context.cause().first(CommandSource.class);
        if (optionalSource.isEmpty()) {
            throw new CommandException(LegacyComponentSerializer.legacyAmpersand().deserialize("&cNo valid command source!"));
        }
        CommandSource src = optionalSource.get();
        return execute(src, context);
    }

    public CommandSpec buildSpec() {
        Command command = Command.builder()
                .executor(ctx -> this.process(ctx))
                .requires(src -> src.hasPermission(PomData.ARTIFACT_ID + ".command.skininfo.base"))
                .build();
        return new CommandSpec(command);
    }

    private void sendSkinDetails(UUID uuid, UserPreference preference) {
        Optional<Player> optPlayer = Sponge.getServer().getPlayer(uuid);
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();

            Optional<SkinModel> optSkin = preference.getTargetSkin();
            if (optSkin.isPresent()) {
                String template = plugin.getCore().getMessage("skin-info");
                String formatted = formatter.apply(template, optSkin.get());

                Component text = LegacyComponentSerializer.legacyAmpersand().deserialize(formatted);
                player.sendMessage(text);
            } else {
                plugin.sendMessage(player, "skin-not-found");
            }
        }
    }

    public static class CommandSpec {
        private final Command command;

        public CommandSpec(Command command) {
            this.command = command;
        }

        public Command getCommand() {
            return command;
        }
    }
}