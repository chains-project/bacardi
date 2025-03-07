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
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandSpec;
import org.spongepowered.api.command.parameter.GenericArguments;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import static org.spongepowered.api.command.parameter.CommonParameters.string;
import static org.spongepowered.api.command.parameter.GenericArguments.flags;
import static org.spongepowered.api.text.Text.of;

public class SetCommand implements CommandExecutor {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandCause cause, CommandContext args) {
        if (!(cause.cause().root() instanceof Player)) {
            plugin.sendMessage(cause, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) cause.cause().root()).getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(cause, "cooldown");
            return CommandResult.empty();
        }

        Player receiver = (Player) cause.cause().root();
        String targetSkin = args.<String>one(Text.of("skin")).get();
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

    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .addParameter(
                        string(Text.of("skin")))
                .addParameter(flags().flag("keep").buildWith(GenericArguments.none()))
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base")
                .build();
    }
}