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
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class SetCommand implements Command.Executor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandCause cause, CommandContext args) {
        if (!(cause.root() instanceof Player)) {
            plugin.sendMessage(cause, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) cause.root()).getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(cause, "cooldown");
            return CommandResult.empty();
        }

        Player receiver = (Player) cause.root();

        Optional<String> skinOptional = args.one(Parameter.string().key("skin").build());
        if (!skinOptional.isPresent()) {
            plugin.sendMessage(cause, "invalid-skin");
            return CommandResult.empty();
        }

        String targetSkin = skinOptional.get();

        boolean keepSkin = args.hasAny("keep");

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID = UUID.fromString(targetSkin);

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(cause, targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(cause, "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, cause, receiver, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return CommandResult.success();
        }

        Runnable nameResolver = new NameResolver(plugin, cause, targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key("skin").build())
                .addFlag(org.spongepowered.api.command.parameter.Flag.builder().alias("keep").build())
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base");
    }
}