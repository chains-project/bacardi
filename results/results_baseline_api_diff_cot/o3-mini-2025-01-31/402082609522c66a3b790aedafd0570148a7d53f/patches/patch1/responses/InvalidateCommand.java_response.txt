package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.entity.living.player.Player;

public class InvalidateCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    public InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public int execute(CommandContext context) throws CommandException {
        // Retrieve the root command source from the new command context API.
        CommandSource source = context.cause().first(CommandSource.class)
                .orElseThrow(() -> new CommandException("Missing command source"));
        if (!(source instanceof Player)) {
            plugin.sendMessage(source, "no-console");
            return 0;
        }

        Player receiver = (Player) source;
        // Schedule the skin invalidation task asynchronously using the new scheduler API.
        // Note: plugin.getGame().getScheduler().executor(plugin.getContainer()) returns an executor
        // on which we call execute(Runnable) to schedule our task.
        plugin.getGame().getScheduler().executor(plugin.getContainer())
                .execute(new SkinInvalidator(plugin, receiver));
        return 1;
    }

    // Build and return the command using the new command API.
    // The old CommandSpec has been replaced by org.spongepowered.api.command.Command.
    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                .build();
    }
}