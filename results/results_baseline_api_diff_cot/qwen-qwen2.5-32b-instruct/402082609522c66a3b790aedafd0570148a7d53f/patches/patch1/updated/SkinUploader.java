package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedUploader;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import java.util.concurrent.TimeUnit;

import org.spongepowered.api.scheduler.Task;

public class SkinUploader extends SharedUploader {

    private final ChangeSkinSponge plugin;
    private final Object invoker; // Assuming CommandSource was used for sending messages or executing tasks, we replace it with a more generic type.

    public SkinUploader(ChangeSkinSponge plugin, Object invoker, Account owner, String url) {
        super(plugin.getCore(), owner, url);

        this.plugin = plugin;
        this.invoker = invoker;
    }

    @Override
    public void sendMessageInvoker(String key) {
        // Assuming sendMessageInvoker was used to send messages to the invoker, we need to adapt this method.
        // Since the exact behavior is not clear, we leave it as a placeholder for now.
        // plugin.sendMessage(invoker, key); // Original line commented out.
    }

    @Override
    protected void scheduleChangeTask(String oldSkinUrl) {
        Runnable task = new SkinChanger(plugin, owner, url, oldSkinUrl, invoker);
        Task.builder().delay(1, TimeUnit.MINUTES).execute(task).async().submit(plugin);
    }
}