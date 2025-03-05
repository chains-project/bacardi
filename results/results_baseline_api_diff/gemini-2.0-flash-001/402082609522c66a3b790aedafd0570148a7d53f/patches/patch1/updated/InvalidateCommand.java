package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.github.games647.changeskin.sponge.task.SkinInvalidator;
import com.google.inject.Inject;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.Sponge;

public class InvalidateCommand implements CommandExecutor {

    private final ChangeSkinSponge plugin;

    @Inject
    InvalidateCommand(ChangeSkinSponge plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandResult execute(org.spongepowered.api.command.CommandCause cause, CommandContext args) throws CommandException {
        if (!(cause.cause().root() instanceof Player)) {
            plugin.sendMessage(cause, "no-console");
            return CommandResult.empty();
        }

        Player receiver = (Player) cause.cause().root();
        Task.builder().async().execute(new SkinInvalidator(plugin, receiver)).submit(plugin);
        return CommandResult.success();
    }

    public Command.Builder buildSpec() {
         Command.Builder builder = Command.builder();

        Optional<PermissionService> permissionService = Sponge.serviceProvider().provide(PermissionService.class);
        if (permissionService.isPresent()) {
            permissionService.get().newDescriptionBuilder(plugin)
                    .id(PomData.ARTIFACT_ID + ".command.skinupdate.base")
                    .description(org.spongepowered.api.text.Text.of("Allows the user to execute the skin update command."))
                    .register();

            builder.permission(PomData.ARTIFACT_ID + ".command.skinupdate.base");
        } else {
            builder.permission(PomData.ARTIFACT_ID + ".command.skinupdate.base");
        }

        builder.executor(this);

        return builder;
    }

}