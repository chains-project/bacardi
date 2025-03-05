package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.CommandSpec;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.CommandContext;

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec();
}