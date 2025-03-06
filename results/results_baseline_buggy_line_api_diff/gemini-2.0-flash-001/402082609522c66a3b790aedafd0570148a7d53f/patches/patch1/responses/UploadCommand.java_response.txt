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
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import net.kyori.adventure.text.TextComponent;
import org.spongepowered.api.command.parameter.Parameter;

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
        String url = args.<String>getOne(Parameter.string().key("url").build()).orElse(null);
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(args.getCause().get(Player.class).orElse(null), "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, args.getCause().get(Player.class).orElse(null), uploadAccount, url);
                plugin.getServer().getScheduler().submit(Task.builder().plugin(plugin).execute(skinUploader).build());
            }
        } else {
            plugin.sendMessage(args.getCause().get(Player.class).orElse(null), "no-valid-url");
        }

        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
        Parameter.Value<String> urlParameter = Parameter.string().key("url").build();

        return Command.builder()
                .addParameter(urlParameter)
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base");
    }
}