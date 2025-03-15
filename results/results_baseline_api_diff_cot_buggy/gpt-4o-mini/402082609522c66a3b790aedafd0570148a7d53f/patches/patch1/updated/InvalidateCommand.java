package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

public class InvalidateCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        Player receiver = (Player) src;
        Task.builder().execute(new SkinInvalidator(plugin, receiver)).submit(plugin);
        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
        return Command.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base");
    }
}
