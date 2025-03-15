package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text; // Updated import for new text handling
import org.spongepowered.api.text.format.TextColor; // New import for text color handling
import org.spongepowered.api.text.format.TextStyles; // New import for text styles

public class SkinChanger extends SharedSkinChanger {

    private final CommandSource invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, CommandSource invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        // Assuming the new API requires creating a Text object directly
        Text message = Text.of(TextColor.WHITE, localeMessage); // Create a Text object with the message
        invoker.sendMessage(message);
    }
}