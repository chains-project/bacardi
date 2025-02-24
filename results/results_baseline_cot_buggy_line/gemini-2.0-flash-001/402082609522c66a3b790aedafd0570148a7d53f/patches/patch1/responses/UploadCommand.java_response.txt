package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;

import java.util.List;
import java.util.Optional;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

public class UploadCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandContext args) {
        Optional<String> urlOptional = args.one(Parameter.string().key("url").build());

        if (!urlOptional.isPresent()) {
            plugin.sendMessage(args.cause().getNotifier(), "no-valid-url");
            return CommandResult.empty();
        }

        String url = urlOptional.get();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(args.cause().getNotifier(), "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, args.cause().getNotifier(), uploadAccount, url);
                Task.builder().async().execute(skinUploader).submit(plugin);
            }
        } else {
            plugin.sendMessage(args.cause().getNotifier(), "no-valid-url");
        }

        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        Parameter.Value<String> urlParameter = Parameter.string().key("url").build();

        return CommandSpec.builder()
                .executor(this)
                .addParameter(urlParameter)
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
    }
}