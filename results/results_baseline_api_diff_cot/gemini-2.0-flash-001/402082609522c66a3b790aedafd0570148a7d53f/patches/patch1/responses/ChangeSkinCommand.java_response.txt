package com.github.games647.changeskin.sponge.command;

//import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.ParameterKey;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.function.Function;

@FunctionalInterface
public interface ChangeSkinCommand {

    //CommandSpec buildSpec();
    Command.Builder buildSpec();
}