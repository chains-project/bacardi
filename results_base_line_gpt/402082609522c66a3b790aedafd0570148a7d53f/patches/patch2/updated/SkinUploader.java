package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedUploader;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import java.util.concurrent.TimeUnit;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.scheduler.Task;

// Updated import statement
import org.spongepowered.api.command.source.CommandSource; // Ensure this import is correct based on the new dependency structure

public class SkinUploader extends SharedUploader {

    private final ChangeSkinSponge plugin;
    private final CommandSource invoker;

    public SkinUploader(ChangeSkinSponge plugin, CommandSource invoker, Account owner, String url) {
        super(plugin.getCore(), owner, url);

        this.plugin = plugin;
        this.invoker = invoker;
    }

    public void sendMessageInvoker(String key) {
        plugin.sendMessage(invoker, key);
    }

    protected void scheduleChangeTask(String oldSkinUrl) {
        Runnable task = new SkinChanger(plugin, owner, url, oldSkinUrl, invoker);
        Task.builder().delay(1, TimeUnit.MINUTES).execute(task).async().submit(plugin);
    }
}