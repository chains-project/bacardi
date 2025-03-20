package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandCause;

@FunctionalInterface
public interface ChangeSkinCommand {

    Command.Parameterized buildSpec();
}