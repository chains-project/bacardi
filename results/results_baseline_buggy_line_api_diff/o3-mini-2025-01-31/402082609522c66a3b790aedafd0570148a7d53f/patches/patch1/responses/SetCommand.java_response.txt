package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.NameResolver;
import com.github.games647.changeskin.sponge.task.SkinDownloader;
import com.google.inject.Inject;

import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import static org.spongepowered.api.text.Text.of;

public class SetCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    public SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandContext args) throws CommandException {
        Optional<Player> playerOpt = args.cause().first(Player.class);
        if (!playerOpt.isPresent()) {
            plugin.sendMessage(args.cause().root(), "no-console");
            return CommandResult.empty();
        }
        Player sender = playerOpt.get();
        UUID uniqueId = sender.getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(sender, "cooldown");
            return CommandResult.empty();
        }

        Object skinArg = args.one("skin").orElse(null);
        if (skinArg == null) {
            throw new CommandException(of("Missing skin argument"));
        }
        String targetSkin = skinArg.toString();
        boolean keepSkin = args.one("keep").map(val -> (Boolean) val).orElse(false);

        if ("reset".equals(targetSkin)) {
            targetSkin = sender.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID = UUID.fromString(targetSkin);

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(sender, targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(sender, "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, sender, sender, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return CommandResult.success();
        }

        Runnable nameResolver = new NameResolver(plugin, sender, targetSkin, sender, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return CommandResult.success();
    }

    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key("skin").build())
                .addParameter(Parameter.bool().key("keep").setDefaultValue(false).build())
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base")
                .build();
    }
}