package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.model.UserPreference;
import com.github.games647.changeskin.core.model.skin.SkinModel;
import com.github.games647.changeskin.core.shared.SkinFormatter;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;

public class InfoCommand implements ChangeSkinCommand {

    @Inject
    private ChangeSkinSponge plugin;

    @Inject
    private SkinFormatter formatter;

    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Object raw = (src instanceof CommandSourceAdapter) ? ((CommandSourceAdapter) src).getSender() : src;
        if (!(raw instanceof Player)) {
            plugin.sendMessage(raw, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) raw).getUniqueId();
        Sponge.server().scheduler().asyncExecutor(plugin).submit(() -> {
            UserPreference preferences = plugin.getCore().getStorage().getPreferences(uniqueId);
            Sponge.server().scheduler().syncExecutor(plugin).submit(() -> sendSkinDetails(uniqueId, preferences));
        });
        return CommandResult.success();
    }

    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(context -> {
                    Object rawSender = context.cause().root();
                    return this.execute(new CommandSourceAdapter(rawSender), context);
                })
                .permission(PomData.ARTIFACT_ID + ".command.skininfo.base")
                .build();
    }

    private void sendSkinDetails(UUID uuid, UserPreference preference) {
        Optional<Player> optPlayer = Sponge.server().player(uuid);
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();

            Optional<SkinModel> optSkin = preference.getTargetSkin();
            if (optSkin.isPresent()) {
                String template = plugin.getCore().getMessage("skin-info");
                String formatted = formatter.apply(template, optSkin.get());

                Component text = LegacyComponentSerializer.legacyAmpersand().deserialize(formatted);
                player.sendMessage(text);
            } else {
                plugin.sendMessage(player, "skin-not-found");
            }
        }
    }

    public interface CommandSource extends net.kyori.adventure.audience.Audience {
    }

    private static class CommandSourceAdapter implements CommandSource {
        private final Object sender;

        public CommandSourceAdapter(Object sender) {
            this.sender = sender;
        }

        public Object getSender() {
            return sender;
        }

        @Override
        public void sendMessage(Component message) {
            if (sender instanceof net.kyori.adventure.audience.Audience) {
                ((net.kyori.adventure.audience.Audience) sender).sendMessage(message);
            }
        }
    }

    public static class CommandSpec {
        private final Command command;

        private CommandSpec(Command command) {
            this.command = command;
        }

        public Command getCommand() {
            return command;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Function<CommandContext, CommandResult> executor;
            private String permission;

            public Builder executor(Function<CommandContext, CommandResult> executor) {
                this.executor = executor;
                return this;
            }

            public Builder permission(String permission) {
                this.permission = permission;
                return this;
            }

            public CommandSpec build() {
                Command command = Command.builder()
                        .executor(context -> {
                            executor.apply(context);
                            return 1;
                        })
                        .permission(permission)
                        .build();
                return new CommandSpec(command);
            }
        }
    }

    public static class CommandResult {
        public static CommandResult success() {
            return new CommandResult();
        }

        public static CommandResult empty() {
            return new CommandResult();
        }
    }
}