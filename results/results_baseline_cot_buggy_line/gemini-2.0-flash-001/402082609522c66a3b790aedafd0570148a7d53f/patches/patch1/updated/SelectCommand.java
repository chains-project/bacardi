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

import com.google.common.collect.ImmutableList;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.CommandSpec;
import org.spongepowered.api.text.Text;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
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
    public CommandSpec buildSpec() {
        Parameter<String> skinNameParameter = Parameter.string().key("skinName").build();

        return CommandSpec.builder()
                .executor(this)
                .arguments(skinNameParameter)
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }
}