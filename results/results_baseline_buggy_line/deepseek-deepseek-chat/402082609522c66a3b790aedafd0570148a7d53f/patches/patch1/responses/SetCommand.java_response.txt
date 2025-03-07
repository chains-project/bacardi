package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.NameResolver;
import com.github.games647.changeskin.sponge.task.SkinDownloader;
import com.google.inject.Inject;

import java.util.UUID;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.command.parameter.managed.client.ClientCommandCompletionTypes;
import org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import static org.spongepowered.api.command.parameter.managed.standard.VariableValueParameters.string;

public class SetCommand implements ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public CommandResult execute(CommandContext args) {
        if (!(args.getCause().getAudience() instanceof Player)) {
            plugin.sendMessage(args.getCause().getAudience(), "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) args.getCause().getAudience()).getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(args.getCause().getAudience(), "cooldown");
            return CommandResult.empty();
        }

        Player receiver = (Player) args.getCause().getAudience();
        String targetSkin = args.requireOne(Parameter.string().key("skin").build());
        boolean keepSkin = args.hasFlag(Flag.builder("keep").build());

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID = UUID.fromString(targetSkin);

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(args.getCause().getAudience(), targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(args.getCause().getAudience(), "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, args.getCause().getAudience(), receiver, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return CommandResult.success();
        }

        Runnable nameResolver = new NameResolver(plugin, args.getCause().getAudience(), targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return CommandResult.success();
    }

    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .parameters(
                        Parameter.string().key("skin").build(),
                        Flag.builder("keep").build())
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base")
                .build();
    }
}