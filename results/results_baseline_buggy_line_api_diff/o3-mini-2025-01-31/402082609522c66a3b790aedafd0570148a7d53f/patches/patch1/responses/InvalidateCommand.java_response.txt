package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;

public class InvalidateCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    public InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        Player receiver = (Player) src;
        Task.builder().execute(new SkinInvalidator(plugin, receiver)).submit(plugin);
        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                .build();
    }

    public static class CommandResult {
        public static CommandResult empty() {
            return new CommandResult();
        }
        public static CommandResult success() {
            return new CommandResult();
        }
    }

    public static class CommandSpec {
        private final Command command;

        public CommandSpec(Command command) {
            this.command = command;
        }

        public Command getCommand() {
            return command;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final Command.Builder delegate = Command.builder();

            public Builder executor(CommandExecutor executor) {
                delegate.executor(executor);
                return this;
            }

            public Builder permission(String permission) {
                delegate.permission(permission);
                return this;
            }

            public CommandSpec build() {
                return new CommandSpec(delegate.build());
            }
        }
    }
}