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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.audience.Audience;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StopGameEvent;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.service.permission.Subject;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.plugin.PluginContainer;

import static com.github.games647.changeskin.core.message.CheckPermMessage.CHECK_PERM_CHANNEL;
import static com.github.games647.changeskin.core.message.SkinUpdateMessage.UPDATE_SKIN_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

@Singleton
public class ChangeSkinSponge implements PlatformPlugin<Audience> {

    private final Path dataFolder;
    private final Logger logger;
    private final Injector injector;
    private final ChangeSkinCore core = new ChangeSkinCore(this);
    private final SpongeSkinAPI api = new SpongeSkinAPI(this);
    private boolean initialized;
    private final PluginContainer pluginContainer;

    @Inject
    ChangeSkinSponge(Logger logger, @ConfigDir(sharedRoot = false) Path dataFolder, Injector injector) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.injector = injector.createChildInjector(binder -> binder.bind(ChangeSkinCore.class).toInstance(core));
        this.pluginContainer = new DummyPluginContainer();
    }

    @Listener
    public void onPreInit(ConstructPluginEvent event) {
        try {
            core.load(true);
            initialized = true;
        } catch (Exception ex) {
            logger.error("Error initializing plugin. Disabling...", ex);
        }
    }

    @Listener
    public void onInit(RegisterCommandEvent event) {
        if (!initialized) {
            return;
        }
        event.register(pluginContainer, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        event.register(pluginContainer, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        event.register(pluginContainer, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        event.register(pluginContainer, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        event.register(pluginContainer, injector.getInstance(InvalidateCommand.class).buildSpec(), "skininvalidate", "skin-invalidate");

        Sponge.getEventManager().registerListeners(pluginContainer, injector.getInstance(LoginListener.class));
    }

    @Listener
    public void onChannelRegister(RegisterChannelEvent event) {
        ResourceKey updateKey = ResourceKey.of(ARTIFACT_ID, UPDATE_SKIN_CHANNEL);
        ResourceKey permKey = ResourceKey.of(ARTIFACT_ID, CHECK_PERM_CHANNEL);
        event.register(updateKey, UpdateSkinListener.class);
        event.register(permKey, CheckPermissionListener.class);
    }

    @Listener
    public void onShutdown(StopGameEvent event) {
        core.close();
    }

    public ChangeSkinCore getCore() {
        return core;
    }

    @Override
    public boolean hasSkinPermission(Audience invoker, UUID uuid, boolean sendMessage) {
        if (invoker instanceof Subject) {
            Subject subject = (Subject) invoker;
            if (subject.hasPermission(PomData.ARTIFACT_ID + ".skin.whitelist." + uuid, Cause.empty())) {
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
            LegacyComponentSerializer serializer = LegacyComponentSerializer.legacySection();
            receiver.sendMessage(serializer.deserialize(message));
        }
    }

    private static class DummyPluginContainer implements PluginContainer {

        @Override
        public String id() {
            return ARTIFACT_ID;
        }

        @Override
        public String name() {
            return PomData.NAME;
        }

        @Override
        public String version() {
            return PomData.VERSION;
        }

        @Override
        public Object instance() {
            return null;
        }

        @Override
        public Logger logger() {
            return org.slf4j.LoggerFactory.getLogger(PomData.NAME);
        }

        @Override
        public String toString() {
            return name() + " (" + id() + ")";
        }
    }
}