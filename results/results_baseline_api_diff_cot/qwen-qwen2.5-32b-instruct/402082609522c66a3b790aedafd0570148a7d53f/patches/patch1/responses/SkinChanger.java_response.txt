package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.text.Text;

public class SkinChanger extends SharedSkinChanger {

    private final Object invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, Object invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);

        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        // Assuming invoker has a sendMessage method that accepts a Text object
        ((Object) invoker).sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(localeMessage));
    }
}