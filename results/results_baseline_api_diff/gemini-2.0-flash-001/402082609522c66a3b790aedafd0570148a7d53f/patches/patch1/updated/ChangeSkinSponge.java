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
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import java.nio.file.Path;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

import static com.github.games647.changeskin.core.message.CheckPermMessage.CHECK_PERM_CHANNEL;
import static com.github.games647.changeskin.core.message.SkinUpdateMessage.UPDATE_SKIN_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

@Singleton
@Plugin(ARTIFACT_ID)
public class ChangeSkinSponge implements PlatformPlugin<org.spongepowered.api.command.CommandCause> {

    private final Path dataFolder;
    private final Logger logger;
    private final Injector injector;
    private final PluginContainer pluginContainer;

    private final ChangeSkinCore core = new ChangeSkinCore(this);
    private final SpongeSkinAPI api = new SpongeSkinAPI(this);

    private boolean initialized;

    //We will place more than one config there (i.e. H2/SQLite database) -> sharedRoot = false
    @Inject
    ChangeSkinSponge(Logger logger, @ConfigDir(sharedRoot = false) Path dataFolder, Injector injector, PluginContainer pluginContainer) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.injector = injector.createChildInjector(binder -> binder.bind(ChangeSkinCore.class).toInstance(core));
        this.pluginContainer = pluginContainer;
    }

    @Listener
    public void onConstructPlugin(ConstructPluginEvent constructEvent) {
        //load config and database
        try {
            core.load(true);
            initialized = true;
        } catch (Exception ex) {
            logger.error("Error initializing plugin. Disabling...", ex);
        }
    }

    @Listener
    public void onInit(StartedEngineEvent<org.spongepowered.api.Server> initEvent) {
        if (!initialized) {
            return;
        }

        //command and event register
        Sponge.eventManager().registerListeners(this, injector.getInstance(LoginListener.class));

        registerCommands();

        //incoming channel
        String updateChannelName = new NamespaceKey(ARTIFACT_ID, UPDATE_SKIN_CHANNEL).getCombinedName();
        String permissionChannelName = new NamespaceKey(ARTIFACT_ID, CHECK_PERM_CHANNEL).getCombinedName();

        Sponge.channelRegistry().register(updateChannelName, injector.getInstance(UpdateSkinListener.class));
        Sponge.channelRegistry().register(permissionChannelName, injector.getInstance(CheckPermissionListener.class));
    }

    private void registerCommands() {
        Sponge.commandManager().register(pluginContainer, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        Sponge.commandManager().register(pluginContainer, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        Sponge.commandManager().register(pluginContainer, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        Sponge.commandManager().register(pluginContainer, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        Sponge.commandManager().register(pluginContainer, injector.getInstance(InvalidateCommand.class).buildSpec(), "skininvalidate", "skin-invalidate");
    }

    @Listener
    public void onShutdown(StoppingEngineEvent<org.spongepowered.api.Server> stoppingServerEvent) {
        core.close();
    }

    public ChangeSkinCore getCore() {
        return core;
    }

    @Override
    public boolean hasSkinPermission(org.spongepowered.api.command.CommandCause invoker, UUID uuid, boolean sendMessage) {
        if (invoker.hasPermission(PomData.ARTIFACT_ID + ".skin.whitelist." + uuid)) {
            return true;
        }

        //disallow - not whitelisted or blacklisted
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
    public void sendMessage(org.spongepowered.api.command.CommandCause receiver, String key) {
        String message = core.getMessage(key);
        if (message != null && receiver != null) {
            Component component = LegacyComponentSerializer.legacySection().deserialize(message);
            receiver.sendMessage(component);
        }
    }
}