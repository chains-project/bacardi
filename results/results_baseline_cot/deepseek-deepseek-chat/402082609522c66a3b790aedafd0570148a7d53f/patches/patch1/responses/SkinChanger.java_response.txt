package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.text.serializer.ComponentSerializer;

public class SkinChanger extends SharedSkinChanger {

    private final CommandCause invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, CommandCause invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        invoker.sendMessage(ComponentSerializer.legacy().deserialize(localeMessage));
    }
}