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

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Game;
import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.network.ChannelRegistrar;
import org.spongepowered.api.network.ChannelBinding;
import org.spongepowered.api.network.ChannelBinding.ChannelBuilder;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.event.EventResult;
import org.spongepowered.plugin.Plugin;

import static com.github.games647.changeskin.core.message.CheckPermMessage.CHECK_PERM_CHANNEL;
import static com.github.games647.changeskin.core.message.SkinUpdateMessage.UPDATE_SKIN_CHANNEL;
import static com.github.games647.changeskin.sponge.PomData.ARTIFACT_ID;

@Singleton
@Plugin(id = ARTIFACT_ID, name = PomData.NAME, version = PomData.VERSION,
        url = PomData.URL, description = PomData.DESCRIPTION)
public class ChangeSkinSponge implements PlatformPlugin<CommandSource> {

{
    private final Path dataFolder;
    private final org.slf4j.Logger logger;
    private final Injector injector;

    private final ChangeSkinCore core = new ChangeSkinCore(this);
    private final SpongeSkinAPI api = new SpongeSkinAPI(this);

    private boolean initialized;

    @Inject
    ChangeSkinSponge(org.slf4j.Logger logger, @ConfigDir(sharedRoot = false) Path dataFolder, Injector injector) {
        this.dataFolder = dataFolder;
        this.logger = logger;
        this.injector = injector.createChildInjector(binder -> binder.bind(ChangeSkinCore.class).toInstance(core));
    }

    @Listener
    public void onPreInit(GameInitializationEvent preInitEvent) {
        try {
            core.load(true);
            initialized = true;
        } catch (Exception ex) {
            logger.error("Error initializing plugin. Disabling...", ex);
        }
    }

    @Listener
    public void onInit(GameInitializationEvent initEvent) {
        if (!initialized)
            return;

        Game game = Sponge.game();
        EventManager eventManager = game.eventManager();
        PluginContainer pluginContainer = game.pluginManager().fromInstance(this).get();

        CommandSpec selectCommandSpec = injector.getInstance(SelectCommand.class).buildSpec();
        CommandSpec infoCommandSpec = injector.getInstance(InfoCommand.class).buildSpec();
        CommandSpec uploadCommandSpec = injector.getInstance(UploadCommand.class).buildSpec();
        CommandSpec setCommandSpec = injector.getInstance(SetCommand.class).buildSpec();
        CommandSpec invalidateCommandSpec = injector.getInstance(InvalidateCommand.class).buildSpec();

        game.commandManager().register(pluginContainer, selectCommandSpec, "skin-select", "skinselect");
        game.commandManager().register(pluginContainer, infoCommandSpec, "skin-info");
        game.commandManager().register(pluginContainer, uploadCommandSpec, "skin-upload");
        game.commandManager().register(pluginContainer, setCommandSpec, "changeskin", "setskin", "skin");
        game.commandManager().register(pluginContainer, invalidateCommandSpec, "skininvalidate", "skin-invalidate");

        eventManager.registerListeners(pluginContainer, injector.getInstance(LoginListener.class));

        ChannelRegistrar channelRegistrar = game.channelRegistrar();
        String updateChannelName = new NamespaceKey(ARTIFACT_ID, UPDATE_SKIN_CHANNEL).getCombinedName();
        String permissionChannelName = new NamespaceKey(ARTIFACT_ID, CHECK_PERM_CHANNEL).getCombinedName();
        ChannelBuilder updateChannelBuilder = channelRegistrar.createChannelBuilder(updateChannelName);
        ChannelBuilder permissionChannelBuilder = channelRegistrar.createChannelBuilder(permissionChannelName);
        updateChannelBuilder.register(pluginContainer, injector.getInstance(UpdateSkinListener.class));
        permissionChannelBuilder.register(pluginContainer, injector.getInstance(CheckPermissionListener.class);
    }

    @Listener
    public void onShutdown(GameStoppingServerEvent stoppingServerEvent) {
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
    public org.slf4j.Logger getLog() {
        return logger;
    }

    @Override
    public void sendMessage(CommandSource receiver, String key) {
        String message = core.getMessage(key);
        if (message != null && receiver != null) {
            receiver.sendMessage(Text.of(message));
        }
    }
}