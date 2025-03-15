package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;
import java.util.function.Predicate;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.Command.Raw;
import org.spongepowered.api.entity.living.Agent;
import org.spongepowered.api.event.Cause;

public class InvalidateCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        Cause cause = context.cause();
        CommandCause cmdCause = cause.first(CommandCause.class).orElse(null);

        if (cmdCause == null) {
            plugin.sendMessage(cmdCause, "no-console");
            return CommandResult.success();
        }

        Subject src = cmdCause.subject();

        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.success();
        }

        Player receiver = (Player) src;
        Sponge.asyncScheduler().submit(Task.builder().execute(new SkinInvalidator(plugin, receiver)).plugin(plugin).build());
        return CommandResult.success();
    }

    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                .build();
    }
}
