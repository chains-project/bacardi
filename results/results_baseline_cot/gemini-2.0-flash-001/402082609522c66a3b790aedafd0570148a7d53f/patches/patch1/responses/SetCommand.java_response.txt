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
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommonParameters;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.CommandCause;

import static org.spongepowered.api.util.Identifiable.name;
import static org.spongepowered.api.command.CommandResult.success;
import static org.spongepowered.api.command.CommandResult.empty;

public class SetCommand implements Command, ChangeSkinCommand {

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
            plugin.sendMessage(cause, "no-console");
            return empty();
        }

        UUID uniqueId = ((Player) cause.root()).getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(cause, "cooldown");
            return empty();
        }

        Player receiver = (Player) cause.root();
        Optional<String> targetSkinOptional = args.getOne(CommonParameters.STRING);
        if (!targetSkinOptional.isPresent()) {
            plugin.sendMessage(cause, "no-skin-specified");
            return empty();
        }

        String targetSkin = targetSkinOptional.get();
        boolean keepSkin = args.hasAny("keep");

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            try {
                UUID targetUUID = UUID.fromString(targetSkin);

                if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(cause, targetUUID, true)) {
                    return empty();
                }

                plugin.sendMessage(cause, "skin-change-queue");
                Runnable skinDownloader = new SkinDownloader(plugin, cause, receiver, targetUUID, keepSkin);
                Task.builder().async().execute(skinDownloader).submit(plugin);
                return success();
            } catch (IllegalArgumentException ex) {
                plugin.sendMessage(cause, "invalid-uuid");
                return empty();
            }
        }

        Runnable nameResolver = new NameResolver(plugin, cause, targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return success();
    }

    @Override
    public org.spongepowered.api.command.Command.Builder build() {
        Parameter.Value<String> skinParameter = Parameter.string().key(CommonParameters.STRING).build();

        return org.spongepowered.api.command.Command.builder()
                .executor(this)
                .addParameter(skinParameter)
                .addFlag(org.spongepowered.api.command.parameter.Flag.builder().alias("k").alias("keep").build())
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base");
    }
}