package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import net.kyori.adventure.audience.Audience;

public class SkinChanger extends SharedSkinChanger {

    private final Object invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, Object invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        ((Audience) invoker).sendMessage(plugin.getPlugin().getAdventure().legacyAmpersand().deserialize(localeMessage));
    }
}
