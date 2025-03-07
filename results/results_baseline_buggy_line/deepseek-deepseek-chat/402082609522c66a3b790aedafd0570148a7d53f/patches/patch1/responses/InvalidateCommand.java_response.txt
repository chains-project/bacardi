package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.command.manager.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

public class InvalidateCommand implements CommandSpec, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    public CommandResult execute(CommandCause src, CommandContext args) {
        if (!(src.subject() instanceof Player)) {
            plugin.sendMessage(src.subject(), "no-console");
            return CommandResult.empty();
        }

        Player receiver = (Player) src.subject();
        Task.builder().async().execute(new SkinInvalidator(plugin, receiver)).submit(plugin);
        return CommandResult.success();
    }

    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                .build();
    }
}