package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SkinChanger extends SharedSkinChanger {

    private final @Nullable Object invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, @Nullable Object invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        if (invoker != null) {
            // Assuming invoker has a sendMessage method that accepts a String
            // This is a placeholder and may need to be adjusted based on the actual method signature
            ((CommandSource) invoker).sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(localeMessage));
        }
    }
}