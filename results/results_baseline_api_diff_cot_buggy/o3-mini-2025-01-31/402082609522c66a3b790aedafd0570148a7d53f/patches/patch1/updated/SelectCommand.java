package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private static final Parameter.Key<String> SKIN_NAME_KEY = Parameter.key("skinName", String.class);

    @Inject
    public SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public int execute(CommandContext context) {
        CommandSource src = context.cause().first(CommandSource.class).orElse(null);
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return 0;
        }

        String skinName = context.requireOne(SKIN_NAME_KEY).toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) src;
            Task.builder().execute(new SkinSelector(plugin, receiver, targetId)).submit(plugin);
        } catch (NumberFormatException e) {
            plugin.sendMessage(src, "invalid-skin-name");
        }

        return 1;
    }

    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key(SKIN_NAME_KEY).build())
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }
}
