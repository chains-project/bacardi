package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.command.CommandSource;

public class InvalidateCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext context) {
        CommandSource src = context.getCause().get(CommandSource.class).orElse(null);
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        Player receiver = (Player) src;
        Task.builder().async().execute(new SkinInvalidator(plugin, receiver)).submit(plugin);
        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                .build();
    }

    @Override
    public CommandResult execute(org.spongepowered.api.command.CommandSource src, org.spongepowered.api.command.args.CommandContext args) throws org.spongepowered.api.command.CommandException {
        return null;
    }
}