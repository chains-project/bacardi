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
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

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
        Optional<Player> playerOpt = args.cause().first(Player.class);
        if (!playerOpt.isPresent()) {
            plugin.sendMessage(args.cause().root(), "no-console");
            return CommandResult.empty();
        }

        Player receiver = playerOpt.get();
        UUID uniqueId = receiver.getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(receiver, "cooldown");
            return CommandResult.empty();
        }

        String targetSkin = args.one("skin").get();
        boolean keepSkin = args.<Boolean>one("keep").orElse(false);

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID = UUID.fromString(targetSkin);

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(receiver, targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(receiver, "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, receiver, receiver, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return CommandResult.success();
        }

        Runnable nameResolver = new NameResolver(plugin, receiver, targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return CommandResult.success();
    }

    public Command.Parameterized buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key("skin").build())
                .addParameter(Parameter.bool().key("keep").optional().build())
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base")
                .build();
    }
}