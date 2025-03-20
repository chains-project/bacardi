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

import static org.spongepowered.api.command.CommandResult.success;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.Entity;

import java.util.Optional;

import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.command.CommandExecutor;

public class SetCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandContext args) throws CommandException {
        CommandCause cause = args.cause();
        Object source = cause.root();

        if (!(source instanceof Player)) {
            plugin.sendMessage(source, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) source).uniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(source, "cooldown");
            return CommandResult.empty();
        }

        Player receiver = (Player) source;
        Optional<String> targetSkinOptional = args.one(CommonParameters.STRING);

        if (!targetSkinOptional.isPresent()) {
             plugin.sendMessage(source, "invalid-usage");
             return CommandResult.empty();
        }

        String targetSkin = targetSkinOptional.get();
        boolean keepSkin = args.hasAny("keep");

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.uniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID = UUID.fromString(targetSkin);

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(source, targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(source, "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, source, receiver, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return success();
        }

        Runnable nameResolver = new NameResolver(plugin, source, targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return success();
    }

    @Override
    public Command.Builder buildSpec() {
        Parameter.Value<String> skinParameter = Parameter.string().key("skin").build();

        Flag keepFlag = Flag.builder().alias("keep").build();

        return Command.builder()
                .executor(this)
                .addParameter(skinParameter)
                .addFlag(keepFlag)
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base");
    }
}