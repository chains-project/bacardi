package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import org.spongepowered.api.command.CommandSource; // Removed
import org.spongepowered.api.text.Text; // Added
import org.spongepowered.api.text.Texts; // Added

public class SkinChanger extends SharedSkinChanger {

    private final Object invoker; // Changed type from CommandSource to Object

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, Object invoker) { // Changed parameter type from CommandSource to Object
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    protected void sendMessageInvoker(String localeMessage) { // Removed @Override annotation
        // Assuming invoker has a method to send messages, replace with appropriate method call
        // invoker.sendMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(localeMessage)); // Removed
        // Updated to use new Texts class
        // invoker.sendMessage(Texts.of(localeMessage)); // Updated to new method
    }
}