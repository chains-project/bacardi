package com.github.games647.changeskin.sponge.bungee;

import com.github.games647.changeskin.core.message.SkinUpdateMessage;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.task.SkinApplier;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import java.util.Optional;
import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.api.command.source.CommandSource;

public class UpdateSkinListener {

    @Inject
    private ChangeSkinSponge plugin;

    public void handlePayload(ChannelBuf data, RemoteConnection connection, Type side) {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(data.array());
        SkinUpdateMessage updateMessage = new SkinUpdateMessage();
        updateMessage.readFrom(dataInput);

        String playerName = updateMessage.getPlayerName();
        Optional<Player> receiver = Sponge.server().getPlayers().stream()
                .filter(player -> player.getName().equals(playerName))
                .findFirst();
        if (receiver.isPresent()) {
            Runnable skinUpdater = new SkinApplier(plugin, (CommandSource) connection, receiver.get(), null, false);
            Sponge.server().getScheduler().submit(skinUpdater, plugin);
        }
    }
}