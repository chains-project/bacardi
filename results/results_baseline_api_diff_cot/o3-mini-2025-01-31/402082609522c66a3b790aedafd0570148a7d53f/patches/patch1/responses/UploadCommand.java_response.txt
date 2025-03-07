package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;

public class UploadCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    public UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    // Updated to follow the new Sponge command API:
    // The new CommandExecutor method now receives a single CommandContext parameter
    // and returns an int (with a nonzero value typically indicating success).
    @Override
    public int execute(CommandContext context) throws Exception {
        // Retrieve the sender from the command execution cause.
        Audience src = context.cause().first(Audience.class)
                .orElseThrow(() -> new Exception("Command source not found"));
        String url = context.one("url").get();

        if (url.startsWith("http://") || url.startsWith("https://")) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(src, "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, src, uploadAccount, url);
                // In the new API the old Task builder has been removed.
                // Instead, we schedule the task asynchronously via the scheduler’s executor.
                plugin.getGame().scheduler().executor(plugin).submit(skinUploader);
            }
        } else {
            plugin.sendMessage(src, "no-valid-url");
        }
        // Return 1 to indicate successful command execution.
        return 1;
    }

    // Updated to build a Command using the new API.
    // Note: The return type has been changed from the now‐removed CommandSpec to Command.
    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key("url").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
    }
}