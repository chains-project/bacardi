package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.shared.task.SharedNameResolver;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.entity.living.player.Player;

public class NameResolver extends SharedNameResolver {

    private final ChangeSkinSponge plugin;
    private final Audience invoker;
    private final Player receiver;
    private final boolean keepSkin;

    public NameResolver(ChangeSkinSponge plugin, Audience invoker, String targetName, Player receiver, boolean keepSkin) {
        super(plugin.getCore(), targetName, keepSkin);
        this.plugin = plugin;
        this.invoker = invoker;
        this.receiver = receiver;
        this.keepSkin = keepSkin;
    }

    @Override
    public void sendMessageInvoker(String id) {
        plugin.sendMessage(invoker, id);
    }

    @Override
    protected boolean hasSkinPermission(UUID uuid) {
        // TODO: check skin permissions
        return true;
    }

    @Override
    protected void scheduleDownloader(UUID uuid) {
        // Run this in the same thread
        new SkinDownloader(plugin, invoker, receiver, uuid, keepSkin).run();
    }
}