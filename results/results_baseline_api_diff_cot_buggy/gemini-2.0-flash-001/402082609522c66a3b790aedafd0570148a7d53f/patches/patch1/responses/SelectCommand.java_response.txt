package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandCause;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandCause cause, CommandContext args) throws CommandException {
        if (!(cause.cause().root() instanceof Player)) {
            plugin.sendMessage(cause, "no-console");
            return CommandResult.empty();
        }

        String skinName = args.<String>one("skinName").get().toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) cause.cause().root();
            Task.builder().async().execute(new SkinSelector(plugin, receiver, targetId)).submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(cause, "invalid-skin-name");
            return CommandResult.failure();
        }

        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
        Parameter skinName = Parameter.key("skinName").type(String.class).build();

        return Command.builder()
                .addParameter(skinName)
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base");
    }
}
