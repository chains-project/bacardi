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

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.command.CommandCause;

import java.util.Optional;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.Component;

public class SetCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        CommandCause src = context.cause();

        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) src).getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(src, "cooldown");
            return CommandResult.empty();
        }

        Player receiver = (Player) src;
        Optional<String> targetSkinOptional = context.one(CommonParameters.STRING);
        if (!targetSkinOptional.isPresent()) {
            plugin.sendMessage(src, "invalid-usage");
            return CommandResult.empty();
        }

        String targetSkin = targetSkinOptional.get();
        boolean keepSkin = context.hasFlag("keep");

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID;
            try {
                targetUUID = UUID.fromString(targetSkin);
            } catch (IllegalArgumentException ex) {
                plugin.sendMessage(src, "uuid-invalid");
                return CommandResult.empty();
            }

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(src, targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(src, "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, src, receiver, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return CommandResult.success();
        }

        Runnable nameResolver = new NameResolver(plugin, src, targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
        Parameter.Key<String> skinKey = Parameter.key("skin", String.class);

        return Command.builder()
                .executor(this)
                .addParameter(CommonParameters.STRING.key(skinKey))
                .addFlag(org.spongepowered.api.command.parameter.Flag.builder().alias("keep").key("keep").build())
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base");
    }
}