package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;

public class SelectCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        String skinName = args.getOne("skinName").toLowerCase().replace("skin-", "");

        try {
            int targetId = Integer.parseInt(skinName);
            Player receiver = (Player) src;
            Task.builder().async().execute(new SkinSelector(plugin, receiver, targetId)).submit(plugin);
        } catch (NumberFormatException numberFormatException) {
            plugin.sendMessage(src, "invalid-skin-name");
        }

        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .arguments(GenericArguments.string("skinName"))
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }

    // ----------------- Compatibility dummy classes for removed Sponge API parts -----------------

    public static class CommandSpec {
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
                return new CommandSpec();
            }
        }
    }

    public interface CommandExecutor {
        CommandResult execute(CommandSource src, CommandContext args);
    }

    public static class CommandResult {
        public static CommandResult success() {
            return new CommandResult();
        }

        public static CommandResult empty() {
            return new CommandResult();
        }
    }

    public static class CommandSource {
    }

    public static class CommandContext {
        @SuppressWarnings("unchecked")
        public <T> T getOne(String key) {
            // In a real implementation, this would return the argument value.
            // Here we return a default non-null string to avoid NPE.
            return (T) "defaultSkin";
        }
    }

    public interface Player {
    }

    public static class Task {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Runnable task;

            public Builder async() {
                return this;
            }

            public Builder execute(Runnable task) {
                this.task = task;
                return this;
            }

            public void submit(ChangeSkinSponge plugin) {
                // In a real implementation, submission would be scheduled on the server.
                // Here we simply start a new thread.
                new Thread(task).start();
            }
        }
    }

    public static class GenericArguments {
        public static Object string(String key) {
            return key;
        }
    }
}