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
import org.spongepowered.api.Platform.Type;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.network.channel.ChannelManager;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginManager;
import org.spongepowered.api.plugin.spi.PluginProvider;
import org.spongepowered.api.plugin.spi.PluginService;
import org.spongepowered.api.plugin.spi.SpongePlugin;
import org.spongepowered.api.adventure.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import static com.github.games647.changeskin.core.message.CheckPermMessage.CHECK_PERM_CHANNEL;
import static com.github.games647.changeskin.core.message.SkinUpdateMessage.UPDATE_SKIN_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

@Singleton
@SpongePlugin(id = ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION,
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
    public void onPreInit(StartedEngineEvent<?> preInitEvent) {
        try {
            core.load(true);
            initialized = true;
        } catch (Exception ex) {
            logger.error("Error initializing plugin. Disabling...", ex);
        }
    }

    @Listener
    public void onInit(RegisterCommandEvent<CommandContext> initEvent) {
        if (!initialized)
            return;

        PluginContainer plugin = Sponge.pluginManager().plugin(ARTIFACT_ID).orElseThrow();

        initEvent.register(plugin, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        initEvent.register(plugin, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        initEvent.register(plugin, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        initEvent.register(plugin, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        initEvent.register(plugin, injector.getInstance(InvalidateCommand.class)
                .buildSpec(), "skininvalidate", "skin-invalidate");

        Sponge.eventManager().registerListeners(plugin, injector.getInstance(LoginListener.class));

        ChannelManager channelManager = Sponge.channelManager();
        String updateChannelName = new NamespaceKey(ARTIFACT_ID, UPDATE_SKIN_CHANNEL).getCombinedName();
        String permissionChannelName = new NamespaceKey(ARTIFACT_ID, CHECK_PERM_CHANNEL).getCombinedName();
        RawDataChannel updateChannel = channelManager.ofType(updateChannelName, RawDataChannel.class);
        RawDataChannel permChannel = channelManager.ofType(permissionChannelName, RawDataChannel.class);
        updateChannel.play().addHandler(Type.SERVER, injector.getInstance(UpdateSkinListener.class));
        permChannel.play().addHandler(Type.SERVER, injector.getInstance(CheckPermissionListener.class));
    }

    @Listener
    public void onShutdown(StoppingEngineEvent<?> stoppingServerEvent) {
        core.close();
    }

    public ChangeSkinCore getCore() {
        return core;
    }

    public boolean hasSkinPermission(Audience invoker, UUID uuid, boolean sendMessage) {
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

    public void sendMessage(Audience receiver, String key) {
        String message = core.getMessage(key);
        if (message != null && receiver != null) {
            Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(message);
            receiver.sendMessage(component);
        }
    }
}
