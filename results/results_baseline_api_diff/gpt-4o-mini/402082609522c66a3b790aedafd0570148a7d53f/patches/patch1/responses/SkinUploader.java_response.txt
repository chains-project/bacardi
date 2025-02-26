package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedUploader;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import java.util.concurrent.TimeUnit;

import org.spongepowered.api.command.CommandSource; // This import will be removed
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.command.source.CommandSource; // New import for the updated API

public class SkinUploader extends SharedUploader {

    private final ChangeSkinSponge plugin;
    private final CommandSource invoker;

    public SkinUploader(ChangeSkinSponge plugin, CommandSource invoker, Account owner, String url) {
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