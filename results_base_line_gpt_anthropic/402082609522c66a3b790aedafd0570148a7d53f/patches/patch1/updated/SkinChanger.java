package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

// Updated import for CommandSource based on the new version of Sponge API
import org.spongepowered.api.command.CommandSource;

// Updated import for TextSerializers based on the new version of Sponge API
import org.spongepowered.api.text.serializer.TextSerializers;

public class SkinChanger extends SharedSkinChanger {

    private final CommandSource invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, CommandSource invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        invoker.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(localeMessage));
    }
}