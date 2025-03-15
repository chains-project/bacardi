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
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.CommandParameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.text.Text;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext args) {
        if (!(args.cause().root() instanceof Player)) {
            plugin.sendMessage(args.cause().root(), "no-console");
            return CommandResult.empty();
        }

        String skinName = args.one(Parameter.string().key("skinName").get()).get().toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) args.cause().root();
            Task.builder().async().execute(new SkinSelector(plugin, receiver, targetId)).submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(args.cause().root(), "invalid-skin-name");
        }

        return CommandResult.success();
    }

    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key("skinName").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }
}