package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.NameResolver;
import com.github.games647.changeskin.sponge.task.SkinDownloader;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

public class SetCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    SetCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public CommandResult execute(CommandContext args) {
        Optional<Player> optionalPlayer = args.cause().first(Player.class);
        if (!optionalPlayer.isPresent()) {
            plugin.sendMessage(args.cause().root(), "no-console");
            return CommandResult.empty();
        }

        Player receiver = optionalPlayer.get();
        UUID uniqueId = receiver.getUniqueId();
        if (core.getCooldownService().isTracked(uniqueId)) {
            plugin.sendMessage(receiver, "cooldown");
            return CommandResult.empty();
        }

        String targetSkin = args.<String>getOne("skin").get();
        boolean keepSkin = args.<Boolean>getOne("keep").orElse(false);

        if ("reset".equals(targetSkin)) {
            targetSkin = receiver.getUniqueId().toString();
        }

        if (targetSkin.length() > 16) {
            UUID targetUUID = UUID.fromString(targetSkin);

            if (core.getConfig().getBoolean("skinPermission") && !plugin.hasSkinPermission(receiver, targetUUID, true)) {
                return CommandResult.empty();
            }

            plugin.sendMessage(receiver, "skin-change-queue");
            Runnable skinDownloader = new SkinDownloader(plugin, receiver, receiver, targetUUID, keepSkin);
            Task.builder().async().execute(skinDownloader).submit(plugin);
            return CommandResult.success();
        }

        Runnable nameResolver = new NameResolver(plugin, receiver, targetSkin, receiver, keepSkin);
        Task.builder().async().execute(nameResolver).submit(plugin);
        return CommandResult.success();
    }

    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .arguments(Parameter.string().key("skin").build(), Parameter.bool().key("keep").build())
                .permission(PomData.ARTIFACT_ID + ".command.setskin.base")
                .build();
    }

    public static class CommandSpec {
        private final CommandExecutor executor;
        private final Object[] arguments;
        private final String permission;

        private CommandSpec(CommandExecutor executor, Object[] arguments, String permission) {
            this.executor = executor;
            this.arguments = arguments;
            this.permission = permission;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CommandExecutor executor;
            private List<Object> arguments = new ArrayList<>();
            private String permission;

            public Builder executor(CommandExecutor executor) {
                this.executor = executor;
                return this;
            }

            public Builder arguments(Object... args) {
                for (Object arg : args) {
                    this.arguments.add(arg);
                }
                return this;
            }

            public Builder permission(String permission) {
                this.permission = permission;
                return this;
            }

            public CommandSpec build() {
                return new CommandSpec(executor, arguments.toArray(), permission);
            }
        }
    }
}