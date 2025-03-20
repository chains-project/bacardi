package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinSelector;
import com.google.inject.Inject;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import java.util.Optional;
import static net.kyori.adventure.text.Component.text;
import static com.github.games647.changeskin.sponge.command.SelectCommand.GenericArguments.string;

interface ChangeSkinCommand {
    CommandSpec buildSpec();
}

public class SelectCommand implements SelectCommand.CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;

    @Inject
    SelectCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        if (!(((Object) src) instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        String skinName = args.<String>getOne("skinName").get().toLowerCase().replace("skin-", "");

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
                .arguments(string(text("skinName")))
                .permission(PomData.ARTIFACT_ID + ".command.skinselect.base")
                .build();
    }

    public interface CommandExecutor {
        CommandResult execute(CommandSource src, CommandContext args);
    }

    public static class CommandResult {
        private static final CommandResult SUCCESS = new CommandResult();
        private static final CommandResult EMPTY = new CommandResult();

        public static CommandResult success() {
            return SUCCESS;
        }

        public static CommandResult empty() {
            return EMPTY;
        }
    }

    public interface CommandSource {
    }

    public interface CommandContext {
        <T> Optional<T> getOne(String key);
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
            private Object[] arguments;
            private String permission;

            public Builder executor(CommandExecutor executor) {
                this.executor = executor;
                return this;
            }

            public Builder arguments(Object... arguments) {
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

    public static class GenericArguments {
        public static Object string(Component component) {
            return component;
        }
    }
}