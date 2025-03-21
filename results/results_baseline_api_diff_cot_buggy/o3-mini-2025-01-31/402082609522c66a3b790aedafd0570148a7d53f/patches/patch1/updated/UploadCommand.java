package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;

public class UploadCommand implements CommandExecutor, ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        String url = args.one("url").get();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(src, "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, src, uploadAccount, url);
                plugin.getScheduler().submit(skinUploader);
            }
        } else {
            plugin.sendMessage(src, "no-valid-url");
        }
        return CommandResult.success();
    }

    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor(this)
                .arguments(Parameter.string().key("url").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
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

    public static class CommandResult {
        public static CommandResult success() {
            return new CommandResult();
        }
    }
}
