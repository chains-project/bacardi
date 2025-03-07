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
import org.slf4j.Logger;

// Use the new plugin annotation package
import org.spongepowered.plugin.Plugin;
import org.spongepowered.plugin.PluginContainer;

// Use the new lifecycle event types
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;

// Use Adventure’s legacy serializer for text conversion
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.ResourceKey;

// Note: We must remove usage of removed types such as CommandSource, CommandManager, ChannelRegistrar, etc.
// To keep the old method signatures, define a replacement CommandSource interface that
// combines the new Adventure Audience and the permission Subject.
@Singleton
@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION,
        url = PomData.URL, description = PomData.DESCRIPTION)
public class ChangeSkinSponge implements PlatformPlugin<ChangeSkinSponge.CommandSource> {

    private final Path dataFolder;
    private final Logger logger;
    private final Injector injector;

    private final ChangeSkinCore core = new ChangeSkinCore(this);
    private final SpongeSkinAPI api = new SpongeSkinAPI(this);

    private boolean initialized;
    
    @Inject
    private PluginContainer pluginContainer;

    // We will place more than one config there (i.e. H2/SQLite database) -> sharedRoot = false
    @Inject
    public ChangeSkinSponge(Logger logger, @org.spongepowered.api.config.ConfigDir(sharedRoot = false) Path dataFolder, Injector injector) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        // Create a child injector and bind the core instance
        this.injector = injector.createChildInjector(binder -> binder.bind(ChangeSkinCore.class).toInstance(core));
    }

    @Listener
    public void onPreInit(ConstructPluginEvent preInitEvent) {
        // load config and database
        try {
            core.load(true);
            initialized = true;
        } catch (Exception ex) {
            logger.error("Error initializing plugin. Disabling...", ex);
        }
    }

    @Listener
    public void onInit(RegisterCommandEvent initEvent) {
        if (!initialized) {
            return;
        }

        // Command registration using the new command registration event.
        initEvent.register(pluginContainer, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        initEvent.register(pluginContainer, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        initEvent.register(pluginContainer, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        initEvent.register(pluginContainer, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        initEvent.register(pluginContainer, injector.getInstance(InvalidateCommand.class).buildSpec(), "skininvalidate", "skin-invalidate");

        // Register additional event listeners using the new event registration API.
        pluginContainer.getEventManager().registerListeners(pluginContainer, injector.getInstance(LoginListener.class));

        // Channel registration via the new channel registration API.
        ResourceKey updateKey = ResourceKey.of(PomData.ARTIFACT_ID, UPDATE_SKIN_CHANNEL);
        ResourceKey permissionKey = ResourceKey.of(PomData.ARTIFACT_ID, CHECK_PERM_CHANNEL);
        pluginContainer.getEventManager().registerChannel(updateKey, injector.getInstance(UpdateSkinListener.class));
        pluginContainer.getEventManager().registerChannel(permissionKey, injector.getInstance(CheckPermissionListener.class));
    }

    @Listener
    public void onShutdown(StoppingEngineEvent stoppingEngineEvent) {
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

        // Disallow - not whitelisted or blacklisted.
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
            Component converted = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            receiver.sendMessage(converted);
        }
    }

    // Define a replacement for the removed CommandSource type.
    // This interface combines Adventure's Audience (for sending messages) with permission checking (Subject).
    public static interface CommandSource extends Audience, Subject {
        // No additional methods required.
    }
}