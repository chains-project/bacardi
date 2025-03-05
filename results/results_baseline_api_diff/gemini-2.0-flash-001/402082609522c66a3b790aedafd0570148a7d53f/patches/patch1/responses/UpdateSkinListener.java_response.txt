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
import org.spongepowered.api.network.ChannelBuf;
import org.spongepowered.api.network.Connection;
import org.spongepowered.api.network.MessageHandler;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.util.bytebuf.ByteBufView;

public class UpdateSkinListener implements MessageHandler<ChannelBuf> {

    @Inject
    private ChangeSkinSponge plugin;

    @Override
    public void handleMessage(ChannelBuf data, Connection connection) {
        ByteBufView buffer = data.asView();
        byte[] array = new byte[buffer.readableBytes()];
        buffer.readBytes(array);

        ByteArrayDataInput dataInput = ByteStreams.newDataInput(array);
        SkinUpdateMessage updateMessage = new SkinUpdateMessage();
        updateMessage.readFrom(dataInput);

        String playerName = updateMessage.getPlayerName();
        Optional<Player> receiver = Sponge.getServer().getPlayer(playerName);
        if (receiver.isPresent()) {
            Runnable skinUpdater = new SkinApplier(plugin, (Player) connection, receiver.get(), null, false);
            Task.builder().execute(skinUpdater).plugin(plugin).submit(plugin);
        }
    }
}