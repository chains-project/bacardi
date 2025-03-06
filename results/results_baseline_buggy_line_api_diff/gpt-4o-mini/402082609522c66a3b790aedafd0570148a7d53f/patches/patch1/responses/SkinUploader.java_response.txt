package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedUploader;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import java.util.concurrent.TimeUnit;

import org.spongepowered.api.command.CommandSource; // This import will be removed
import org.spongepowered.api.command.CommandSender; // New import for updated dependency
import org.spongepowered.api.scheduler.Task;

public class SkinUploader extends SharedUploader {

    private final ChangeSkinSponge plugin;
    private final CommandSender invoker; // Changed type from CommandSource to CommandSender

    public SkinUploader(ChangeSkinSponge plugin, CommandSender invoker, Account owner, String url) { // Updated parameter type
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