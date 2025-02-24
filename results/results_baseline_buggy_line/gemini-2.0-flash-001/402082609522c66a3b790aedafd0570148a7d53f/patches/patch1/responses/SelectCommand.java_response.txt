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
import org.spongepowered.api.text.Text;

import java.util.Collections;

public class SelectCommand implements ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    public CommandResult execute(org.spongepowered.api.command.CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        Parameter.Value<String> skinNameValue = args.one(Parameter.string().key("skinName").build());
        if (!skinNameValue.isPresent()) {
            plugin.sendMessage(src, "invalid-skin-name");
            return CommandResult.empty();
        }

        String skinName = skinNameValue.get().toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) src;
            Task.builder().async().execute(new SkinSelector(plugin, receiver, targetId)).submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(src, "invalid-skin-name");
        }

        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
        return Command.builder()
                .executor(this::execute)
                .addParameter(Parameter.string().key("skinName").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base");
    }
}