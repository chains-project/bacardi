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
import java.util.function.Consumer;

import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.network.RemoteConnection;
import org.spongepowered.api.network.channel.Channel;
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.ChannelRegistry;
import org.spongepowered.api.network.channel.RawDataChannel;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.Identifiable;

import static com.github.games647.changeskin.core.message.PermResultMessage.PERMISSION_RESULT_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

public class CheckPermissionListener implements java.util.function.BiConsumer<RemoteConnection, ChannelBuf> {

    private final ChangeSkinSponge plugin;
    private final RawDataChannel permissionsResultChannel;

    @Inject
    CheckPermissionListener(ChangeSkinSponge plugin, ChannelRegistry channelRegistry) {
        this.plugin = plugin;

        ResourceKey channelKey = ResourceKey.of(ARTIFACT_ID, PERMISSION_RESULT_CHANNEL);
        permissionsResultChannel = channelRegistry.getOrCreate(channelKey, RawDataChannel.class);
    }

    public void accept(RemoteConnection connection, ChannelBuf data) {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(data.readByteArray());
        CheckPermMessage checkMessage = new CheckPermMessage();
        checkMessage.readFrom(dataInput);

        CheckPermMessage message = new CheckPermMessage();
        message.readFrom(dataInput);

        checkPermissions((Player) connection, message);
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
        byte[] bytes = dataOutput.toByteArray();

        permissionsResultChannel.sendTo((ServerPlayer) receiver, channelBuf -> channelBuf.writeByteArray(bytes));
    }
}
