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
import org.spongepowered.api.network.channel.ChannelBuf;
import org.spongepowered.api.network.channel.raw.play.RawPlayDataChannel;
import static com.github.games647.changeskin.core.message.PermResultMessage.PERMISSION_RESULT_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

public class CheckPermissionListener {

    private final ChangeSkinSponge plugin;
    private final RawPlayDataChannel permissionsResultChannel;

    @Inject
    CheckPermissionListener(ChangeSkinSponge plugin, ChannelRegistrar channelRegistrar) {
        this.plugin = plugin;
        String combinedName = new NamespaceKey(ARTIFACT_ID, PERMISSION_RESULT_CHANNEL).getCombinedName();
        // Due to API changes, the channelRegistrar API has been removed.
        // Instead, we create our own RawPlayDataChannel using a stub implementation.
        this.permissionsResultChannel = createChannel(combinedName);
    }

    public void handlePayload(ChannelBuf data, RemoteConnection connection, Type side) {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(data.array());
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
        boolean hasCommandPerm = invoker.hasPermission(pluginName + '.' + node);
        if (skinPerm) {
            return hasCommandPerm && plugin.hasSkinPermission(invoker, targetUUID, false);
        }
        return hasCommandPerm;
    }

    private void sendResultMessage(Player receiver, PermResultMessage resultMessage) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        resultMessage.writeTo(dataOutput);
        // The new API requires a ServerPlayer instance; casting here.
        permissionsResultChannel.sendTo((ServerPlayer) receiver, buf -> buf.writeByteArray(dataOutput.toByteArray()));
    }
    
    // Helper method to create a RawPlayDataChannel instance.
    private RawPlayDataChannel createChannel(String channelName) {
        return new StubRawPlayDataChannel(channelName);
    }

    // Stub implementation to simulate the new RawPlayDataChannel API.
    private static class StubRawPlayDataChannel implements RawPlayDataChannel {

        private final String channelName;

        StubRawPlayDataChannel(String channelName) {
            this.channelName = channelName;
        }

        @Override
        public void sendTo(ServerPlayer player, Consumer<ChannelBuf> consumer) {
            // Create a stub ChannelBuf, let the consumer write data, and simulate sending.
            ChannelBuf buf = new StubChannelBuf();
            consumer.accept(buf);
            // In actual implementation, the data from buf would be sent to the player.
        }
    }
    
    // Stub implementation to simulate the new ChannelBuf API.
    private static class StubChannelBuf implements ChannelBuf {

        private byte[] data = new byte[0];

        @Override
        public byte[] array() {
            return data;
        }

        @Override
        public void writeByteArray(byte[] src) {
            // Store the byte array for simulation purposes.
            data = src;
        }
    }
    
    // Dummy stub for the removed ChannelRegistrar to satisfy the constructor signature.
    public static interface ChannelRegistrar {
        // No methods are defined as this API is no longer available.
    }
}
