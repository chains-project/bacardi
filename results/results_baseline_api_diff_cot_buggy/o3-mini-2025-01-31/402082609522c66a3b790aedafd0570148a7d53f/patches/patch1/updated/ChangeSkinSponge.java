package com.github.games647.changeskin.sponge;

import static com.github.games647.changeskin.core.message.CheckPermMessage.CHECK_PERM_CHANNEL;
import static com.github.games647.changeskin.core.message.SkinUpdateMessage.UPDATE_SKIN_CHANNEL;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.PlatformPlugin;
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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StopEvent;
import org.spongepowered.plugin.Plugin;
import org.spongepowered.plugin.PluginContainer;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.LoginListener;

@Singleton
@Plugin(id = PomData.ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION,
        url = PomData.URL, description = PomData.DESCRIPTION)
public class ChangeSkinSponge implements PlatformPlugin<Audience> {

    private final Path dataFolder;
    private final Logger logger;
    private final Injector injector;

    private final ChangeSkinCore core = new ChangeSkinCore(this);
    private final SpongeSkinAPI api = new SpongeSkinAPI(this);

    private boolean initialized;

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    ChangeSkinSponge(Logger logger, @ConfigDir(sharedRoot = false) Path dataFolder, Injector injector) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.injector = injector.createChildInjector(binder -> binder.bind(ChangeSkinCore.class).toInstance(core));
    }

    @Listener
    public void onConstruct(ConstructPluginEvent event) {
        try {
            core.load(true);
            initialized = true;
        } catch (Exception ex) {
            logger.error("Error initializing plugin. Disabling...", ex);
        }
        if (initialized) {
            Sponge.eventManager().registerListeners(pluginContainer, injector.getInstance(LoginListener.class));
        }
    }

    @Listener
    public void onRegisterCommands(RegisterCommandEvent event) {
        if (!initialized) {
            return;
        }
        event.register(pluginContainer, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        event.register(pluginContainer, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        event.register(pluginContainer, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        event.register(pluginContainer, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        event.register(pluginContainer, injector.getInstance(InvalidateCommand.class).buildSpec(), "skininvalidate", "skin-invalidate");
    }

    @Listener
    public void onRegisterChannels(RegisterChannelEvent event) {
        if (!initialized) {
            return;
        }
        ResourceKey updateChannelKey = ResourceKey.of(PomData.ARTIFACT_ID, UPDATE_SKIN_CHANNEL);
        event.register(updateChannelKey, UpdateSkinListener.class);
        ResourceKey permissionChannelKey = ResourceKey.of(PomData.ARTIFACT_ID, CHECK_PERM_CHANNEL);
        event.register(permissionChannelKey, CheckPermissionListener.class);
    }

    @Listener
    public void onStop(StopEvent event) {
        core.close();
    }

    public ChangeSkinCore getCore() {
        return core;
    }

    @Override
    public boolean hasSkinPermission(Audience invoker, UUID uuid, boolean sendMessage) {
        if (invoker instanceof org.spongepowered.api.service.permission.Subject) {
            org.spongepowered.api.service.permission.Subject subject = (org.spongepowered.api.service.permission.Subject) invoker;
            if (subject.hasPermission(PomData.ARTIFACT_ID + ".skin.whitelist." + uuid)) {
                return true;
            }
        }
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
    public void sendMessage(Audience receiver, String key) {
        String message = core.getMessage(key);
        if (message != null && receiver != null) {
            receiver.sendMessage(LegacyComponentSerializer.legacySection().deserialize(message));
        }
    }
}