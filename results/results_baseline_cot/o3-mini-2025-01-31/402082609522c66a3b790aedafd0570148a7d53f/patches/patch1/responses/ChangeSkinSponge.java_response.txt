package com.github.games647.changeskin.sponge;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.PlatformPlugin;
import com.github.games647.changeskin.core.message.NamespaceKey;
import com.github.games647.changeskin.sponge.bungee.CheckPermissionListener;
import com.github.games647.changeskin.sponge.bungee.UpdateSkinListener;
import com.github.games647.changeskin.sponge.command.InfoCommand;
import com.github.games647.changeskin.sponge.command.InvalidateCommand;
import com.github.games647.changeskin.sponge.command.SelectCommand;
import com.github.games647.changeskin.sponge.command.SetCommand;
import com.github.games647.changeskin.sponge.command.UploadCommand;
import com.github.games647.changeskin.sponge.LoginListener;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;

// Updated imports for Sponge API 8+
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StartEngineEvent;
import org.spongepowered.api.event.lifecycle.StopEngineEvent;
import org.spongepowered.api.network.channel.ChannelRegistrar;
import org.spongepowered.api.network.channel.ChannelBinding.RawDataChannel;
// Removed: import org.spongepowered.api.plugin.Plugin;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import static com.github.games647.changeskin.core.message.CheckPermMessage.CHECK_PERM_CHANNEL;
import static com.github.games647.changeskin.core.message.SkinUpdateMessage.UPDATE_SKIN_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

@Singleton
// Removing @Plugin annotation because it no longer exists in the updated dependency.
public class ChangeSkinSponge implements PlatformPlugin<CommandSource> {

    private final Path dataFolder;
    private final Logger logger;
    private final Injector injector;

    private final ChangeSkinCore core = new ChangeSkinCore(this);
    private final SpongeSkinAPI api = new SpongeSkinAPI(this);

    private boolean initialized;

    // We will place more than one config there (i.e. H2/SQLite database) -> sharedRoot = false
    @Inject
    ChangeSkinSponge(Logger logger, @ConfigDir(sharedRoot = false) Path dataFolder, Injector injector) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.injector = injector.createChildInjector(binder -> binder.bind(ChangeSkinCore.class).toInstance(core));
    }

    @Listener
    public void onPreInit(ConstructPluginEvent event) {
        // Load config and database
        try {
            core.load(true);
            initialized = true;
        } catch (Exception ex) {
            logger.error("Error initializing plugin. Disabling...", ex);
        }
    }

    @Listener
    public void onInit(StartEngineEvent event) {
        if (!initialized) {
            return;
        }

        CommandManager cmdManager = Sponge.getCommandManager();

        // Command and event registration
        cmdManager.register(this, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        cmdManager.register(this, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        cmdManager.register(this, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        cmdManager.register(this, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        cmdManager.register(this, injector.getInstance(InvalidateCommand.class).buildSpec(), "skininvalidate", "skin-invalidate");

        Sponge.getEventManager().registerListeners(this, injector.getInstance(LoginListener.class));

        // Incoming channel registration
        ChannelRegistrar channelReg = Sponge.getChannelRegistrar();
        String updateChannelName = new NamespaceKey(ARTIFACT_ID, UPDATE_SKIN_CHANNEL).getCombinedName();
        String permissionChannelName = new NamespaceKey(ARTIFACT_ID, CHECK_PERM_CHANNEL).getCombinedName();
        RawDataChannel updateChannel = channelReg.getOrCreateRaw(this, updateChannelName);
        RawDataChannel permChannel = channelReg.getOrCreateRaw(this, permissionChannelName);
        updateChannel.addListener(org.spongepowered.api.Platform.Type.SERVER, injector.getInstance(UpdateSkinListener.class));
        permChannel.addListener(org.spongepowered.api.Platform.Type.SERVER, injector.getInstance(CheckPermissionListener.class));
    }

    @Listener
    public void onShutdown(StopEngineEvent event) {
        core.close();
    }

    public ChangeSkinCore getCore() {
        return core;
    }

    @Override
    public boolean hasSkinPermission(CommandSource invoker, UUID uuid, boolean sendMessage) {
        if (invoker.hasPermission(PomData.ARTIFACT_ID + ".skin.whitelist." + uuid)) {
            return true;
        }

        // Disallow - not whitelisted or blacklisted
        if (sendMessage) {
            sendMessage(invoker, "no-permission");
        }

        return false;
    }

    public SpongeSkinAPI getApi() {
        return api;
    }

    @Override
    public String getName() {
        return PomData.NAME;
    }

    @Override
    public Path getPluginFolder() {
        return dataFolder;
    }

    @Override
    public Logger getLog() {
        return logger;
    }

    @Override
    public void sendMessage(CommandSource receiver, String key) {
        String message = core.getMessage(key);
        if (message != null && receiver != null) {
            receiver.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        }
    }
}