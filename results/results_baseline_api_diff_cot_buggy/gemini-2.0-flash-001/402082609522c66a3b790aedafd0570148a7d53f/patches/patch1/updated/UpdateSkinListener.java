package com.github.games647.changeskin.sponge.bungee;

import com.github.games647.changeskin.core.message.SkinUpdateMessage;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.task.SkinApplier;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.RawDataHandler;
import org.spongepowered.api.network.channel.ChannelReceiver;

public class UpdateSkinListener implements RawDataHandler<RemoteConnection> {

    @Inject
    private ChangeSkinSponge plugin;

    @Override
    public void handlePayload(ChannelBuf data, RemoteConnection connection, ChannelReceiver side) {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(data.array());
        SkinUpdateMessage updateMessage = new SkinUpdateMessage();
        updateMessage.readFrom(dataInput);

        String playerName = updateMessage.getPlayerName();
        Optional<Player> receiver = Sponge.getServer().getPlayer(playerName);
        if (receiver.isPresent()) {
            Runnable skinUpdater = new SkinApplier(plugin, connection, receiver.get(), null, false);
            Task.builder().execute(skinUpdater).submit(plugin);
        }
    }
}
