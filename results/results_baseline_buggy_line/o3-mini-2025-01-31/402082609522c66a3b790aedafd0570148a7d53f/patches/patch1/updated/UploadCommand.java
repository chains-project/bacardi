package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;

import java.util.List;
import java.util.ArrayList;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.CommandElement;
import org.spongepowered.api.command.parameter.managed.standard.StringParameters;
import org.spongepowered.api.scheduler.Task;

import static net.kyori.adventure.text.Component.text;

public class UploadCommand implements ChangeSkinCommand {

    private final ChangeSkinSponge plugin;
    private final ChangeSkinCore core;

    @Inject
    UploadCommand(ChangeSkinSponge plugin, ChangeSkinCore core) {
        this.plugin = plugin;
        this.core = core;
    }

    public CommandResult execute(CommandSource src, CommandContext args) {
        String url = args.requireOne("url");
        if (url.startsWith("http://") || url.startsWith("https://")) {
            List<Account> accounts = plugin.getCore().getUploadAccounts();
            if (accounts.isEmpty()) {
                plugin.sendMessage(src, "no-accounts");
            } else {
                Account uploadAccount = accounts.get(0);
                Runnable skinUploader = new SkinUploader(plugin, src, uploadAccount, url);
                Task.builder().async().execute(skinUploader).submit(plugin);
            }
        } else {
            plugin.sendMessage(src, "no-valid-url");
        }
        return CommandResult.success();
    }

    @Override
    public CommandSpec buildSpec() {
        return CommandSpec.builder()
                .executor((context, cause) -> this.execute(cause.root(), context))
                .arguments(StringParameters.string().key("url").build())
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
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
            private CommandExecutor commandExecutor;
            private final List<CommandElement> parameters = new ArrayList<>();
            private String permission;
            
            public Builder executor(CommandExecutor executor) {
                this.commandExecutor = executor;
                return this;
            }
            
            public Builder arguments(CommandElement element) {
                parameters.add(element);
                return this;
            }
            
            public Builder permission(String permission) {
                this.permission = permission;
                return this;
            }
            
            public CommandSpec build() {
                Command.Builder newBuilder = Command.builder();
                if (commandExecutor != null) {
                    newBuilder.executor(commandExecutor);
                }
                for (CommandElement element : parameters) {
                    newBuilder.addParameter(element);
                }
                if (permission != null) {
                    newBuilder.permission(permission);
                }
                return new CommandSpec(newBuilder.build());
            }
        }
    }
}