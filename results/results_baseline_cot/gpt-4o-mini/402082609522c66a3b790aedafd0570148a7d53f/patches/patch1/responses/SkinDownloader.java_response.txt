package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.skin.SkinModel;
import com.github.games647.changeskin.core.shared.task.SharedDownloader;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import java.util.UUID;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.command.CommandSource; // Updated import

public class SkinDownloader extends SharedDownloader {

    private final ChangeSkinSponge plugin;
    private final CommandSource invoker;
    private final Player receiver;

    public SkinDownloader(ChangeSkinSponge plugin, CommandSource invoker, Player receiver, UUID targetUUID
            , boolean keepSkin) {
        super(plugin.getCore(), keepSkin, targetUUID, receiver.getUniqueId());

        this.plugin = plugin;
        this.invoker = invoker;
        this.receiver = receiver;
    }

    protected void scheduleApplyTask(SkinModel skinData) { // Removed @Override
        Runnable skinUpdater = new SkinApplier(plugin, invoker, receiver, skinData, keepSkin);
        Task.builder().execute(skinUpdater).submit(plugin);
    }

    public void sendMessageInvoker(String id) {
        plugin.sendMessage(invoker, id);
    }
}