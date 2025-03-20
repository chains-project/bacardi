package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.command.spec.CommandSpec;

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec();
}