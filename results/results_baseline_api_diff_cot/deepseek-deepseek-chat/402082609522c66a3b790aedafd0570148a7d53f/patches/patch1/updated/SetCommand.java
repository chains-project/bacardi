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
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.Parameter.Value;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import static org.spongepowered.api.command.parameter.Parameter.string;
import static org.spongepowered.api.text.Text.of;

public class SetCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandContext args) {
        if (!(args.cause().root() instanceof Player)) {
            plugin.sendMessage(args.cause().root(), "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) args.cause().root()).getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(args.cause().root(), "cooldown");
            return CommandResult.empty();
        }

        Player receiver = (Player) args.cause().root();
        String targetSkin = args.one(Parameter.string("skin")).get();
        boolean keepSkin = args.hasAny(Parameter.bool("keep"));

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID = UUID.fromString(targetSkin);

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(args.cause().root(), targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(args.cause().root(), "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, args.cause().root(), receiver, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return CommandResult.success();
        }

        Runnable nameResolver = new NameResolver(plugin, args.cause().root(), targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .parameters(
                        Parameter.string("skin"),
                        Parameter.bool("keep"))
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base")
                .build();
    }
}