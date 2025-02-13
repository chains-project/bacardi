package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.executor.CommandExecutor;
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

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        String skinName = args.<String>getOne("skinName").get().toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) src;
            Task.builder().async().execute(new SkinSelector(plugin, receiver, targetId)).submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(src, "invalid-skin-name");
        }

        return CommandResult.success();
    }

    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string(text("skinName")).key("skinName").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }
}