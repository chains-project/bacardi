package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.ChangeSkinCore;
import com.github.games647.changeskin.core.model.auth.Account;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinUploader;
import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandContext;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.command.parameter.CommandParameters;
import org.spongepowered.api.scheduler.Task;
import net.kyori.adventure.text.Component;
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
        String url = args.<String>getOne("url").get();
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
        Command command = Command.builder()
                .executor(ctx -> {
                    CommandSource src = ctx.cause().first(CommandSource.class)
                            .orElseThrow(() -> new IllegalStateException("No command source"));
                    return execute(src, ctx);
                })
                .addParameter(CommandParameters.string(text("url")))
                .permission(PomData.ARTIFACT_ID + ".command.skinupload.base")
                .build();
        return new CommandSpecImpl(command);
    }

    public static interface CommandSpec extends Command {
    }

    private static class CommandSpecImpl implements CommandSpec {

        private final Command inner;

        CommandSpecImpl(Command inner) {
            this.inner = inner;
        }

        @Override
        public CommandResult process(CommandContext context) {
            return inner.process(context);
        }

        @Override
        public List<String> getUsage(CommandSource source) {
            return inner.getUsage(source);
        }

        @Override
        public Optional<Component> getShortDescription(CommandSource source) {
            return inner.getShortDescription(source);
        }

        @Override
        public Optional<Component> getHelp(CommandSource source) {
            return inner.getHelp(source);
        }

        @Override
        public Map<List<String>, Command> getChildren() {
            return inner.getChildren();
        }

        @Override
        public boolean testPermission(CommandSource source) {
            return inner.testPermission(source);
        }
    }
}