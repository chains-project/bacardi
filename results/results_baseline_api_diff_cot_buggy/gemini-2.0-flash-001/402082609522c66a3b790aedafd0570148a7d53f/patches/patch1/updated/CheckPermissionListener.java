package com.github.games647.changeskin.sponge.bungee;

import com.github.games647.changeskin.core.message.NamespaceKey;
import com.github.games647.changeskin.core.message.CheckPermMessage;
import com.github.games647.changeskin.core.message.PermResultMessage;
import com.github.games647.changeskin.core.model.skin.SkinModel;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;

import java.util.UUID;

import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.network.RemoteConnection;

import static com.github.games647.changeskin.core.message.PermResultMessage.PERMISSION_RESULT_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

import org.spongepowered.api.network.channel.raw.play.RawPlayDataChannel;
import org.spongepowered.api.event.channel.ChannelEventListener;
import org.spongepowered.api.event.channel.ConnectionEvent;
import org.spongepowered.api.network.channel.PacketContext;
import org.spongepowered.api.network.channel.ChannelBuf;

public class CheckPermissionListener implements ChannelEventListener.Raw {

    private final ChangeSkinSponge plugin;
    private final RawPlayDataChannel permissionsResultChannel;

    @Inject
    CheckPermissionListener(ChangeSkinSponge plugin, RawPlayDataChannel permissionsResultChannel) {
        this.plugin = plugin;
        this.permissionsResultChannel = permissionsResultChannel;
    }

    public void handle(ConnectionEvent.Receive event, PacketContext context) {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(event.data().array());
        CheckPermMessage checkMessage = new CheckPermMessage();
        checkMessage.readFrom(dataInput);

        CheckPermMessage message = new CheckPermMessage();
        message.readFrom(dataInput);

        checkPermissions((Player) event.connection(), message);
    }

    private void checkPermissions(Player player, CheckPermMessage permMessage) {
        UUID receiverUUID = permMessage.getReceiverUUD();
        boolean op = permMessage.isOp();
        SkinModel targetSkin = permMessage.getTargetSkin();
        UUID skinProfile = targetSkin.getProfileId();

        boolean success = op || checkBungeePerms(player, receiverUUID, permMessage.isSkinPerm(), skinProfile);
        sendResultMessage(player, new PermResultMessage(success, targetSkin, receiverUUID));
    }

    private boolean checkBungeePerms(Player player, UUID receiverUUID, boolean skinPerm, UUID targetUUID) {
        if (player.getUniqueId().equals(receiverUUID)) {
            return checkPerm(player, "command.setskin", skinPerm, targetUUID);
        }

        return checkPerm(player, "command.setskin.other", skinPerm, targetUUID);
    }

    private boolean checkPerm(Player invoker, String node, boolean skinPerm, UUID targetUUID) {
        String pluginName = plugin.getName().toLowerCase();
        boolean hasCommandPerm = invoker.hasPermission(pluginName +  '.' + node);
        if (skinPerm) {
            return hasCommandPerm && plugin.hasSkinPermission(invoker, targetUUID, false);
        }

        return hasCommandPerm;
    }

    private void sendResultMessage(Player receiver, PermResultMessage resultMessage) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        resultMessage.writeTo(dataOutput);
        permissionsResultChannel.sendTo((org.spongepowered.api.entity.living.player.server.ServerPlayer) receiver, buf -> buf.writeByteArray(dataOutput.toByteArray()));
    }
}
