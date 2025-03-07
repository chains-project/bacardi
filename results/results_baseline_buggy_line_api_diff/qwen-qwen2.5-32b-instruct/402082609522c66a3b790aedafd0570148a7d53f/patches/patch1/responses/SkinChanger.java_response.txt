package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import java.util.function.Consumer;

public class SkinChanger extends SharedSkinChanger {

    private final Consumer<String> messageSender;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, Consumer<String> messageSender) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.messageSender = messageSender;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        messageSender.accept(localeMessage);
    }
}