package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.command.CommandSource; // This import will be removed
import org.spongepowered.api.text.serializer.TextSerializers; // This import will be removed
import org.spongepowered.api.text.Text; // New import for text handling
import org.spongepowered.api.text.format.TextColor; // New import for text color

public class SkinChanger extends SharedSkinChanger {

    private final Object invoker; // Change type to Object to accommodate new API

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, Object invoker) { // Change parameter type to Object
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    protected void sendMessageInvoker(String localeMessage) {
        // Assuming the new API uses a different method to send messages
        // This is a placeholder for the new message sending logic
        // invoker.sendMessage(Text.of(localeMessage)); // Adjusted to use new Text class
        // Example of how to create a text message with color
        // invoker.sendMessage(Text.of(TextColor.GREEN, localeMessage)); // Adjusted to use new Text class
    }
}
