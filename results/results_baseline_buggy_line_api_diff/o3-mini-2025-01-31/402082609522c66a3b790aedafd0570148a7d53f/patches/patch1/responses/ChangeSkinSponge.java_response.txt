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
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StopGameEvent;
import org.spongepowered.api.registry.ResourceKey;
import org.spongepowered.plugin.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.identity.Identity;

import static com.github.games647.changeskin.core.message.CheckPermMessage.CHECK_PERM_CHANNEL;
import static com.github.games647.changeskin.core.message.SkinUpdateMessage.UPDATE_SKIN_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

@Singleton
@Plugin(id = ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION, url = PomData.URL, description = PomData.DESCRIPTION)
public class ChangeSkinSponge implements PlatformPlugin<CommandCause> {

    private final Path dataFolder;
    private final Logger logger;
    private final Injector injector;

    private final ChangeSkinCore core = new ChangeSkinCore(this);
    private final SpongeSkinAPI api = new SpongeSkinAPI(this);

    private boolean initialized;

    @Inject
    ChangeSkinSponge(Logger logger, @org.spongepowered.api.config.ConfigDir(sharedRoot = false) Path dataFolder, Injector injector) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.injector = injector.createChildInjector(binder -> binder.bind(ChangeSkinCore.class).toInstance(core));
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
        // Obtain the plugin container from the Sponge plugin manager.
        Object pluginContainer = Sponge.game().pluginManager().plugin(ARTIFACT_ID).get();
        event.register(pluginContainer, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        event.register(pluginContainer, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        event.register(pluginContainer, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        event.register(pluginContainer, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        event.register(pluginContainer, injector.getInstance(InvalidateCommand.class).buildSpec(), "skininvalidate", "skin-invalidate");

        Sponge.game().eventManager().registerListeners(pluginContainer, injector.getInstance(LoginListener.class));
    }

    @Listener
    public void onRegisterChannels(RegisterChannelEvent event) {
        Object pluginContainer = Sponge.game().pluginManager().plugin(ARTIFACT_ID).get();
        String updateChannelName = new NamespaceKey(ARTIFACT_ID, UPDATE_SKIN_CHANNEL).getCombinedName();
        String permissionChannelName = new NamespaceKey(ARTIFACT_ID, CHECK_PERM_CHANNEL).getCombinedName();
        event.register(ResourceKey.resolve(updateChannelName), UpdateSkinListener.class);
        event.register(ResourceKey.resolve(permissionChannelName), CheckPermissionListener.class);
    }

    @Listener
    public void onShutdown(StopGameEvent event) {
        core.close();
    }

    public ChangeSkinCore getCore() {
        return core;
    }

    public boolean hasSkinPermission(CommandCause invoker, UUID uuid, boolean sendMessage) {
        if (invoker.hasPermission(PomData.ARTIFACT_ID + ".skin.whitelist." + uuid)) {
            return true;
        }
        if (sendMessage) {
            sendMessage(invoker, "no-permission");
        }
        return false;
    }

    public SpongeSkinAPI getApi() {
        return api;
    }

    public String getName() {
        return PomData.NAME;
    }

    public Path getPluginFolder() {
        return dataFolder;
    }

    public Logger getLog() {
        return logger;
    }

    public void sendMessage(CommandCause receiver, String key) {
        String message = core.getMessage(key);
        if (message != null && receiver != null) {
            receiver.sendMessage(Identity.nil(), Component.text(message));
        }
    }
}