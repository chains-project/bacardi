package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandContext;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import static org.spongepowered.api.command.parameter.CommandParameters.string;
import static net.kyori.adventure.text.Component.text;

/**
 * NOTE: This patch adapts the old Sponge 7 command API to the new Sponge 8 API.
 * 
 * Changes made:
 * 1. Updated the import for CommandSource to: org.spongepowered.api.command.source.CommandSource
 * 2. Removed the old CommandExecutor and CommandSpec (from package org.spongepowered.api.command.spec)
 *    and now use the new Command class from org.spongepowered.api.command.
 * 3. Updated the command argument API by replacing the GenericArguments.string(of("skinName"))
 *    with CommandParameters.string(text("skinName")).
 * 4. Updated the text creation call by replacing Text.of(..) with net.kyori.adventure.text.Component.text(..).
 * 5. Removed CommandExecutor from the implemented interfaces since the new API does not use it;
 *    the execute method is now used as a method reference for the command executor.
 */
public class SelectCommand implements ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    public SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    // Removed @Override because this method no longer implements the old CommandExecutor interface.
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

    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(this::execute)
                .addParameter(string(text("skinName")))
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }
}