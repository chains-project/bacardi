package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Scheduler;
import org.spongepowered.api.text.Text;

public class InvalidateCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    public InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext args) throws CommandException {
        CommandSource src = args.cause().first(CommandSource.class)
                .orElseThrow(() -> new CommandException(Text.of("No command source provided.")));
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.builder().build();
        }

        Player receiver = (Player) src;
        Scheduler scheduler = Sponge.getScheduler();
        scheduler.executor(plugin).execute(new SkinInvalidator(plugin, receiver));
        return CommandResult.success();
    }

    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                .build();
    }
}