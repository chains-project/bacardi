package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    public SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandContext context) {
        CommandSource src = context.cause().first(CommandSource.class).orElse(null);
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        String skinName = context.requireOne("skinName").toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) src;
            Task.builder()
                    .async()
                    .execute(new SkinSelector(plugin, receiver, targetId))
                    .submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(src, "invalid-skin-name");
        }

        return CommandResult.success();
    }

    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key(Component.text("skinName")).build())
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }
}