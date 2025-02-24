package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import java.util.Optional;

public class InvalidateCommand implements ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    public InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    // This method is kept intact from the original client code.
    // Note: The @Override annotation has been removed because it no longer overrides any method in the new dependency.
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        Player receiver = (Player) src;
        Task.builder().async().execute(new SkinInvalidator(plugin, receiver)).submit(plugin);
        return CommandResult.success();
    }

    // New method that conforms to the new Sponge command API executor,
    // extracting the CommandSource from the CommandContext and delegating to the existing execute method.
    public CommandResult process(CommandContext args) {
        Optional<CommandSource> sourceOpt = args.cause().first(CommandSource.class);
        if (!sourceOpt.isPresent()) {
            return CommandResult.empty();
        }
        return execute(sourceOpt.get(), args);
    }

    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(this::process)
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                .build();
    }
}