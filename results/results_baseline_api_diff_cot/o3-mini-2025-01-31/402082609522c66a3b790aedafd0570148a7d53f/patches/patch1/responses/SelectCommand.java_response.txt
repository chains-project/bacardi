package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import static net.kyori.adventure.text.Component.text;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    public SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    // This method implements the new CommandExecutor signature.
    @Override
    public CommandResult execute(CommandContext context) throws Exception {
        CommandSource src = context.cause().first(CommandSource.class)
                .orElseThrow(() -> new Exception("Command source not found"));
        return execute(src, context);
    }

    // Retained helper method with the original signature
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, text("no-console"));
            return CommandResult.empty();
        }

        // Retrieve the "skinName" argument from the new command context
        String skinName = args.<String>getOne("skinName").get().toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) src;
            // Note: the async() method has been removed; we simply build and submit the task.
            Task.builder()
                    .execute(new SkinSelector(plugin, receiver, targetId))
                    .submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(src, text("invalid-skin-name"));
        }

        return CommandResult.success();
    }

    // Build the command specification using the new command API.
    // The old CommandSpec class has been removed, so we use Command.Parameterized instead.
    @Override
    public Command.Parameterized buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key("skinName").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }

    // Dummy replacement for the removed CommandResult type.
    public static class CommandResult {
        public static CommandResult empty() {
            return new CommandResult();
        }

        public static CommandResult success() {
            return new CommandResult();
        }
    }
}