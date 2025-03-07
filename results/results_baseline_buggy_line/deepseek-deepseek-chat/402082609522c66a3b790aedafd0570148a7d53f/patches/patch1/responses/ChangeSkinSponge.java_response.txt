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
import org.spongepowered.api.command.manager.CommandManager;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.api.network.channel.ChannelManager;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializer;

import static com.github.games647.changeskin.core.message.CheckPermMessage.CHECK_PERM_CHANNEL;
import static com.github.games647.changeskin.core.message.SkinUpdateMessage.UPDATE_SKIN_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

@Singleton
@Plugin(ARTIFACT_ID)
public class ChangeSkinSponge implements PlatformPlugin<CommandCause> {

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
    public void onPreInit(StartingEngineEvent<Server> preInitEvent) {
        try {
            core.load(true);
            initialized = true;
        } catch (Exception ex) {
            logger.error("Error initializing plugin. Disabling...", ex);
        }
    }

    @Listener
    public void onInit(RegisterCommandEvent<CommandCause> initEvent) {
        if (!initialized)
            return;

        CommandManager cmdManager = Sponge.server().commandManager();

        cmdManager.register(this, injector.getInstance(SelectCommand.class).buildSpec(), "skin-select", "skinselect");
        cmdManager.register(this, injector.getInstance(InfoCommand.class).buildSpec(), "skin-info");
        cmdManager.register(this, injector.getInstance(UploadCommand.class).buildSpec(), "skin-upload");
        cmdManager.register(this, injector.getInstance(SetCommand.class).buildSpec(), "changeskin", "setskin", "skin");
        cmdManager.register(this, injector.getInstance(InvalidateCommand.class)
                .buildSpec(), "skininvalidate", "skin-invalidate");

        Sponge.eventManager().registerListeners(this, injector.getInstance(LoginListener.class));

        ChannelManager channelReg = Sponge.channelManager();
        String updateChannelName = new NamespaceKey(ARTIFACT_ID, UPDATE_SKIN_CHANNEL).getCombinedName();
        String permissionChannelName = new NamespaceKey(ARTIFACT_ID, CHECK_PERM_CHANNEL).getCombinedName();
        RawDataChannel updateChannel = channelReg.ofType(RawDataChannel.class, updateChannelName);
        RawDataChannel permChannel = channelReg.ofType(RawDataChannel.class, permissionChannelName);
        updateChannel.play().addHandler(injector.getInstance(UpdateSkinListener.class));
        permChannel.play().addHandler(injector.getInstance(CheckPermissionListener.class));
    }

    @Listener
    public void onShutdown(StoppingEngineEvent<Server> stoppingServerEvent) {
        core.close();
    }

    public ChangeSkinCore getCore() {
        return core;
    }

    @Override
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
    public void sendMessage(CommandCause receiver, String key) {
        String message = core.getMessage(key);
        if (message != null && receiver != null) {
            receiver.sendMessage(Text.of(message));
        }
    }
}