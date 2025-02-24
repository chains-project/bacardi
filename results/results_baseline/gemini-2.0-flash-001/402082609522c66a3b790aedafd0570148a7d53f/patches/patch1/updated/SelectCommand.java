package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.command.parameter.Parameter;

import java.util.Optional;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(org.spongepowered.api.command.CommandCause cause, CommandContext args) {
        if (!(cause.cause().root() instanceof Player)) {
            plugin.sendMessage(cause, "no-console");
            return CommandResult.empty();
        }

        Optional<String> skinNameOptional = args.one(Parameter.string().key("skinName").build());
        if (!skinNameOptional.isPresent()) {
            plugin.sendMessage(cause, "invalid-skin-name");
            return CommandResult.empty();
        }

        String skinName = skinNameOptional.get().toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) cause.cause().root();
            Task.builder().async().execute(new SkinSelector(plugin, receiver, targetId)).submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(cause, "invalid-skin-name");
        }

        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .addParameter(Parameter.string().key("skinName").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }
}