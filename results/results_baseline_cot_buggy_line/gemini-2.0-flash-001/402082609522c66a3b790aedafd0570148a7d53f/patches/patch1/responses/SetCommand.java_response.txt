package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.NameResolver;
import com.github.games647.changeskin.sponge.task.SkinDownloader;
import com.google.inject.Inject;

import java.util.UUID;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommonParameters;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.CommandCause;

import java.util.Optional;
import org.spongepowered.api.text.Text;

public class SetCommand implements Command.Executor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandCause cause, CommandContext args) throws CommandException {
        if (!(cause.root() instanceof Player)) {
            plugin.sendMessage(cause.audience(), "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) cause.root()).getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(cause.audience(), "cooldown");
            return CommandResult.empty();
        }

        Player receiver = (Player) cause.root();
        Optional<String> targetSkinOptional = args.getOne(CommonParameters.STRING);

        if (!targetSkinOptional.isPresent()) {
            plugin.sendMessage(cause.audience(), "invalid-usage");
            return CommandResult.empty();
        }

        String targetSkin = targetSkinOptional.get();
        boolean keepSkin = args.hasAny("keep");

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID = UUID.fromString(targetSkin);

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(cause.audience(), targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(cause.audience(), "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, cause.audience(), receiver, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return CommandResult.success();
        }

        Runnable nameResolver = new NameResolver(plugin, cause.audience(), targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
         Parameter.Value<String> skinParameter = Parameter.string().key(CommonParameters.STRING).build();

        return Command.builder()
                .executor(this)
                .addParameter(skinParameter)
                .addFlag(org.spongepowered.api.command.flag.Flag.of("keep", "k"))
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base");
    }
}