package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.Parameterized;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext args) {
        Player src = args.getCause().first(Player.class).orElse(null);
        if (src == null) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        String skinName = args.one(Parameter.string().key("skinName").build()).orElse("").toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Task.builder().execute(new SkinSelector(plugin, src, targetId)).submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(src, "invalid-skin-name");
        }

        return CommandResult.success();
    }

    @Override
    public Parameterized buildSpec() {
        return Parameterized.builder()
                .addParameter(Parameter.string().key("skinName").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .executor(this)
                .build();
    }
}
