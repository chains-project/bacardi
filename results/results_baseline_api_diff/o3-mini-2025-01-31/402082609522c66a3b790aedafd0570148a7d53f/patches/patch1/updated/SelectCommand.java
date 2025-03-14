package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

public class SelectCommand implements ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    private static final String SKIN_NAME_KEY = "skinName";

    @Inject
    public SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            // CommandResult.empty() has been removed; using success() for no-op.
            return CommandResult.success();
        }

        String skinName = args.one(SKIN_NAME_KEY).get().toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) src;
            // Removed .async() call since Task.builder().async() is no longer available.
            Task.builder().execute(new SkinSelector(plugin, receiver, targetId)).submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(src, "invalid-skin-name");
        }

        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        Command<CommandSource> command = Command.builder()
                .addParameter(Parameter.string().key(SKIN_NAME_KEY).build())
                .executor(this::execute)
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
        return new CommandSpec(command);
    }

    // Local shim to replace removed org.spongepowered.api.command.spec.CommandSpec
    public static class CommandSpec {
        private final Command<CommandSource> command;

        public CommandSpec(Command<CommandSource> command) {
            this.command = command;
        }

        public Command<CommandSource> getCommand() {
            return command;
        }
    }
}