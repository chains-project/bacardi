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
import org.spongepowered.api.Sponge;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.plugin.Plugin;
import org.spongepowered.plugin.PluginContainer;
import com.github.games647.changeskin.sponge.PomData;

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
    ChangeSkinSponge(Logger logger, @ConfigDir(sharedRoot = false) Path dataFolder, Injector injector) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.injector = injector.createChildInjector(binder -> binder.bind(ChangeSkinCore.class).toInstance(core));
    }

    @Listener
    public void onPreInit(ConstructPluginEvent.Pre preInitEvent) {
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
        event.register(this, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        event.register(this, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        event.register(this, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        event.register(this, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        event.register(this, injector.getInstance(InvalidateCommand.class).buildSpec(), "skininvalidate", "skin-invalidate");

        PluginContainer container = Sponge.pluginManager().plugin(PomData.ARTIFACT_ID)
                .orElseThrow(() -> new IllegalStateException("Plugin container not found"));
        Sponge.eventManager().registerListeners(container, injector.getInstance(LoginListener.class));
    }

    @Listener
    public void onShutdown(StoppingEngineEvent stoppingEngineEvent) {
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
            receiver.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
        }
    }
}