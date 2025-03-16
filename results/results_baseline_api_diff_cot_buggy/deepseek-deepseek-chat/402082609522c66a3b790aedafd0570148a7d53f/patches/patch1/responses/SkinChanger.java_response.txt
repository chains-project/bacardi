package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.text.Component;

public class SkinChanger extends SharedSkinChanger {

    private final ServerPlayer invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, ServerPlayer invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        invoker.sendMessage(Component.text(localeMessage));
    }
}
