package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;

import java.util.List;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.scheduler.Task;

import net.kyori.adventure.text.Component;

import static org.spongepowered.api.command.parameter.Parameter.key;

public class UploadCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public org.spongepowered.api.command.result.CommandResult execute(CommandCause src, CommandContext args) {
        String url = args.one(Parameter.<String>key(Component.text("url"), String.class)).orElse(null);
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(src, "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, src, uploadAccount, url);
                Task.builder().execute(skinUploader).submit(plugin);
            }
        } else {
            plugin.sendMessage(src, "no-valid-url");
        }

        return org.spongepowered.api.command.result.CommandResult.success();
    }

    @Override
    public Command buildSpec() {
        return Command.builder()
                .executor(cause -> this.execute(cause, (CommandContext) cause.context()))
                .addParameter(Parameter.string().key(Component.text("url")).build())
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
    }
}
