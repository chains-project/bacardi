package com.github.games647.changeskin.sponge.bungee;

import com.github.games647.changeskin.core.message.SkinUpdateMessage;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.task.SkinApplier;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.api.scheduler.Task;

public class UpdateSkinListener {

    @Inject
    private ChangeSkinSponge plugin;

    public void handlePayload(ByteBuf data, RemoteConnection connection, Type side) {
        byte[] array = new byte[data.readableBytes()];
        data.readBytes(array);
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(array);
        SkinUpdateMessage updateMessage = new SkinUpdateMessage();
        updateMessage.readFrom(dataInput);

        String playerName = updateMessage.getPlayerName();
        Optional<Player> receiver = Sponge.getServer().getPlayer(playerName);
        if (receiver.isPresent()) {
            Runnable skinUpdater = new SkinApplier(plugin, (CommandSource) connection, receiver.get(), null, false);
            Task.builder().execute(skinUpdater).submit(plugin);
        }
    }
}