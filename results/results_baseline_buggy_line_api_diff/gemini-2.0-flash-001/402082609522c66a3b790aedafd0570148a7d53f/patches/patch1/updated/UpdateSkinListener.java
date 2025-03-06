package com.github.games647.changeskin.sponge.bungee;

import com.github.games647.changeskin.core.message.SkinUpdateMessage;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.task.SkinApplier;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.RawDataListener;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.scheduler.TaskExecutorService;

public class UpdateSkinListener implements RawDataListener {

    @Inject
    private ChangeSkinSponge plugin;

    @Override
    public void handlePayload(ChannelBuf data, RemoteConnection connection, Type side) {
        byte[] rawData = new byte[data.available()];
        data.readBytes(rawData);

        ByteArrayDataInput dataInput = ByteStreams.newDataInput(rawData);
        SkinUpdateMessage updateMessage = new SkinUpdateMessage();
        updateMessage.readFrom(dataInput);

        String playerName = updateMessage.getPlayerName();
        Optional<Player> receiver = Sponge.getServer().getPlayer(playerName);
        if (receiver.isPresent()) {
            Player player = receiver.get();
            TaskExecutorService taskExecutorService = Sponge.getServer().getScheduler().createSyncExecutor(plugin);
            Runnable skinUpdater = new SkinApplier(plugin, player.getCommandSource().orElse(null), player, null, false);
            taskExecutorService.execute(skinUpdater);
        }
    }
}