package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.command.CommandSource; // This import will be removed
import org.spongepowered.api.text.Text; // New import for text handling
import org.spongepowered.api.text.Texts; // New import for text serialization

public class SkinChanger extends SharedSkinChanger {

    private final CommandSource invoker; // This field will be removed

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, CommandSource invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker; // This assignment will be removed
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        Text message = Texts.of(localeMessage); // Updated to use new text handling
        invoker.sendMessage(message); // This line remains unchanged
    }
}
