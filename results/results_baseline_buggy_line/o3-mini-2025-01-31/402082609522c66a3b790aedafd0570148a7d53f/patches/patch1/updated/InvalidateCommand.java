package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

public class InvalidateCommand implements ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        Player receiver = (Player) src;
        Task.builder().async().execute(new SkinInvalidator(plugin, receiver)).submit(plugin);
        return CommandResult.success();
    }

    public Command buildSpec() {
        return Command.builder()
                .executor(context -> {
                    CommandSource src = context.cause().first(CommandSource.class)
                            .orElseThrow(() -> new CommandException(Text.of("No command source")));
                    return this.execute(src, context);
                })
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                .build();
    }
}