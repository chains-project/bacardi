package com.github.games647.changeskin.sponge.task;

import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.core.shared.task.SharedSkinChanger;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import org.spongepowered.api.command.source.CommandSource;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class SkinChanger extends SharedSkinChanger {

    private final CommandSource invoker;

    public SkinChanger(ChangeSkinSponge plugin, Account owner, String url, String oldSkinUrl, CommandSource invoker) {
        super(plugin.getCore(), owner, url, oldSkinUrl);
        this.invoker = invoker;
    }

    @Override
    protected void sendMessageInvoker(String localeMessage) {
        invoker.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(localeMessage));
    }
}