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
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import java.util.Optional;

public class SelectCommand implements Command, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(org.spongepowered.api.command.CommandCause cause, CommandContext args) throws CommandException {
        org.spongepowered.api.command.CommandSource src = cause.cause().first(org.spongepowered.api.command.CommandSource.class).orElse(null);
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        Parameter.Key<String> skinNameKey = Parameter.key("skinName", String.class);
        Optional<String> skinNameOptional = args.one(skinNameKey);

        if (!skinNameOptional.isPresent()) {
            plugin.sendMessage(src, "invalid-skin-name");
            return CommandResult.empty();
        }

        String skinName = skinNameOptional.get().toLowerCase().replace("skin-", "");

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
    public org.spongepowered.api.command.Command.Builder build() {
        Parameter skinNameParameter = Parameter.string().key("skinName").build();

        return org.spongepowered.api.command.Command.builder()
                .executor(this)
                .addParameter(skinNameParameter)
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base");
    }

    @Override
    public String getShortDescription() {
        return "Selects a skin by ID.";
    }
}