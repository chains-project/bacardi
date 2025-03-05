package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;
import org.spongepowered.api.entity.living.player.Player;
import java.util.Optional;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    public SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        String skinName = args.<String>getOne("skinName").get().toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) src;
            new Thread(new SkinSelector(plugin, receiver, targetId)).start();
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(src, "invalid-skin-name");
        }

        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .arguments(GenericArguments.string(Text.of("skinName")))
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }

    // Compatibility stubs for the removed API classes

    public static interface CommandSource {
        // Marker interface for a command source.
    }

    public static interface CommandExecutor {
        CommandResult execute(CommandSource src, CommandContext args);
    }

    public static class CommandResult {
        private final boolean success;

        private CommandResult(boolean success) {
            this.success = success;
        }

        public static CommandResult empty() {
            return new CommandResult(false);
        }

        public static CommandResult success() {
            return new CommandResult(true);
        }
    }

    public static class CommandSpec {
        private final CommandExecutor executor;
        private final Object arguments;
        private final String permission;

        private CommandSpec(CommandExecutor executor, Object arguments, String permission) {
            this.executor = executor;
            this.arguments = arguments;
            this.permission = permission;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private CommandExecutor executor;
            private Object arguments;
            private String permission;

            public Builder executor(CommandExecutor executor) {
                this.executor = executor;
                return this;
            }

            public Builder arguments(Object arguments) {
                this.arguments = arguments;
                return this;
            }

            public Builder permission(String permission) {
                this.permission = permission;
                return this;
            }

            public CommandSpec build() {
                return new CommandSpec(executor, arguments, permission);
            }
        }
    }

    public static class CommandContext {
        // Dummy implementation for compatibility with the old API.
        public <T> Optional<T> getOne(String key) {
            return Optional.empty();
        }
    }

    public static class GenericArguments {
        public static Object string(Text text) {
            return text;
        }
    }

    public static class Text {
        private final String content;

        private Text(String content) {
            this.content = content;
        }

        public static Text of(String content) {
            return new Text(content);
        }
    }

    public static interface ChangeSkinCommand {
        CommandSpec buildSpec();
    }
}