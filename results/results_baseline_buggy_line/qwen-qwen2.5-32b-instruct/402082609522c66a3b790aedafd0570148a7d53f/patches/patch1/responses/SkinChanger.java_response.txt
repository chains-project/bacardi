package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.service.context.Contextual;
import org.spongepowered.api.text.Text;

public class SkinChanger extends SharedSkinChanger {

    private final Contextual invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, Contextual invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        invoker.sendMessage(Text.of(localeMessage));
    }
}