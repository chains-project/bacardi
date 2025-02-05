package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.command.CommandSource; // This import is now incorrect
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.command.CommandSourceStack; // New import for CommandSourceStack

public class SkinChanger extends SharedSkinChanger {

    private final CommandSourceStack invoker; // Changed type to CommandSourceStack

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, CommandSourceStack invoker) { // Updated constructor parameter type
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        invoker.sendMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(localeMessage));
    }
}