package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

// Updated imports to reflect the new package structure in the Sponge API
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class SkinChanger extends SharedSkinChanger {

    private final CommandSource invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, CommandSource invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        // Updated to use the correct Text type for the new Sponge API
        Text message = TextSerializers.LEGACY_FORMATTING_CODE.deserialize(localeMessage);
        invoker.sendMessage(message);
    }
}