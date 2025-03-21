package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;

import java.util.List;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.Command;
import org.spongepowered.api.scheduler.TaskExecutorService;
import org.spongepowered.api.command.source.CommandSource;

import static org.spongepowered.api.command.parameter.Parameter.string;
import static org.spongepowered.api.text.Text.of;

public class UploadCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public CommandResult execute(CommandSource src, CommandContext args) {
        String url = args.one("url").orElse("");
        if (url.startsWith("http://") || url.startsWith("https://")) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(src, "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, src, uploadAccount, url);
                TaskExecutorService scheduler = plugin.getScheduler();
                scheduler.submit(skinUploader);
            }
        } else {
            plugin.sendMessage(src, "no-valid-url");
        }

        return CommandResult.success();
    }

    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key("url").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
    }
}
