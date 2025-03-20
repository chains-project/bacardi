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
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

import net.kyori.adventure.text.TextComponent;

import static org.spongepowered.api.command.parameter.CommonParameters.string;
import static net.kyori.adventure.text.Component.text;

public class UploadCommand implements Command.Executor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandContext args) {
        Parameter.Value<String> urlValue = args.one(string());
        if (!urlValue.isPresent()) {
            return CommandResult.success();
        }

        String url = urlValue.get();
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                args.sendMessage(text("no-accounts"));
            } else {
                Account uploadAccount = accounts.get(0);
                Object source = args.getCause().root();

                if (source instanceof Player) {
                    Player src = (Player) source;
                    Runnable skinUploader = new SkinUploader(plugin, src, uploadAccount, url);
                    Task.builder().async().execute(skinUploader).submit(plugin);
                } else {
                    args.sendMessage(text("This command can only be executed by a player."));
                }
            }
        } else {
            args.sendMessage(text("no-valid-url"));
        }

        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
        return Command.builder()
                .executor(this)
                .addParameter(string())
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base");
    }
}