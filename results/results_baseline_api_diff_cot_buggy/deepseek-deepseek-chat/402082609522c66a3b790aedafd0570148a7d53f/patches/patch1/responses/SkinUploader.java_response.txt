package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedUploader;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import java.util.concurrent.TimeUnit;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.source.RemoteSource;
import org.spongepowered.api.command.source.CommandBlockSource;
import org.spongepowered.api.command.source.ProxySource;
import org.spongepowered.api.command.source.RconSource;
import org.spongepowered.api.command.source.SignSource;
import org.spongepowered.api.command.source.User;
import org.spongepowered.api.scheduler.Task;

public class SkinUploader extends SharedUploader {

    private final ChangeSkinSponge plugin;
    private final User invoker;

    public SkinUploader(ChangeSkinSponge plugin, User invoker, Account owner, String url) {
        super(plugin.getCore(), owner, url);

        this.plugin = plugin;
        this.invoker = invoker;
    }

    @Override
    public void sendMessageInvoker(String key) {
        plugin.sendMessage(invoker, key);
    }

    @Override
    protected void scheduleChangeTask(String oldSkinUrl) {
        Runnable task = new SkinChanger(plugin, owner, url, oldSkinUrl, invoker);
        Task.builder().delay(1, TimeUnit.MINUTES).execute(task).async().submit(plugin);
    }
}
