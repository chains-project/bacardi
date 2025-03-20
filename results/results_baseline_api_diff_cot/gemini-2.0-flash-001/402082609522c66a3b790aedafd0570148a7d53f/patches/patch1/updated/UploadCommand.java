package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;

import java.util.List;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.scheduler.Task;

import java.util.Optional;

public class UploadCommand implements org.spongepowered.api.command.CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public CommandResult execute(CommandSource src, CommandContext args) {
        Parameter.Key<String> urlKey = Parameter.key("url", String.class);
        Optional<String> urlOptional = args.one(urlKey);

        if (!urlOptional.isPresent()) {
            plugin.sendMessage(src, "no-valid-url");
            return CommandResult.empty();
        }

        String url = urlOptional.get();

        if (url.startsWith("http://") || url.startsWith("https://")) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(src, "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, src, uploadAccount, url);
                Task.builder().plugin(plugin).async().execute(skinUploader).submit();
            }
        } else {
            plugin.sendMessage(src, "no-valid-url");
        }

        return CommandResult.success();
    }

    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(Parameter.string().key("url").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
    }
}